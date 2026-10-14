package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProductionCyclePersistenceTest {
    @Test fun ascensionCancelsPendingCyclesFromPreviousRun() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            db.gameDao.updateBuilding(repo.getBuildingsDirect()[1].copy(level = 1))
            repo.saveGameState(repo.getGameStateDirect()!!.copy(cash = 100_000.0))
            val startedAt = repo.getGameStateDirect()!!.lastSavedTime + 1_000L
            assertTrue(repo.startManualCycle(2, startedAt, ProductionEngine.basePrices))
            assertTrue(repo.performPrestige())
            assertEquals(0L, repo.getBuildingsDirect()[1].cycleEndsAtMs)
            repo.processOfflineDifference(startedAt + 30_000L)
            assertEquals(0.0, repo.getResourcesDirect().first { it.name == "Iron Ore" }.quantity, 0.0)
        } finally { db.close() }
    }

    @Test fun activeManualCycleSurvivesSnapshotAndFinishesOnlyOnce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sourceDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        val targetDb = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()
        try {
            val source = GameRepository(sourceDb)
            val target = GameRepository(targetDb)
            source.checkAndInitialize()
            target.checkAndInitialize()
            sourceDb.gameDao.updateBuilding(source.getBuildingsDirect()[1].copy(level = 1))
            val startedAt = source.getGameStateDirect()!!.lastSavedTime + 1_000L
            assertTrue(source.startManualCycle(2, startedAt, ProductionEngine.basePrices))
            assertFalse(source.startManualCycle(2, startedAt + 1, ProductionEngine.basePrices))
            val saved = source.exportBackup()
            assertEquals(0.0, saved.resources.first { it.name == "Iron Ore" }.quantity, 0.0)
            assertEquals(startedAt + 3_000L, saved.buildings[1].cycleEndsAtMs)
            target.restoreBackup(saved.state, saved.resources, saved.buildings, saved.combatUnits,
                saved.researchers, saved.missions)
            target.processOfflineDifference(startedAt + 3_001L)
            val completed = target.getResourcesDirect().first { it.name == "Iron Ore" }.quantity
            assertEquals(4.5, completed, 1e-8)
            target.processOfflineDifference(startedAt + 30_000L)
            assertEquals(completed, target.getResourcesDirect().first { it.name == "Iron Ore" }.quantity, 1e-8)
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }
}
