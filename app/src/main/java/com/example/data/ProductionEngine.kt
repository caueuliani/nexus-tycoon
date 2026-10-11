package com.example.data

/** Pure one-second simulation, shared by live and offline production. */
object ProductionEngine {
    val basePrices = linkedMapOf("Energy Cell" to 1.0, "Iron Ore" to 5.0,
    "Hyperalloy" to 15.0, "Quantum Chip" to 100.0, "Organic Feedstock" to 400.0,
    "Neural Implant" to 2000.0, "Antimatter Containment" to 12000.0, "Warp Drive" to 80000.0)
    data class Tick(val resources: List<ResourceInventory>, val cash: Double,
    val comrades: Double, val produced: Map<String, Double>)
    fun step(activeState: GameState, buildings: List<BusinessBuilding>,
    resources: List<ResourceInventory>, researchers: List<ResearcherCard>,
    prices: Map<String, Double> = basePrices): Tick {
        val bList = buildings.sortedBy { it.id }
        // Fetch scientists / researchers list
        val resCardList = researchers

        // Calculate Comrade passive generation multiplier (ID 5)
        val comradeGenCard = resCardList.find { it.id == 5 }
        val comradeScalar = 1.0 + (if (comradeGenCard != null && comradeGenCard.level > 0) comradeGenCard.getBoostValue() else 0.0)
        val scienceComradeMulti = 1.0 + (activeState.scienceComradeLevel * 0.20)
        val comradeRate = comradeScalar * 1.0 * scienceComradeMulti
        val newComrades = activeState.comrades + comradeRate

        // Fetch inventory map
        val resList = resources
        val startResMap = resList.associate { it.name to it.quantity }
        val resMap = startResMap.toMutableMap()
        var creditsGainedThisTick = 0.0

        // 1. Calculate production and update inventories
        val actualProducedMap = mutableMapOf<String, Double>()
        for (building in bList) {
            if (building.level == 0) continue
            if (!building.isAutomated) continue

            // Prestige system bonus
            val prestigeBonus = 1.0 + (activeState.nebulaCores * 0.10)
            // Sub VIP bonus
            val energyFactor = (if (activeState.isSubscribed) 1.5 else 1.0) * prestigeBonus

            // --- Scientist/Researcher multipliers ---
            // a) Global supreme boost (ID 6)
            val globalSupremeCard = resCardList.find { it.id == 6 }
            val globalSupremeMulti = 1.0 + (if (globalSupremeCard != null && globalSupremeCard.level > 0) globalSupremeCard.getBoostValue() else 0.0)

            // b) Local production boost (target ID matches building)
            val localProdCard = resCardList.find { it.targetBuildingId == building.id && it.boostType == "PRODUCTION" }
            val localProdMulti = 1.0 + (if (localProdCard != null && localProdCard.level > 0) localProdCard.getBoostValue() else 0.0)

            // c) Local speed boost (speeding up turns = more output)
            val localSpeedCard = resCardList.find { it.targetBuildingId == building.id && it.boostType == "SPEED" }
            val localSpeedMulti = 1.0 + (if (localSpeedCard != null && localSpeedCard.level > 0) localSpeedCard.getBoostValue() else 0.0)

            val scienceProdMulti = 1.0 + (activeState.scienceProdLevel * 0.15)
            val researcherMultipliersCombined = globalSupremeMulti * localProdMulti * localSpeedMulti * scienceProdMulti

            val baseProduced = building.getActualProductionRate() * energyFactor * researcherMultipliersCombined

            // Deduct input if any
            val inputName = building.inputResource
            var scale = 1.0
            if (inputName != null) {
                val inputNeeded = building.inputAmountPerSec * building.level
                val inputAvailable = resMap[inputName] ?: 0.0
                if (inputAvailable >= inputNeeded) {
                    resMap[inputName] = inputAvailable - inputNeeded
                    scale = 1.0
                } else {
                    scale = if (inputNeeded > 0) inputAvailable / inputNeeded else 0.0
                    resMap[inputName] = 0.0
                }
            }

            val actualProduced = baseProduced * scale
            actualProducedMap[building.id.toString()] = actualProduced

            resMap[building.resourceProduced] = (resMap[building.resourceProduced] ?: 0.0) + actualProduced


        }

        // 2. Process autosells of SURPLUS
        for (building in bList) {
            if (building.level == 0 || !building.isAutomated) continue
            val actualProduced = actualProducedMap[building.id.toString()] ?: 0.0
            if (actualProduced <= 0.0) continue

            val isHighLevelResource = building.resourceProduced != "Energy Cell" && building.resourceProduced != "Iron Ore"
            if (building.isAutoSelling && activeState.hasAutoSellLicense && isHighLevelResource) {
                val startQty = startResMap[building.resourceProduced] ?: 0.0
                val currentQty = resMap[building.resourceProduced] ?: 0.0
                val netSurplus = (currentQty - startQty).coerceAtLeast(0.0)

                val sellAmount = minOf(actualProduced, netSurplus)
                if (sellAmount > 0.0) {
                    val price = prices[building.resourceProduced] ?: 1.0
                    val scienceCreditsMulti = 1.0 + (activeState.scienceCreditsLevel * 0.10)
                    creditsGainedThisTick += sellAmount * price * scienceCreditsMulti
                    resMap[building.resourceProduced] = currentQty - sellAmount
                }
            }
        }


        require(creditsGainedThisTick.isFinite() && creditsGainedThisTick >= 0)
        require(comradeRate.isFinite() && comradeRate >= 0)
        require(resMap.values.all { it.isFinite() && it >= 0 })
        return Tick(resMap.map { ResourceInventory(it.key, it.value) }, creditsGainedThisTick,
        comradeRate, actualProducedMap)
    }
}
