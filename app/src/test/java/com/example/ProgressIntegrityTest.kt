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
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProgressIntegrityTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun memory() = Room.inMemoryDatabaseBuilder(context, GameDatabase::class.java).build()

    @Test fun concurrentLicenseAndProductionPreserveEveryGain() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(starGems = 300))
            val paid = AtomicInteger()
            coroutineScope {
                repeat(12) { launch(Dispatchers.Default) {
                    repo.buyAutoSellLicenseWithGems({ paid.incrementAndGet() }, {})
                    repo.runProductionTransaction {
                        val state = repo.getGameStateDirect()!!
                        yield()
                        repo.saveGameState(state.copy(cash = state.cash + 10))
                    }
                } }
                launch(Dispatchers.Default) { repo.grantAdReward("concurrent-ad") }
                launch(Dispatchers.Default) { repo.joinGuild("Guild") }
                launch(Dispatchers.Default) { repo.completeTutorial() }
            }
            val state = repo.getGameStateDirect()!!
            assertEquals(1, paid.get()); assertEquals(5L, state.starGems)
            assertEquals(5120.0, state.cash, 0.0)
            assertEquals("Guild", state.guildName); assertTrue(state.hasCompletedTutorial)
        } finally { db.close() }
    }

    @Test fun capsulesCannotSpendTheSameGemsTwiceOrAcceptUnknownTypes() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(starGems = 50))
            val opened = AtomicInteger()
            coroutineScope { repeat(12) { launch(Dispatchers.Default) {
                repo.buyAndOpenCapsuleWithGems("COMUM", { _, _ -> opened.incrementAndGet() }, {})
            } } }
            assertEquals(1, opened.get()); assertEquals(0L, repo.getGameStateDirect()!!.starGems)
            assertTrue(repo.getGameStateDirect()!!.science in 15..39)
            val before = repo.exportBackup()
            repo.buyAndOpenCapsuleWithGems("INVALID", { _, _ -> fail() }, {})
            assertEquals(before, repo.exportBackup())
        } finally { db.close() }
    }

    @Test fun scienceAndResearcherPurchasesChargeOnceWithLimitedFunds() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            repo.saveGameState(repo.getGameStateDirect()!!.copy(science = 100))
            val card = repo.getResearchersDirect().first().copy(cardsCollected = 100)
            repo.updateResearcher(card)
            val bought = AtomicInteger()
            coroutineScope { repeat(12) { launch(Dispatchers.Default) {
                if (it % 2 == 0) repo.purchaseScienceUpgrade("PROD", { bought.incrementAndGet() }, {})
                else repo.upgradeResearcherCard(1, { bought.incrementAndGet() }, {})
            } } }
            assertEquals(1, bought.get()); assertEquals(0L, repo.getGameStateDirect()!!.science)
            val state = repo.getGameStateDirect()!!
            assertEquals(1, state.scienceProdLevel + repo.getResearchersDirect().first().level - card.level)
        } finally { db.close() }
    }

    @Test fun missionAndRankRewardsAndVipGiftCannotBeRepeated() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val claimed = AtomicInteger(); val ranked = AtomicInteger()
            repo.rankUpPlayer { _, _, _ -> fail("Unqualified rank up") }
            repo.saveActiveMissions(repo.getActiveMissionsDirect().map { it.copy(progress = it.target, isCompleted = true) })
            coroutineScope { repeat(10) { launch(Dispatchers.Default) {
                for (id in 1..3) repo.claimMissionReward(id) { _, _, _, _ -> claimed.incrementAndGet() }
            } } }
            assertEquals(3, claimed.get())
            coroutineScope { repeat(10) { launch(Dispatchers.Default) {
                repo.rankUpPlayer { _, _, _ -> ranked.incrementAndGet() }
                repo.buySubscription()
            } } }
            assertEquals(1, ranked.get())
            val state = repo.getGameStateDirect()!!
            assertEquals(2, state.playerRank); assertEquals(0, state.completedRankMissions)
            assertEquals(15L + 20 + 30 + 70, state.starGems)
            assertEquals(50L + 80 + 150 + 200, state.science)
        } finally { db.close() }
    }

    @Test fun failedAtomicActionRollsBackDebitCardsAndMission() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val before = repo.exportBackup()
            try {
                repo.runProgressTransaction {
                    repo.updateResearcher(before.researchers.first().copy(cardsCollected = 500))
                    repo.updateActiveMission(before.missions.first().copy(isClaimed = true))
                    repo.saveGameState(before.state.copy(starGems = -1))
                }
                fail("Invalid debit must fail")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, repo.exportBackup())
        } finally { db.close() }
    }

    @Test fun malformedBackupsNeverWriteOrStopSimulation() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val cloud = CloudSaveManager(context)
            val before = repo.exportBackup(); val payload = cloud.createBackupPayload(before)
            cloud.beforeRestore = { fail("Invalid backup must not interrupt simulation") }
            val cases = listOf(payload + ("cash" to Double.NaN), payload + ("starGems" to -1L),
                payload + ("scienceProdLevel" to 1.5), payload + ("science" to 1e40),
                payload + ("backupVersion" to 3.5), payload - "researchers",
                payload + ("comrades" to Double.POSITIVE_INFINITY),
                payload + ("resources" to before.resources.map { mapOf("name" to "fake", "quantity" to 1.0) }),
                payload + ("buildings" to (payload["buildings"] as List<*>).dropLast(1)),
                payload + ("adReceipts" to listOf(mapOf("token" to "x", "rewardedAt" to -1))))
            for (invalid in cases) {
                try { cloud.restoreFromPayload(repo, invalid); fail("Invalid payload accepted") }
                catch (_: IllegalArgumentException) { }
                catch (_: ArithmeticException) { }
                assertEquals(before, repo.exportBackup())
            }
        } finally { db.close() }
    }

    @Test fun v2v3v4v5RestoreAndAdReceiptsRemainCompatible() = runBlocking {
        val db = memory(); val targetDb = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val target = GameRepository(targetDb); target.checkAndInitialize()
            repo.grantAdReward("paid-on-source")
            val cloud = CloudSaveManager(context)
            val payload = cloud.createBackupPayload(repo.exportBackup())
            cloud.restoreFromPayload(target, payload)
            assertFalse(target.grantAdReward("paid-on-source"))
            assertEquals(5L, target.getGameStateDirect()!!.starGems)
            for (version in listOf(2, 3, 4)) {
                val legacy = payload + ("backupVersion" to version) - "autoSellLicenseSource"
                cloud.restoreFromPayload(target, if (version < 4) legacy - "adReceipts" else legacy)
                assertEquals(5L, target.getGameStateDirect()!!.starGems)
                assertFalse(target.grantAdReward("paid-on-source"))
            }
        } finally { db.close(); targetDb.close() }
    }

    @Test fun offlineMatchesRepeatedOnlineStepsIncludingSharedInputShortageAndAutosell() = runBlocking {
        val db = memory()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val original = repo.exportBackup()
            val state = original.state.copy(lastSavedTime = 1000, nebulaCores = 10, availableCores = 10,
                hasAutoSellLicense = true, autoSellLicenseSource = "LEGACY",
                scienceProdLevel = 2, scienceCreditsLevel = 3, scienceComradeLevel = 2, isSubscribed = true)
            val buildings = original.buildings.map { it.copy(level = if (it.id == 1) 1 else 8, isAutomated = true, isAutoSelling = true) }
            val researchers = original.researchers.map { it.copy(level = 2) }
            repo.restoreBackup(state, original.resources, buildings, original.combatUnits, researchers, original.missions)
            var resources = original.resources; var cash = 0.0; var workers = 0.0
            repeat(120) {
                val tick = ProductionEngine.step(state, buildings, resources, researchers)
                resources = tick.resources; cash += tick.cash; workers += tick.comrades
            }
            val report = repo.processOfflineDifference(121000)!!
            assertEquals(120L, report.offlineSeconds)
            assertEquals(resources, repo.getResourcesDirect())
            assertEquals(state.cash + cash, repo.getGameStateDirect()!!.cash, 1e-7)
            assertEquals(state.comrades + workers, repo.getGameStateDirect()!!.comrades, 1e-7)
            val after = repo.exportBackup()
            assertNull(repo.processOfflineDifference(121000))
            assertNull(repo.processOfflineDifference(1000))
            assertEquals(after, repo.exportBackup())
        } finally { db.close() }
    }

    @Test fun offlineCompetitionUsesPerSecondInputPriorityNotBulkPriority() {
        val state = GameState()
        val resources = listOf(ResourceInventory("Energy Cell", 0.0), ResourceInventory("Iron Ore", 0.0), ResourceInventory("Quantum Chip", 0.0))
        val buildings = listOf(
            BusinessBuilding(1, "source", 1, true, 1.0, productionRatePerLevel = 2.0, resourceProduced = "Energy Cell"),
            BusinessBuilding(2, "first", 1, true, 1.0, productionRatePerLevel = 1.0, resourceProduced = "Iron Ore", inputResource = "Energy Cell", inputAmountPerSec = 1.0),
            BusinessBuilding(4, "second", 1, true, 1.0, productionRatePerLevel = 1.0, resourceProduced = "Quantum Chip", inputResource = "Energy Cell", inputAmountPerSec = 2.0))
        val tick = ProductionEngine.step(state, buildings, resources, emptyList())
        assertEquals(1.0, tick.resources.first { it.name == "Iron Ore" }.quantity, 0.0)
        assertEquals(0.5, tick.resources.first { it.name == "Quantum Chip" }.quantity, 0.0)
    }
}
