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
class GameplayBalanceTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), GameDatabase::class.java
    ).build()

    @Test fun freshGameHasNoSpecialCurrencyAndExistingBalancesSurvive() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            val fresh = repo.getGameStateDirect()!!
            assertEquals(0L, fresh.starGems)
            assertEquals(0L, fresh.science)
            assertEquals(0L, fresh.nebulaCores)
            assertEquals(0L, fresh.guildTokens)
            assertEquals(5000.0, fresh.cash, 0.0)
            assertTrue(repo.getBuildingsDirect().first().isAutomated)
            assertEquals(1, repo.getBuildingsDirect().first().level)
            val existing = fresh.copy(starGems = 250, science = 180, nebulaCores = 15, guildTokens = 50)
            db.gameDao.saveGameState(existing)
            repo.checkAndInitialize()
            assertEquals(existing, repo.getGameStateDirect())
        } finally { db.close() }
    }

    @Test fun offlineProductionIncludesComradesAndIsCappedAndNotDuplicated() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(
                scienceComradeLevel = 2,
                lastSavedTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            ))
            repo.processOfflineDifference()
            val after = repo.exportBackup()
            assertEquals(28800 * 1.4, after.state.comrades, 0.001)
            assertEquals(100.0 + 28800 * 2, after.resources.first { it.name == "Energy Cell" }.quantity, 0.001)
            assertNull(repo.processOfflineDifference())
            assertEquals(after.resources, repo.getResourcesDirect())
            assertEquals(after.state.comrades, repo.getGameStateDirect()!!.comrades, 0.0)
        } finally { db.close() }
    }

    @Test fun prestigeUsesCurrentBalanceAndCannotBeCollectedTwice() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db)
            repo.checkAndInitialize()
            assertEquals(100000.0, repo.getPrestigeMinCash(0), 0.001)
            assertEquals(121000.0, repo.getPrestigeMinCash(1), 0.001)
            assertEquals(400000.0, repo.getPrestigeMinCash(10), 0.001)
            assertFalse(repo.performPrestige())
            repo.saveGameState(repo.getGameStateDirect()!!.copy(
                cash = 400000.0, science = 80, starGems = 20, scienceProdLevel = 2
            ))
            val results = listOf(async { repo.performPrestige() }, async { repo.performPrestige() }).awaitAll()
            assertEquals(1, results.count { it })
            val state = repo.getGameStateDirect()!!
            assertEquals(2L, state.nebulaCores)
            assertEquals(5000.0, state.cash, 0.0)
            assertEquals(80L, state.science)
            assertEquals(20L, state.starGems)
            assertEquals(2, state.scienceProdLevel)
            assertEquals(1, repo.getBuildingsDirect().first().level)
        } finally { db.close() }
    }

    @Test fun prestigeStartsFreshRankAndMissionsWithoutLosingPermanentRewards() = runBlocking {
        val sourceDb = database()
        val targetDb = database()
        try {
            val source = GameRepository(sourceDb)
            val target = GameRepository(targetDb)
            source.checkAndInitialize()
            target.checkAndInitialize()
            val original = source.getGameStateDirect()!!
            sourceDb.gameDao.saveGameState(original.copy(
                cash = 1_600_000.0, playerRank = 4, completedRankMissions = 2,
                science = 230, starGems = 80, nebulaCores = 10, availableCores = 5,
                industrialEngineeringLevel = 1, claimedCoreEvents = "Energy Supplier",
                scienceProdLevel = 3, highestCombatStage = 3, stage1CompletedCount = 12
            ))
            source.updateActiveMission(source.getActiveMissionsDirect().first().copy(progress = 4.0))
            source.updateActiveMission(source.getActiveMissionsDirect()[1].copy(
                progress = 250.0, isCompleted = true, isClaimed = true))
            val researchers = source.getResearchersDirect().map {
                if (it.id == 2) it.copy(level = 2, cardsCollected = 9) else it
            }
            source.saveResearchers(researchers)

            assertTrue(source.performPrestige())
            assertFalse(source.performPrestige())
            val ascended = source.exportBackup()
            assertEquals(1, ascended.state.playerRank)
            assertEquals(0, ascended.state.completedRankMissions)
            assertEquals(initialRankMissions(), ascended.missions)
            assertEquals(80L, ascended.state.starGems)
            assertEquals(230L, ascended.state.science)
            assertEquals(12L, ascended.state.nebulaCores)
            assertEquals(7L, ascended.state.availableCores)
            assertEquals(1, ascended.state.industrialEngineeringLevel)
            assertEquals("Energy Supplier", ascended.state.claimedCoreEvents)
            assertEquals(3, ascended.state.scienceProdLevel)
            assertEquals(3, ascended.state.highestCombatStage)
            assertEquals(12, ascended.state.stage1CompletedCount)
            assertEquals(researchers, ascended.researchers)

            val cloud = CloudSaveManager(ApplicationProvider.getApplicationContext())
            cloud.restoreFromPayload(target, cloud.createBackupPayload(ascended))
            val restoredState = target.getGameStateDirect()!!
            assertEquals(ascended.state.copy(lastSavedTime = restoredState.lastSavedTime), restoredState)
            assertEquals(ascended.missions, target.getActiveMissionsDirect())
            assertEquals(researchers, target.getResearchersDirect())
        } finally {
            sourceDb.close()
            targetDb.close()
        }
    }
}
