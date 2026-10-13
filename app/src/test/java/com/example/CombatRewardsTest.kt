package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CombatRewardsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()

    @Test fun rewardBoundariesAndGuildTokenFormulaAreSharedByAllSectors() {
        for (sector in 1..4) for ((level, diamonds) in listOf(1 to 1L, 10 to 1L, 11 to 3L, 20 to 3L, 21 to 5L, 50 to 9L)) {
            val reward = CombatRewards.forLevel(sector, level)
            assertEquals(diamonds, reward.diamonds)
            assertEquals(listOf(500.0,1000.0,1500.0,2500.0)[sector-1] * (1 + .06 * (level-1)), reward.credits, 1e-9)
            assertEquals(((sector * 5L) * (1 + .05 * (level-1))).toLong(), reward.guildTokens)
        }
    }

    @Test fun shownRewardMatchesPaymentAndOnlyWinningSectorAdvances() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            for (sector in 1..4) for (level in listOf(1,10,11,20,21)) {
                val base = repo.getGameStateDirect()!!.copy(highestCombatStage=4, starGems=1234,
                    stage1CompletedCount=level-1,stage2CompletedCount=level-1,stage3CompletedCount=level-1,stage4CompletedCount=level-1)
                db.gameDao.saveGameState(base)
                val displayed = CombatRewards.forLevel(sector, CombatRewards.victories(base,sector)+1)
                val ticket = repo.beginCombat(sector)!!
                assertEquals(level,ticket.enemyLevel)
                assertEquals(displayed,ticket.reward())
                assertEquals(displayed,repo.completeCombat(ticket.id,true))
                val after = repo.getGameStateDirect()!!
                assertEquals(base.cash+displayed.credits,after.cash,1e-8)
                assertEquals(1234+displayed.diamonds,after.starGems)
                assertEquals(base.guildTokens+displayed.guildTokens,after.guildTokens)
                for (other in 1..4) assertEquals(level-1+(if(other==sector)1 else 0),CombatRewards.victories(after,other))
            }
        } finally { db.close() }
    }

    @Test fun defeatAndDuplicateCompletionsCannotPayOrAdvanceAgain() = runBlocking {
        val db=memory()
        try {
            val repo=GameRepository(db);repo.checkAndInitialize()
            val before=repo.getGameStateDirect()!!
            val loss=repo.beginCombat(1)!!
            assertNull(repo.completeCombat(loss.id,false))
            assertNull(repo.completeCombat(loss.id,true))
            assertEquals(before,repo.getGameStateDirect())
            val win=repo.beginCombat(1)!!
            val attempts=List(10){async(Dispatchers.Default){repo.completeCombat(win.id,true)}}.awaitAll()
            assertEquals(1,attempts.count{it!=null})
            assertEquals(before.cash+500,repo.getGameStateDirect()!!.cash,0.0)
            assertEquals(1,repo.getGameStateDirect()!!.stage1CompletedCount)
            assertNull(repo.completeCombat(win.id,false))
        } finally {db.close()}
    }

    @Test fun productionAndCombatUseFreshBalancesWithinTransactions() = runBlocking {
        val db=memory()
        try {
            val repo=GameRepository(db);repo.checkAndInitialize()
            val ticket=repo.beginCombat(1)!!
            val read=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
            val tick=launch(Dispatchers.Default){repo.runProductionTransaction {
                val state=repo.getGameStateDirect()!!;read.complete(Unit);release.await()
                repo.saveGameState(state.copy(cash=state.cash+123.0))
            }}
            read.await()
            val payment=async(Dispatchers.Default){repo.completeCombat(ticket.id,true)}
            yield();release.complete(Unit);tick.join();payment.await()
            assertEquals(5623.0,repo.getGameStateDirect()!!.cash,0.0)
            repo.runProductionTransaction {
                val state=repo.getGameStateDirect()!!;repo.saveGameState(state.copy(cash=state.cash+77))
            }
            assertEquals(5700.0,repo.getGameStateDirect()!!.cash,0.0)
            assertEquals(1L,repo.getGameStateDirect()!!.starGems)
            assertEquals(1,repo.getGameStateDirect()!!.stage1CompletedCount)
        }finally{db.close()}
    }

    @Test fun replacementAndRestoreInvalidateUnfinishedBattles() = runBlocking {
        val db=memory()
        try {
            val repo=GameRepository(db);repo.checkAndInitialize()
            assertNull(repo.beginCombat(4))
            val old=repo.beginCombat(1)!!;val active=repo.beginCombat(1)!!
            assertNull(repo.completeCombat(old.id,true))
            val snapshot=repo.exportBackup()
            repo.restoreBackup(snapshot.state,snapshot.resources,snapshot.buildings,snapshot.combatUnits,snapshot.researchers,snapshot.missions)
            assertNull(repo.completeCombat(active.id,true))
            assertEquals(snapshot.state,repo.getGameStateDirect())
        }finally{db.close()}
    }

    @Test fun migrationPreservesProgressAndCompletedReceiptsSurviveReopening() = runBlocking {
        val name="combat-${java.util.UUID.randomUUID()}.db"
        var db=Room.databaseBuilder(context,GameDatabase::class.java,name).build()
        try {
            var repo=GameRepository(db);repo.checkAndInitialize()
            db.gameDao.saveGameState(repo.getGameStateDirect()!!.copy(starGems=777,nebulaCores=100,availableCores=95,industrialEngineeringLevel=1,stage1CompletedCount=10,stage2CompletedCount=20,highestCombatStage=3))
            val saved=repo.exportBackup();db.close()
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE combat_battles")
                it.execSQL("DROP TABLE play_purchase_receipts")
                it.execSQL("ALTER TABLE game_state DROP COLUMN autoSellLicenseSource")
                it.version=10
            }
            fun open()=Room.databaseBuilder(context,GameDatabase::class.java,name).addMigrations(
                GameDatabase.MIGRATION_10_11, GameDatabase.MIGRATION_11_12, GameDatabase.MIGRATION_12_13).build()
            db=open();repo=GameRepository(db);assertEquals(saved,repo.exportBackup())
            val ticket=repo.beginCombat(1)!!;assertEquals(11,ticket.enemyLevel)
            db.close();db=open();repo=GameRepository(db)
            assertEquals(ticket.reward(),repo.completeCombat(ticket.id,true))
            val paid=repo.getGameStateDirect()!!;db.close();db=open();repo=GameRepository(db)
            assertNull(repo.completeCombat(ticket.id,true));assertEquals(paid,repo.getGameStateDirect())
            assertEquals(780L,paid.starGems)
            assertEquals(95L,paid.availableCores)
        }finally{db.close();context.deleteDatabase(name)}
    }

    @Test fun cancelledPaymentWaitingForProductionTransactionCannotAwardLater() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val ticket = repo.beginCombat(1)!!
            val before = repo.getGameStateDirect()!!
            val entered = CompletableDeferred<Unit>(); val release = CompletableDeferred<Unit>()
            val production = launch(Dispatchers.Default) {
                repo.runProductionTransaction { entered.complete(Unit); release.await() }
            }
            entered.await()
            val payment = launch(Dispatchers.Default) { repo.completeCombat(ticket.id, true) }
            payment.cancelAndJoin()
            release.complete(Unit); production.join()
            repo.completeCombat(ticket.id, false)
            assertEquals(before, repo.getGameStateDirect())
            assertNull(repo.completeCombat(ticket.id, true))
        } finally { db.close() }
    }
}
