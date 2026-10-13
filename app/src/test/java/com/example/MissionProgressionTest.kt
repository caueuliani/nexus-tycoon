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
            assertEquals(9, current.count { !it.isClaimed })
            assertEquals(old, current.take(3))
            repo.checkAndInitialize()
            assertEquals(current, repo.getActiveMissionsDirect())
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
            assertEquals(9, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertEquals(old, repo.getActiveMissionsDirect().take(3))
        } finally { db.close() }
    }

    @Test fun promotionKeepsExistingMissionsAndClaimRefillsWithoutDoublePayment() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val original = repo.getActiveMissionsDirect().map { it.copy(progress = it.target, isCompleted = true) }
            repo.saveActiveMissions(original)
            var payments = 0
            original.forEach { repo.claimMissionReward(it.id) { _, _, _, _ -> payments++ } }
            assertEquals(3, payments)
            assertEquals(3, repo.getActiveMissionsDirect().count { !it.isClaimed })
            repo.claimMissionReward(original.first().id) { _, _, _, _ -> payments++ }
            assertEquals(3, payments)
            val beforePromotion = repo.getActiveMissionsDirect().filterNot { it.isClaimed }
            repo.rankUpPlayer { _, _, _ -> }
            assertEquals(2, repo.getGameStateDirect()!!.playerRank)
            assertEquals(beforePromotion, repo.getActiveMissionsDirect().filterNot { it.isClaimed })
            repo.saveGameState(repo.getGameStateDirect()!!.copy(playerRank = 3, completedRankMissions = 3))
            repo.rankUpPlayer { _, _, _ -> }
            assertEquals(4, repo.getGameStateDirect()!!.playerRank)
            assertEquals(4, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertEquals(beforePromotion, repo.getActiveMissionsDirect().filterNot { it.isClaimed }.take(3))
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
            assertEquals(9, repo.getActiveMissionsDirect().count { !it.isClaimed })
            assertTrue(repo.performPrestige())
            assertEquals(1, repo.getGameStateDirect()!!.playerRank)
            assertEquals(3, repo.getActiveMissionsDirect().count { !it.isClaimed })
        } finally { db.close() }
    }
}
