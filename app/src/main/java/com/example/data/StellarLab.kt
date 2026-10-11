package com.example.data

import kotlin.math.pow

enum class StellarUpgrade(val title: String, val costs: List<Long>) {
    INDUSTRIAL("Engenharia Industrial", listOf(5,8,12,18,27,40,60,90,135,200)),
    TACTICAL("Logística Tática", listOf(5,8,12,18,27,40,60,90,135,200)),
    OFFLINE("Reserva Offline", listOf(10,25,50,100));
    fun level(s: GameState): Int = when (this) {
        INDUSTRIAL -> s.industrialEngineeringLevel
        TACTICAL -> s.tacticalLogisticsLevel
        OFFLINE -> s.offlineReserveLevel
    }
}

object StellarLab {
    fun offlineHours(s: GameState): Int = (if (s.isSubscribed) 12 else 8) + s.offlineReserveLevel
    fun grant(s: GameState, amount: Long): GameState {
        require(amount >= 0)
        return s.copy(nebulaCores = Math.addExact(s.nebulaCores, amount), availableCores = Math.addExact(s.availableCores, amount))
    }
    fun validate(s: GameState) {
        require(s.nebulaCores >= 0 && s.availableCores in 0..s.nebulaCores)
        require(StellarUpgrade.entries.all { it.level(s) in 0..it.costs.size })
        val spent = StellarUpgrade.entries.sumOf { it.costs.take(it.level(s)).sum() }
        require(spent <= s.nebulaCores - s.availableCores)
    }
}

object EconomyCosts {
    fun buildingBase(b: BusinessBuilding, scientists: List<ResearcherCard>, s: GameState): Double {
        val card = scientists.find { it.targetBuildingId == b.id && it.boostType == "COST_REDUCTION" }
        val existingDiscount = if (card != null && card.level > 0) card.getBoostValue() else 0.0
        return b.baseCost * (1.0 - existingDiscount).coerceIn(0.2, 1.0) * (1.0 - 0.02 * s.industrialEngineeringLevel)
    }
    fun combat(u: CombatUnit, s: GameState): Double =
        u.upgradeCost * u.costMultiplier.pow(u.level - 1) * (1.0 - 0.02 * s.tacticalLogisticsLevel)
    fun calculateMultiUpgrade(
        baseCost: Double,
        costMultiplier: Double,
        currentLevel: Int,
        currentCash: Double,
        multiplier: String
    ): Pair<Int, Double> {
        val targetMultiplier = when (multiplier) {
            "x5" -> 5
            "x10" -> 10
            "MAX" -> 999999
            else -> 1
        }

        var totalCost = 0.0
        var levelsGained = 0
        var currentL = currentLevel
        var remainingCash = currentCash

        if (currentL == 0) {
            val unlockCost = baseCost
            if (remainingCash < unlockCost) {
                return Pair(1, unlockCost)
            }
            totalCost += unlockCost
            levelsGained = 1
            remainingCash -= unlockCost
            currentL = 1
            if (targetMultiplier == 1) {
                return Pair(1, unlockCost)
            }
        }

        val nLimit = if (multiplier == "MAX") {
            if (remainingCash <= 0.0) {
                0
            } else {
                val r = costMultiplier
                val maxPossible = if (kotlin.math.abs(r - 1.0) < 1e-9) {
                    val denom = baseCost * r.pow(currentL.toDouble())
                    if (denom <= 0.0) 0 else (remainingCash / denom).toInt()
                } else {
                    val denom = baseCost * r.pow(currentL.toDouble())
                    val arg = 1.0 + (remainingCash * (r - 1.0)) / denom
                    if (arg <= 1.0) {
                        0
                    } else {
                        (kotlin.math.log(arg, kotlin.math.E) / kotlin.math.log(r, kotlin.math.E)).toInt()
                    }
                }
                kotlin.math.min(maxPossible, 999999 - levelsGained)
            }
        } else {
            targetMultiplier - levelsGained
        }

        if (nLimit > 0) {
            val r = costMultiplier
            var addCost = if (kotlin.math.abs(r - 1.0) < 1e-9) {
                baseCost * r.pow(currentL.toDouble()) * nLimit
            } else {
                baseCost * r.pow(currentL.toDouble()) * (r.pow(nLimit.toDouble()) - 1.0) / (r - 1.0)
            }

            var currentLimit = nLimit
            while (currentLimit > 0 && addCost > remainingCash) {
                currentLimit--
                addCost = if (kotlin.math.abs(r - 1.0) < 1e-9) {
                    baseCost * r.pow(currentL.toDouble()) * currentLimit
                } else {
                    baseCost * r.pow(currentL.toDouble()) * (r.pow(currentLimit.toDouble()) - 1.0) / (r - 1.0)
                }
            }

            totalCost += addCost
            levelsGained += currentLimit
        }

        if (levelsGained == 0) {
            val nextLevelCost = baseCost * costMultiplier.pow(currentL.toDouble())
            return Pair(1, nextLevelCost)
        }

        return Pair(levelsGained, totalCost)
    }

}
