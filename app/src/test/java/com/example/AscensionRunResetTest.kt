package com.example

import com.example.data.GameState
import com.example.data.initialRankMissions
import com.example.data.resetRankForNewRun
import org.junit.Assert.*
import org.junit.Test

class AscensionRunResetTest {
    @Test fun onlyRunRankCountersAreReset() {
        val before = GameState(playerRank = 7, completedRankMissions = 2,
            starGems = 80, science = 230, nebulaCores = 10, availableCores = 10,
            claimedCoreEvents = "Energy Supplier", scienceProdLevel = 3,
            highestCombatStage = 3, stage1CompletedCount = 12)
        val after = resetRankForNewRun(before)
        assertEquals(1, after.playerRank)
        assertEquals(0, after.completedRankMissions)
        assertEquals(before.copy(playerRank = 1, completedRankMissions = 0), after)
    }

    @Test fun initialMissionsReplaceOldProgressWithStartingAssets() {
        val missions = initialRankMissions()
        assertEquals(listOf(1, 2, 3), missions.map { it.id })
        assertEquals(listOf(1.0, 100.0, 5000.0), missions.map { it.progress })
        assertTrue(missions.none { it.isCompleted || it.isClaimed })
    }
}
