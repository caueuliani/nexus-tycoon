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
class AdRewardTest {
    @Test fun debugBuildUsesOnlyGoogleDemoAdIdentifiers() {
        assertTrue(BuildConfig.DEBUG)
        assertEquals("ca-app-pub-3940256099942544/5224354917", BuildConfig.REWARDED_AD_UNIT_ID)
        assertEquals("ca-app-pub-3940256099942544~3347511713", BuildConfig.ADMOB_APP_ID)
    }

    @Test fun duplicateCallbacksGrantExactlyFiveGems() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val result = List(10) { async(Dispatchers.Default) { repo.grantAdReward("same-ad") } }.awaitAll()
            assertEquals(1, result.count { it })
            assertEquals(5L, repo.getGameStateDirect()!!.starGems)
            assertTrue(repo.grantAdReward("second-ad"))
            assertEquals(10L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close() }
    }

    @Test fun failedRewardBeforeInitializationCanBeRetried() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            try { repo.grantAdReward("pending-ad"); fail("Empty save must not consume reward") }
            catch (_: IllegalStateException) { }
            repo.checkAndInitialize()
            assertTrue(repo.grantAdReward("pending-ad"))
            assertEquals(5L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close() }
    }

    @Test fun migrationPreservesVersionEightSaveAndReceiptsSurviveReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "ad-migration-${java.util.UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, GameDatabase::class.java, name).build()
        try {
            var repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(companyName = "Existing player", starGems = 123))
            val before = repo.exportBackup()
            db.close()
            // Version 8 has the same six gameplay tables and no reward receipt table.
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
                android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { old ->
                old.execSQL("DROP TABLE ad_reward_receipts")
                old.version = 8
            }
            db = Room.databaseBuilder(context, GameDatabase::class.java, name)
                .addMigrations(GameDatabase.MIGRATION_8_9).build()
            repo = GameRepository(db)
            assertEquals(before, repo.exportBackup())
            assertTrue(repo.grantAdReward("persisted-ad"))
            db.close()
            db = Room.databaseBuilder(context, GameDatabase::class.java, name)
                .addMigrations(GameDatabase.MIGRATION_8_9).build()
            repo = GameRepository(db)
            assertFalse(repo.grantAdReward("persisted-ad"))
            assertEquals(128L, repo.getGameStateDirect()!!.starGems)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
