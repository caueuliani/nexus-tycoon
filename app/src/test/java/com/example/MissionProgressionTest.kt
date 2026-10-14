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
class MissionProgressionTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), GameDatabase::class.java
    ).build()

    @Test fun rankLimitsGrowGraduallyAndCapAtTwelve() {
        val expected = listOf(3, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12)
        expected.forEachIndexed { index, limit -> assertEquals("rank ${index + 1}", limit, missionLimitForRank(index + 1)) }
        assertEquals(12, missionLimitForRank(100))
    }

    @Test fun oldRankFourteenSaveFillsOnlyMissingSlotsAndPreservesProgress() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(playerRank = 14))
            val old = repo.getActiveMissionsDirect().mapIndexed { index, m -> m.copy(progress = index + 1.0) }
            repo.saveActiveMissions(old)
            repo.checkAndInitialize()
            val current = repo.getActiveMissionsDirect()
            assertEquals(4, current.count { !it.isClaimed })
            assertEquals(old, current.take(3))
            repo.checkAndInitialize()
            assertEquals(current, repo.getActiveMissionsDirect())
        } finally { db.close() }
    }

    @Test fun promotionRequiresCurrentRanksMissionLimitAndAcceptsLegacyProgress() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val legacy = repo.getGameStateDirect()!!.copy(playerRank = 14, completedRankMissions = 3)
            repo.saveGameState(legacy)
            repo.checkAndInitialize()
            assertEquals(9, missionLimitForRank(14))
            BackupValidation.state(legacy) // An older three-completion save remains valid.

            var promotions = 0
            repo.rankUpPlayer { _, _, _ -> promotions++ }
            assertEquals(0, promotions)
            assertEquals(14, repo.getGameStateDirect()!!.playerRank)

            repeat(6) {
                val mission = repo.getActiveMissionsDirect().first { !it.isClaimed }
                repo.updateActiveMission(mission.copy(progress = mission.target, isCompleted = true))
                repo.claimMissionReward(mission.id) { _, _, _, _ -> }
                assertEquals(minOf(4, 9 - (4 + it)), repo.getActiveMissionsDirect().count { !it.isClaimed })
            }
            assertEquals(9, repo.getGameStateDirect()!!.completedRankMissions)
            BackupValidation.state(repo.getGameStateDirect()!!)
            repo.rankUpPlayer { _, _, _ -> promotions++ }
            assertEquals(1, promotions)
            assertEquals(15, repo.getGameStateDirect()!!.playerRank)
            assertEquals(0, repo.getGameStateDirect()!!.completedRankMissions)
        } finally { db.close() }
    }

    @Test fun rankTwentyProgressCapsAtTwelveWithoutEarlyPromotion() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(playerRank = 20, completedRankMissions = 11))
            repo.checkAndInitialize()
            var promotions = 0
            repo.rankUpPlayer { _, _, _ -> promotions++ }
            assertEquals(0, promotions)
            val mission = repo.getActiveMissionsDirect().first()
            repo.updateActiveMission(mission.copy(progress = mission.target, isCompleted = true))
            repo.claimMissionReward(mission.id) { _, _, _, _ -> }
            assertEquals(12, repo.getGameStateDirect()!!.completedRankMissions)
            repo.rankUpPlayer { _, _, _ -> promotions++ }
            assertEquals(1, promotions)
            assertEquals(21, repo.getGameStateDirect()!!.playerRank)
        } finally { db.close() }
    }

    @Test fun restoringLegacyThreeMissionBackupFillsRankFourteenSlots() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val saved = repo.exportBackup()
            val old = saved.missions.mapIndexed { index, m -> m.copy(progress = index + 1.0) }
            repo.restoreBackup(saved.state.copy(playerRank = 14), saved.resources, saved.buildings,
                saved.combatUnits, saved.researchers, old)
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertEquals(old, repo.getActiveMissionsDirect().take(3))
        } finally { db.close() }
    }

    @Test fun rankFourteenBatchShrinksToZeroAndStaysFinishedAfterReload() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(playerRank = 14))
            repo.checkAndInitialize()
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
            repeat(9) { claimed ->
                val mission = repo.getActiveMissionsDirect().first { !it.isClaimed }
                repo.updateActiveMission(mission.copy(progress = mission.target, isCompleted = true))
                repo.claimMissionReward(mission.id) { _, _, _, _ -> }
                assertEquals(claimed + 1, repo.getGameStateDirect()!!.completedRankMissions)
                assertEquals(minOf(4, 8 - claimed), repo.getActiveMissionsDirect().count { !it.isClaimed })
            }
            repo.checkAndInitialize()
            assertEquals(0, repo.getActiveMissionsDirect().count { !it.isClaimed })
        } finally { db.close() }
    }

    @Test fun restoreAtSevenOfNineKeepsOnlyTwoExistingMissions() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val saved = repo.exportBackup()
            val old = (1..4).map { missionForRank(14, it) }
            repo.restoreBackup(saved.state.copy(playerRank = 14, completedRankMissions = 7),
                saved.resources, saved.buildings, saved.combatUnits, saved.researchers, old)
            val remaining = repo.getActiveMissionsDirect().filterNot { it.isClaimed }
            assertEquals(2, remaining.size)
            assertEquals(old.take(2), remaining)
            repo.checkAndInitialize()
            assertEquals(remaining, repo.getActiveMissionsDirect().filterNot { it.isClaimed })
        } finally { db.close() }
    }

    @Test fun newlyRevealedMissionCanStartCompletedWhenRequirementIsAlreadyMet() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val saved = repo.exportBackup()
            repo.restoreBackup(saved.state.copy(playerRank = 14, cash = 1_000_000_000_000_000.0),
                saved.resources, saved.buildings, saved.combatUnits, saved.researchers,
                (3..5).map { missionForRank(14, it) })
            val revealed = repo.getActiveMissionsDirect().first { it.id == 6 }
            assertEquals("EARN_CASH", revealed.missionType)
            assertTrue(revealed.isCompleted)
            assertEquals(revealed.target, revealed.progress, 0.0)
            repo.claimMissionReward(revealed.id) { _, _, _, _ -> }
            assertEquals(1, repo.getGameStateDirect()!!.completedRankMissions)
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
        } finally { db.close() }
    }

    @Test fun promotionStartsFreshFiniteBatchAndClaimDoesNotPayTwice() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val original = repo.getActiveMissionsDirect().map { it.copy(progress = it.target, isCompleted = true) }
            repo.saveActiveMissions(original)
            var payments = 0
            original.forEachIndexed { index, mission ->
                repo.claimMissionReward(mission.id) { _, _, _, _ -> payments++ }
                assertEquals(2 - index, repo.getActiveMissionsDirect().count { !it.isClaimed })
            }
            assertEquals(3, payments)
            assertEquals(0, repo.getActiveMissionsDirect().count { !it.isClaimed })
            repo.claimMissionReward(original.first().id) { _, _, _, _ -> payments++ }
            assertEquals(3, payments)
            val oldIds = repo.getActiveMissionsDirect().map { it.id }.toSet()
            repo.rankUpPlayer { _, _, _ -> }
            assertEquals(2, repo.getGameStateDirect()!!.playerRank)
            assertEquals(3, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertTrue(repo.getActiveMissionsDirect().none { it.id in oldIds })
            repo.saveGameState(repo.getGameStateDirect()!!.copy(playerRank = 3, completedRankMissions = 3))
            repo.rankUpPlayer { _, _, _ -> }
            assertEquals(4, repo.getGameStateDirect()!!.playerRank)
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertTrue(repo.getActiveMissionsDirect().none { it.id in oldIds })
        } finally { db.close() }
    }

    @Test fun ascensionReturnsToThreeMissions() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val original = repo.getGameStateDirect()!!
            repo.saveGameState(original.copy(playerRank = 14, nebulaCores = 50, availableCores = 50,
                cash = 1_000_000_000.0))
            repo.checkAndInitialize()
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertTrue(repo.performPrestige())
            assertEquals(1, repo.getGameStateDirect()!!.playerRank)
            assertEquals(3, repo.getActiveMissionsDirect().count { !it.isClaimed })
        } finally { db.close() }
    }
}
