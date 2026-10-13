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
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ResearcherExpansionTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), GameDatabase::class.java
    ).build()

    @Test fun allEightIndustriesHaveThreeDistinctSpecializationsAndOriginalCards() {
        assertEquals(26, ResearcherCatalog.cards.size)
        for (id in 1..8) {
            assertEquals(setOf("PRODUCTION", "SPEED", "COST_REDUCTION"),
                ResearcherCatalog.cards.filter { it.targetBuildingId == id }.map { it.boostType }.toSet())
        }
        assertEquals((1..26).toList(), ResearcherCatalog.cards.map { it.id })
        assertEquals(2, ResearcherCatalog.cards.count { it.targetBuildingId == 0 })
    }

    @Test fun productionSpeedAndEfficiencyApplyOnceToTheirOwnIndustry() = runBlocking {
        val db = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val saved = repo.exportBackup()
            val building = saved.buildings.first().copy(level = 10, isAutomated = true)
            val state = saved.state
            val neutral = saved.researchers.map { it.copy(level = 0) }
            fun output(cards: List<ResearcherCard>) = ProductionEngine.step(state,
                listOf(building), saved.resources, cards).produced["1"]!!
            val base = output(neutral)
            val production = neutral.map { if (it.targetBuildingId == 1 && it.boostType == "PRODUCTION") it.copy(level = 1) else it }
            val speed = neutral.map { if (it.targetBuildingId == 1 && it.boostType == "SPEED") it.copy(level = 1) else it }
            val both = neutral.map { if (it.targetBuildingId == 1 && it.boostType in setOf("PRODUCTION", "SPEED")) it.copy(level = 1) else it }
            assertEquals(base * 3.0, output(production), 0.0001)
            assertEquals(base * 1.5, output(speed), 0.0001)
            assertEquals(base * 4.5, output(both), 0.0001)
            val unrelated = both.map { if (it.targetBuildingId == 2) it.copy(level = 5) else it }
            assertEquals(output(both), output(unrelated), 0.0001)
            val efficiency = neutral.map { if (it.targetBuildingId == 1 && it.boostType == "COST_REDUCTION") it.copy(level = 1) else it }
            assertEquals(EconomyCosts.buildingBase(building, neutral, state) * 0.85,
                EconomyCosts.buildingBase(building, efficiency, state), 0.0001)
            assertEquals(base, output(efficiency), 0.0001)
            val biosphere = saved.buildings.first { it.id == 5 }
            val newEfficiency = neutral.map { if (it.targetBuildingId == 5 && it.boostType == "COST_REDUCTION") it.copy(level = 1) else it }
            assertEquals(EconomyCosts.buildingBase(biosphere, neutral, state) * 0.85,
                EconomyCosts.buildingBase(biosphere, newEfficiency, state), 0.0001)
            assertTrue(output(speed.map { if (it.targetBuildingId == 1 && it.boostType == "SPEED") it.copy(level = 20) else it }).isFinite())
        } finally { db.close() }
    }

    @Test fun oldSixCardSaveMigratesWithoutChangingProgressAndNewCardEvolvesAndRestores() = runBlocking {
        val sourceDb = database(); val targetDb = database()
        try {
            val source = GameRepository(sourceDb); source.checkAndInitialize()
            val target = GameRepository(targetDb); target.checkAndInitialize()
            val saved = source.exportBackup()
            val old = saved.researchers.filter { it.id <= 6 }.map {
                if (it.id == 2) it.copy(level = 3, cardsCollected = 7) else it
            }
            target.restoreBackup(saved.state.copy(science = 1000), saved.resources, saved.buildings,
                saved.combatUnits, old, saved.missions)
            assertEquals(old, target.getResearchersDirect().filter { it.id <= 6 })
            assertEquals(26, target.getResearchersDirect().size)
            val newCard = target.getResearchersDirect().first { it.id == 7 }
            target.updateResearcher(newCard.copy(cardsCollected = newCard.cardsNeededToUpgrade))
            var evolved = false
            target.upgradeResearcherCard(7, { evolved = true }, { fail(it) })
            assertTrue(evolved)
            assertEquals(1, target.getResearchersDirect().first { it.id == 7 }.level)
            val roundTrip = target.exportBackup()
            source.restoreBackup(roundTrip.state, roundTrip.resources, roundTrip.buildings,
                roundTrip.combatUnits, roundTrip.researchers, roundTrip.missions)
            assertEquals(target.getResearchersDirect(), source.getResearchersDirect())
        } finally { sourceDb.close(); targetDb.close() }
    }

    @Test fun existingDatabaseAndCloudBackupWithSixCardsBothAddMissingCardsOnce() = runBlocking {
        val db = database(); val restoredDb = database()
        try {
            val repo = GameRepository(db); repo.checkAndInitialize()
            val restored = GameRepository(restoredDb); restored.checkAndInitialize()
            val original = repo.exportBackup()
            val legacy = original.researchers.filter { it.id <= 6 }.map {
                when (it.id) {
                    2 -> it.copy(description = "Duplica a taxa de faturamento da Broca de Perfuração.", level = 3, cardsCollected = 7)
                    3 -> it.copy(level = 4, cardsCollected = 8)
                    else -> it
                }
            }
            db.gameDao.clearResearchers()
            db.gameDao.saveResearchers(legacy)
            repo.checkAndInitialize()
            repo.checkAndInitialize()
            assertEquals(26, repo.getResearchersDirect().size)
            assertEquals(legacy.map { if (it.id == 2) it.copy(description = ResearcherCatalog.cards.first { card -> card.id == 2 }.description) else it },
                repo.getResearchersDirect().filter { it.id <= 6 })

            val payload = CloudSaveManager(ApplicationProvider.getApplicationContext()).createBackupPayload(original)
            val oldPayload = payload + ("researchers" to (payload["researchers"] as List<*>).take(6))
            CloudSaveManager(ApplicationProvider.getApplicationContext()).restoreFromPayload(restored, oldPayload)
            assertEquals(26, restored.getResearchersDirect().size)
            assertEquals(original.researchers.take(6), restored.getResearchersDirect().take(6))
        } finally { db.close(); restoredDb.close() }
    }

    @Test fun capsulePoolCanAwardNewCardsWithoutMultiplyingTotalDrops() {
        val pool = ResearcherCatalog.cards
        val grant = ResearcherRewards.roll(pool, 300, "SUPREMA", Random(42))
        assertTrue(grant.researchers.any { it.id > 6 && it.cardsCollected > 0 })
        assertEquals(pool.size, grant.researchers.size)
        assertTrue(grant.researchers.all { it.cardsCollected >= 0 })
        val small = ResearcherRewards.roll(pool.take(6), 300, "SUPREMA", Random(42))
        val expandedTotal = grant.researchers.sumOf { it.cardsCollected - pool.first { old -> old.id == it.id }.cardsCollected }
        val originalTotal = small.researchers.sumOf { it.cardsCollected - pool.first { old -> old.id == it.id }.cardsCollected }
        assertTrue(expandedTotal in (originalTotal / 2)..(originalTotal * 2))
        val promotion = ResearcherRewards.promotion(pool, Random(42))
        assertEquals(6, promotion.labels.size)
        assertEquals(mapOf("COMMON" to 2, "RARE" to 2, "EPIC" to 1, "SUPREME" to 1),
            promotion.researchers.filterIndexed { index, card -> card.cardsCollected > pool[index].cardsCollected }
                .groupingBy { it.rarity }.eachCount())
    }
}
