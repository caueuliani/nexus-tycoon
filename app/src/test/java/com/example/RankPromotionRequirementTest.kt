package com.example

import com.example.data.BackupValidation
import com.example.data.GameState
import com.example.data.missionLimitForRank
import com.example.data.missionActiveSlotLimitForRank
import com.example.data.missionForRank
import com.example.data.missingMissions
import com.example.data.missionVisibleLimit
import com.example.data.visibleMissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RankPromotionRequirementTest {
    @Test fun requirementMatchesActiveMissionSlotsAcrossAllRankBands() {
        val expected = listOf(3, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9,
            10, 10, 11, 11, 12)
        expected.forEachIndexed { index, required ->
            assertEquals("rank ${index + 1}", required, missionLimitForRank(index + 1))
        }
        assertEquals(12, missionLimitForRank(100))
    }

    @Test fun existingThreeCompletionSaveStaysValidAtRankFourteen() {
        BackupValidation.state(GameState(playerRank = 14, completedRankMissions = 3))
        BackupValidation.state(GameState(playerRank = 14, completedRankMissions = 9))
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidation.state(GameState(playerRank = 14, completedRankMissions = 10))
        }
    }

    @Test fun activeWindowCapsAtFourWithoutDeletingLegacyMissions() {
        assertEquals(3, missionActiveSlotLimitForRank(1))
        assertEquals(4, missionActiveSlotLimitForRank(14))
        assertEquals(4, missionActiveSlotLimitForRank(100))
        val legacy = (1..9).map { missionForRank(14, it) }
        assertEquals(4, visibleMissions(14, 0, legacy).size)
        assertEquals(0, missingMissions(14, 0, legacy).size)
        val firstClaimed = legacy.first().copy(isCompleted = true, isClaimed = true)
        val afterClaim = listOf(firstClaimed) + legacy.drop(1)
        assertEquals(legacy.drop(1).take(4), visibleMissions(14, 1, afterClaim))
        assertEquals(9, afterClaim.size)
        val completedHidden = legacy.last().copy(progress = legacy.last().target, isCompleted = true)
        assertEquals(completedHidden.id,
            visibleMissions(14, 0, legacy.dropLast(1) + completedHidden).first().id)
    }

    @Test fun newHighRankSaveFillsOnlyFourSlots() {
        val existing = (1..3).map { missionForRank(14, it) }
        val generated = missingMissions(14, 0, existing)
        assertEquals(1, generated.size)
        assertEquals(4, visibleMissions(14, 0, existing + generated).size)
    }

    @Test fun visibleWindowShrinksWithFiniteRankBatch() {
        listOf(0 to 4, 3 to 4, 5 to 4, 6 to 3, 7 to 2, 8 to 1, 9 to 0)
            .forEach { (completed, visible) ->
                assertEquals("rank 14, $completed/9", visible, missionVisibleLimit(14, completed))
            }
        assertEquals(listOf(3, 2, 1, 0), (0..3).map { missionVisibleLimit(1, it) })
        assertEquals(listOf(4, 3, 2, 1, 0), (0..4).map { missionVisibleLimit(4, it) })
    }
}
