package com.example.data

import kotlin.math.max
import kotlin.math.roundToLong

/** All rates below are the rates of the pre-cycle, one-second simulation. */
object ProductionCycles {
    private val baseMs = longArrayOf(2_000, 3_000, 5_000, 6_000, 8_000, 10_000, 12_000, 15_000)

    data class Plan(val durationMs: Long, val output: Double, val input: Double, val averagePerSecond: Double)
    data class Result(
        val buildings: List<BusinessBuilding>, val resources: List<ResourceInventory>,
        val cash: Double, val comrades: Double, val comradesRate: Double,
        val produced: Map<Int, Double>
    )

    fun plan(state: GameState, building: BusinessBuilding, researchers: List<ResearcherCard>): Plan {
        require(building.id in 1..8)
        val speed = researchers.firstOrNull { it.targetBuildingId == building.id && it.boostType == "SPEED" }
            ?.let { if (it.level > 0) 1.0 + it.getBoostValue() else 1.0 } ?: 1.0
        val milestoneSpeed = when {
            building.level >= 50 -> 8.0
            building.level >= 25 -> 4.0
            building.level >= 10 -> 2.0
            else -> 1.0
        }
        val durationMs = max(500L, (baseMs[building.id - 1] / milestoneSpeed / speed).roundToLong())
        val global = researchers.firstOrNull { it.id == 6 }
            ?.let { if (it.level > 0) 1.0 + it.getBoostValue() else 1.0 } ?: 1.0
        val production = researchers.firstOrNull { it.targetBuildingId == building.id && it.boostType == "PRODUCTION" }
            ?.let { if (it.level > 0) 1.0 + it.getBoostValue() else 1.0 } ?: 1.0
        val amountMultiplier = (if (state.isSubscribed) 1.5 else 1.0) *
            (1.0 + state.nebulaCores * 0.10) * global * production *
            (1.0 + state.scienceProdLevel * 0.15)
        val oldAverage = building.getActualProductionRate() * amountMultiplier * speed
        // The speed milestone changes frequency, not income. At the 0.5s floor,
        // the otherwise lost SPEED benefit stays in output, exactly once.
        val durationSeconds = durationMs / 1000.0
        val output = oldAverage * durationSeconds
        val input = if (building.inputResource == null) 0.0
            else building.inputAmountPerSec * building.level * durationSeconds
        require(output.isFinite() && output >= 0 && input.isFinite() && input >= 0)
        return Plan(durationMs, output, input, oldAverage)
    }

    fun advance(
        state: GameState, buildings: List<BusinessBuilding>, resources: List<ResourceInventory>,
        researchers: List<ResearcherCard>, fromMs: Long, untilMs: Long,
        prices: Map<String, Double> = ProductionEngine.basePrices
    ): Result {
        require(untilMs >= fromMs)
        val active = buildings.associateBy { it.id }.toMutableMap()
        val stock = resources.associate { it.name to it.quantity }.toMutableMap()
        val produced = mutableMapOf<Int, Double>()
        val comradesRate = (1.0 + (researchers.firstOrNull { it.id == 5 }
            ?.let { if (it.level > 0) it.getBoostValue() else 0.0 } ?: 0.0)) *
            (1.0 + state.scienceComradeLevel * 0.20)
        var cash = 0.0
        var cursor = fromMs

        fun startAutomated(time: Long) {
            for (id in 1..8) {
                val b = active[id] ?: continue
                if (b.level <= 0 || !b.isAutomated || b.cycleEndsAtMs > 0) continue
                val next = start(state, b, researchers, stock, time, prices)
                if (next != null) active[id] = next
            }
        }

        startAutomated(cursor)
        var events = 0
        while (true) {
            val due = active.values.filter { it.cycleEndsAtMs > 0 && it.cycleEndsAtMs <= untilMs }
            if (due.isEmpty()) break
            val nextTime = max(cursor, due.minOf { it.cycleEndsAtMs })
            val completing = due.filter { it.cycleEndsAtMs <= nextTime }.sortedBy { it.id }
            val before = stock.toMap()
            for (b in completing) {
                stock[b.resourceProduced] = (stock[b.resourceProduced] ?: 0.0) + b.cycleOutput
                produced[b.id] = (produced[b.id] ?: 0.0) + b.cycleOutput
                active[b.id] = b.copy(cycleStartedAtMs = 0, cycleEndsAtMs = 0,
                    cycleOutput = 0.0, cycleSalePrice = 0.0)
            }
            cursor = nextTime
            startAutomated(cursor)
            // Sell only output left after newly started downstream cycles reserve input.
            for (b in completing) {
                if (b.cycleSalePrice <= 0) continue
                val current = stock[b.resourceProduced] ?: 0.0
                val surplus = (current - (before[b.resourceProduced] ?: 0.0)).coerceAtLeast(0.0)
                // A consumer already in flight reserved this cycle's input at its start.
                // Keep enough stock for its next cycle before selling newly completed output.
                val chainReserve = active.values.asSequence()
                    .filter { it.level > 0 && it.inputResource == b.resourceProduced }
                    .sumOf { plan(state, it, researchers).input }
                val sold = minOf(b.cycleOutput, surplus, (current - chainReserve).coerceAtLeast(0.0))
                if (sold > 0) {
                    stock[b.resourceProduced] = current - sold
                    cash += sold * b.cycleSalePrice
                }
            }
            // Guard malformed imported data; normal cycles are at least 500ms.
            if (++events > 2_000_000) error("Limite de simulação excedido")
        }
        require(stock.values.all { it.isFinite() && it >= 0 } && cash.isFinite())
        return Result(active.values.sortedBy { it.id }, stock.map { ResourceInventory(it.key, it.value) },
            cash, comradesRate * ((untilMs - fromMs) / 1000.0), comradesRate, produced)
    }

    fun start(
        state: GameState, building: BusinessBuilding, researchers: List<ResearcherCard>,
        stock: MutableMap<String, Double>, nowMs: Long,
        prices: Map<String, Double> = ProductionEngine.basePrices
    ): BusinessBuilding? {
        if (building.level <= 0 || building.cycleEndsAtMs > 0) return null
        val plan = plan(state, building, researchers)
        val inputName = building.inputResource
        if (inputName != null) {
            val available = stock[inputName] ?: 0.0
            if (available + 1e-9 < plan.input) return null
            stock[inputName] = (available - plan.input).coerceAtLeast(0.0)
        }
        val canSell = building.isAutoSelling && state.hasAutoSellLicense &&
            building.resourceProduced !in setOf("Energy Cell", "Iron Ore")
        val salePrice = if (canSell) (prices[building.resourceProduced] ?: 1.0) *
            (1.0 + state.scienceCreditsLevel * 0.10) else 0.0
        return building.copy(cycleStartedAtMs = nowMs, cycleEndsAtMs = nowMs + plan.durationMs,
            cycleOutput = plan.output, cycleSalePrice = salePrice)
    }
}
