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
            repo.saveGameState(existing)
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
}
