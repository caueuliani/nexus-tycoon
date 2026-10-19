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
class NovaPilotTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()

    @Test fun tutorialQueuesOnceAndDismissalSurvivesReplay() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            assertEquals(0, repo.getGameStateDirect()!!.novaPendingMask)
            repo.completeTutorial()
            assertEquals(NovaPilot.FIRST_INDUSTRY, repo.getGameStateDirect()!!.novaPendingMask)
            repo.completeTutorial() // repeated callback and recomposition
            assertEquals(NovaPilot.FIRST_INDUSTRY, repo.getGameStateDirect()!!.novaPendingMask)
            repo.acknowledgeNova(NovaPilot.FIRST_INDUSTRY)
            repo.resetTutorial(); repo.completeTutorial()
            val state = repo.getGameStateDirect()!!
            assertEquals(0, state.novaPendingMask)
            assertEquals(NovaPilot.FIRST_INDUSTRY, state.novaSeenMask)
        } finally { db.close() }
    }

    @Test fun firstResearcherIsFirstCopyAndAscensionQueuesOnlyOnce() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val before = repo.getResearchersDirect()
            assertTrue(NovaPilot.firstResearcherAcquired(before,
                before.map { if (it.id == 2) it.copy(cardsCollected = 1) else it }))
            assertFalse(NovaPilot.firstResearcherAcquired(before,
                before.map { it.copy(level = 0, cardsCollected = 0) }))
            val state = repo.getGameStateDirect()!!
            repo.saveGameState(state.copy(cash = 100_000.0))
            assertTrue(repo.performPrestige())
            assertEquals(NovaPilot.FIRST_ASCENSION, repo.getGameStateDirect()!!.novaPendingMask)
            repo.acknowledgeNova(NovaPilot.FIRST_ASCENSION)
            repo.saveGameState(repo.getGameStateDirect()!!.copy(cash = 200_000.0))
            assertTrue(repo.performPrestige())
            assertEquals(0, repo.getGameStateDirect()!!.novaPendingMask)
        } finally { db.close() }
    }

    @Test fun pendingAndSeenSurviveBackupAndOlderBackupsDoNotReplay() = runBlocking {
        val firstDb = memory(); val secondDb = memory()
        try {
            val first = GameRepository(firstDb); first.checkAndInitialize()
            val second = GameRepository(secondDb); second.checkAndInitialize()
            first.completeTutorial()
            val cloud = CloudSaveManager(context)
            val payload = cloud.createBackupPayload(first.exportBackup())
            assertEquals(7, payload["backupVersion"])
            cloud.restoreFromPayload(second, payload)
            assertEquals(NovaPilot.FIRST_INDUSTRY, second.getGameStateDirect()!!.novaPendingMask)
            second.acknowledgeNova(NovaPilot.FIRST_INDUSTRY)
            assertEquals(NovaPilot.FIRST_INDUSTRY, second.getGameStateDirect()!!.novaSeenMask)
            cloud.restoreFromPayload(second, payload)
            assertEquals(0, second.getGameStateDirect()!!.novaPendingMask)
            assertEquals(NovaPilot.FIRST_INDUSTRY, second.getGameStateDirect()!!.novaSeenMask)

            val old = payload.toMutableMap().apply {
                this["backupVersion"] = 6
                remove("novaSeenMask"); remove("novaPendingMask")
            }
            cloud.restoreFromPayload(second, old)
            assertEquals(NovaPilot.ALL, second.getGameStateDirect()!!.novaSeenMask)
            assertEquals(0, second.getGameStateDirect()!!.novaPendingMask)
        } finally { firstDb.close(); secondDb.close() }
    }

    @Test fun simultaneousEventsHaveStableOrderAndInvalidStateIsRejected() {
        val initial = GameState()
        val combined = NovaPilot.queue(NovaPilot.queue(initial, NovaPilot.FIRST_RESEARCHER),
            NovaPilot.FIRST_INDUSTRY)
        assertEquals(NovaPilot.FIRST_INDUSTRY, NovaPilot.next(combined))
        assertEquals(NovaPilot.FIRST_RESEARCHER,
            NovaPilot.next(NovaPilot.acknowledge(combined, NovaPilot.FIRST_INDUSTRY)))
        assertThrows(IllegalArgumentException::class.java) {
            BackupValidation.state(initial.copy(novaSeenMask = 8))
        }
    }
}
