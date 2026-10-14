package com.example

import com.example.data.*
import org.junit.Assert.*
import org.junit.Test

class ProductionCyclesTest {
    private val state = GameState(lastSavedTime = 1_000L)
    private fun building(id: Int = 3, level: Int = 1, automated: Boolean = false) = BusinessBuilding(
        id, "Test", level, automated, 100.0, 1.15, 1.0,
        if (id == 1) "Energy Cell" else "Hyperalloy",
        if (id == 1) null else "Iron Ore", if (id == 1) 0.0 else 0.3)
    private fun stocks(iron: Double = 100.0) = listOf(
        ResourceInventory("Energy Cell", 0.0), ResourceInventory("Iron Ore", iron),
        ResourceInventory("Hyperalloy", 0.0))

    @Test fun everyIndustryUsesItsOwnBaseDuration() {
        assertEquals(listOf(2000L, 3000L, 5000L, 6000L, 8000L, 10000L, 12000L, 15000L),
            (1..8).map { ProductionCycles.plan(state, building(id = it), emptyList()).durationMs })
    }

    @Test fun milestoneSpeedDoesNotChangeAverageIncome() {
        val base = building()
        val at1 = ProductionCycles.plan(state, base, emptyList())
        val at10 = ProductionCycles.plan(state, base.copy(level = 10), emptyList())
        val at25 = ProductionCycles.plan(state, base.copy(level = 25), emptyList())
        val at50 = ProductionCycles.plan(state, base.copy(level = 50), emptyList())
        assertEquals(listOf(5000L, 2500L, 1250L, 625L),
            listOf(at1.durationMs, at10.durationMs, at25.durationMs, at50.durationMs))
        for (plan in listOf(at1, at10, at25, at50)) {
            assertEquals(plan.averagePerSecond, plan.output / (plan.durationMs / 1000.0), 1e-8)
        }
        assertEquals(5.0, at1.output, 1e-8)
        assertEquals(1.5, at1.input, 1e-8)
    }

    @Test fun speedAtFloorMovesOnlyExcessToQuantity() {
        val b = building(level = 50)
        val speed = ResearcherCard(3, "Speed", "", 2, 0, 3, 3, "SPEED", "RARE")
        val normal = ProductionCycles.plan(state, b, emptyList())
        val boosted = ProductionCycles.plan(state, b, listOf(speed))
        assertEquals(625L, normal.durationMs)
        assertEquals(500L, boosted.durationMs)
        assertEquals(normal.averagePerSecond * 2, boosted.averagePerSecond, 1e-8)
        assertEquals(boosted.averagePerSecond, boosted.output / 0.5, 1e-8)
        assertEquals(0.3 * 50 * 0.5, boosted.input, 1e-8)
    }

    @Test fun manualReservesInputAndPaysOnlyOnceAtCompletion() {
        val b = building()
        val stock = stocks().associate { it.name to it.quantity }.toMutableMap()
        val started = ProductionCycles.start(state, b, emptyList(), stock, 1_000L)!!
        assertNull(ProductionCycles.start(state, started, emptyList(), stock, 1_001L))
        assertEquals(98.5, stock["Iron Ore"]!!, 1e-8)
        val before = ProductionCycles.advance(state, listOf(started),
            stock.map { ResourceInventory(it.key, it.value) }, emptyList(), 1_000L, 5_999L)
        assertEquals(0.0, before.resources.first { it.name == "Hyperalloy" }.quantity, 0.0)
        val completed = ProductionCycles.advance(state, before.buildings, before.resources,
            emptyList(), 5_999L, 6_000L)
        assertEquals(5.0, completed.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
        val again = ProductionCycles.advance(state, completed.buildings, completed.resources,
            emptyList(), 6_000L, 12_000L)
        assertEquals(5.0, again.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
    }

    @Test fun automatedRepeatsAndStopsWhenInputRunsOut() {
        val b = building(automated = true)
        val first = ProductionCycles.advance(state, listOf(b), stocks(3.0), emptyList(), 1_000L, 6_000L)
        assertEquals(5.0, first.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
        assertEquals(0.0, first.resources.first { it.name == "Iron Ore" }.quantity, 1e-8)
        val second = ProductionCycles.advance(state, first.buildings, first.resources, emptyList(), 6_000L, 11_000L)
        assertEquals(10.0, second.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
        val third = ProductionCycles.advance(state, second.buildings, second.resources, emptyList(), 11_000L, 30_000L)
        assertEquals(10.0, third.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
    }

    @Test fun insufficientInputNeverStartsOrPartiallyPaysACycle() {
        val stock = stocks(1.0).associate { it.name to it.quantity }.toMutableMap()
        assertNull(ProductionCycles.start(state, building(), emptyList(), stock, 1_000L))
        assertEquals(1.0, stock["Iron Ore"]!!, 0.0)
        assertEquals(0.0, stock["Hyperalloy"]!!, 0.0)
    }

    @Test fun autoSellKeepsInputForTheNextDownstreamCycle() {
        val seller = building(automated = true).copy(isAutoSelling = true)
        val consumer = BusinessBuilding(7, "Reactor", 1, true, 100.0, 1.15,
            0.15, "Antimatter Containment", "Hyperalloy", 0.2)
        val stock = listOf(ResourceInventory("Iron Ore", 100.0),
            ResourceInventory("Hyperalloy", 0.0), ResourceInventory("Antimatter Containment", 0.0))
        val result = ProductionCycles.advance(state.copy(hasAutoSellLicense = true),
            listOf(seller, consumer), stock, emptyList(), 1_000L, 11_000L)
        assertEquals(2.4, result.resources.first { it.name == "Hyperalloy" }.quantity, 1e-8)
        assertTrue(result.cash > 0.0)
        assertTrue(result.buildings.first { it.id == 7 }.cycleEndsAtMs > 11_000L)
    }
}
