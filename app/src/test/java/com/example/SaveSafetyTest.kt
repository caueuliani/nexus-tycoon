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
class SaveSafetyTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), GameDatabase::class.java
    ).build()

    @Test fun incompleteSaveIsPreservedInsteadOfReset() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val progress = repo.getGameStateDirect()!!.copy(cash = 98765.0, starGems = 80)
            repo.saveGameState(progress)
            val buildings = repo.getBuildingsDirect().dropLast(1)
            db.gameDao.clearBuildings()
            db.gameDao.saveBuildings(buildings)
            try { repo.checkAndInitialize(); fail("Partial save must not be reseeded") }
            catch (_: IllegalStateException) { }
            assertEquals(progress, repo.getGameStateDirect())
            assertEquals(buildings, repo.getBuildingsDirect())
        } finally { db.close() }
    }

    @Test fun simultaneousSalesCannotSellTheSameStockTwice() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val results = List(10) { async(Dispatchers.Default) {
                repo.sellResource("Energy Cell", 100.0, 2.0)
            } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(5200.0, repo.getGameStateDirect()!!.cash, 0.0)
            assertEquals(0.0, repo.getResourcesDirect().first { it.name == "Energy Cell" }.quantity, 0.0)
            assertFalse(repo.sellResource("Energy Cell", -10.0, 2.0))
            assertFalse(repo.sellResource("Energy Cell", 1.0, Double.NaN))
        } finally { db.close() }
    }

    @Test fun simultaneousManagerPurchasesChargeOnlyOnce() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(starGems = 100))
            assertFalse(repo.automateBuilding(2, 35))
            assertTrue(repo.upgradeBuilding(2, "x1"))
            val results = List(10) { async(Dispatchers.Default) { repo.automateBuilding(2, 35) } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(65L, repo.getGameStateDirect()!!.starGems)
            assertTrue(repo.getBuildingsDirect().first { it.id == 2 }.isAutomated)
        } finally { db.close() }
    }

    @Test fun concurrentGemExchangesAndAdRewardPreserveBothBalances() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val exchanges = List(10) { async(Dispatchers.Default) { repo.exchangeCashForGems(5000.0, 100) } }
            val ad = async(Dispatchers.Default) { repo.grantAdReward("concurrent-ad") }
            assertEquals(1, exchanges.awaitAll().count { it })
            assertTrue(ad.await())
            assertEquals(0.0, repo.getGameStateDirect()!!.cash, 0.0)
            assertEquals(105L, repo.getGameStateDirect()!!.starGems)
            val before = repo.exportBackup()
            assertFalse(repo.exchangeCashForGems(-1.0, 50))
            assertFalse(repo.exchangeCashForGems(Double.NaN, 50))
            assertFalse(repo.exchangeCashForGems(1.0, -50))
            assertEquals(before, repo.exportBackup())
        } finally { db.close() }
    }

    @Test fun concurrentTimeWarpsCannotSpendTheSameGemsTwice() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(starGems = 20))
            val before = repo.exportBackup()
            val results = List(10) { async(Dispatchers.Default) { repo.applyTimeWarp(20, 1) } }.awaitAll()
            assertEquals(1, results.count { it })
            assertEquals(0L, repo.getGameStateDirect()!!.starGems)
            val after = repo.exportBackup()
            assertTrue(after.state.cash > before.state.cash)
            assertTrue(after.resources.first { it.name == "Energy Cell" }.quantity > 100.0)
            assertFalse(repo.applyTimeWarp(-20, 1))
            assertFalse(repo.applyTimeWarp(20, -1))
            assertEquals(after, repo.exportBackup())
        } finally { db.close() }
    }
}
