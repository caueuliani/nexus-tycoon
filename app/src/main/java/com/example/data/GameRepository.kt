package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.pow

data class BackupSnapshot(
    val state: GameState,
    val resources: List<ResourceInventory>,
    val buildings: List<BusinessBuilding>,
    val combatUnits: List<CombatUnit>,
    val researchers: List<ResearcherCard>,
    val missions: List<ActiveMission>,
    val adReceipts: List<AdRewardReceipt> = emptyList()
)

class GameRepository(private val db: GameDatabase) {
    private val dao = db.gameDao

    private suspend fun saveValidatedState(state: GameState) {
        BackupValidation.state(state)
        dao.saveGameState(state)
    }

    val gameState: Flow<GameState?> = dao.getGameStateFlow()
    val resources: Flow<List<ResourceInventory>> = dao.getResourcesFlow()
    val buildings: Flow<List<BusinessBuilding>> = dao.getBuildingsFlow()
    val combatUnits: Flow<List<CombatUnit>> = dao.getCombatUnitsFlow()
    val researchers: Flow<List<ResearcherCard>> = dao.getResearchersFlow()
    val activeMissions: Flow<List<ActiveMission>> = dao.getActiveMissionsFlow()

    suspend fun getGameStateDirect(): GameState? = dao.getGameStateDirect()

    suspend fun checkAndInitialize() = db.withTransaction {
        val state = dao.getGameStateDirect()
        val buildingsList = dao.getBuildingsDirect()
        if (state != null) {
            check(buildingsList.map { it.id }.sorted() == (1..8).toList() &&
                dao.getResourcesDirect().size == 8 && dao.getCombatUnitsDirect().size == 4 &&
                dao.getResearchersDirect().size == 6 && dao.getActiveMissionsDirect().isNotEmpty()) {
                "Save incompleto. Os dados foram preservados; é necessário recuperar o backup."
            }
            // A debug/demo or restored flag is not evidence of a paid subscription.
            if (!com.example.BuildConfig.DEBUG &&
                (state.isSubscribed || state.autoSellLicenseSource == "DEMO")) {
                saveValidatedState(state.copy(isSubscribed = false,
                    hasAutoSellLicense = if (state.autoSellLicenseSource == "DEMO") false else state.hasAutoSellLicense,
                    autoSellLicenseSource = if (state.autoSellLicenseSource == "DEMO") "NONE" else state.autoSellLicenseSource))
            }
            return@withTransaction
        }
        check(buildingsList.isEmpty() && dao.getResourcesDirect().isEmpty() &&
            dao.getCombatUnitsDirect().isEmpty() && dao.getResearchersDirect().isEmpty() &&
            dao.getActiveMissionsDirect().isEmpty()) {
            "Save parcial encontrado. Os dados foram preservados."
        }
        if (state == null) {
            // Seed base state
            saveValidatedState(GameState())
            
            // Seed Resources
            dao.saveResources(listOf(
                ResourceInventory("Energy Cell", 100.0),
                ResourceInventory("Iron Ore", 0.0),
                ResourceInventory("Hyperalloy", 0.0),
                ResourceInventory("Quantum Chip", 0.0),
                ResourceInventory("Organic Feedstock", 0.0),
                ResourceInventory("Neural Implant", 0.0),
                ResourceInventory("Antimatter Containment", 0.0),
                ResourceInventory("Warp Drive", 0.0)
            ))

            // Seed Buildings
            dao.saveBuildings(listOf(
                BusinessBuilding(
                    id = 1,
                    name = "Sol-Power Array",
                    level = 1,
                    isAutomated = true,
                    baseCost = 80.0,
                    costMultiplier = 1.12,
                    productionRatePerLevel = 2.0,
                    resourceProduced = "Energy Cell",
                    inputResource = null,
                    inputAmountPerSec = 0.0,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 2,
                    name = "Deep-Core Drill",
                    level = 0,
                    isAutomated = false,
                    baseCost = 350.0,
                    costMultiplier = 1.14,
                    productionRatePerLevel = 1.5,
                    resourceProduced = "Iron Ore",
                    inputResource = "Energy Cell",
                    inputAmountPerSec = 0.5,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 3,
                    name = "Alloy Smelter",
                    level = 0,
                    isAutomated = false,
                    baseCost = 1500.0,
                    costMultiplier = 1.16,
                    productionRatePerLevel = 1.0,
                    resourceProduced = "Hyperalloy",
                    inputResource = "Iron Ore",
                    inputAmountPerSec = 0.3,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 4,
                    name = "Quantum Assembler",
                    level = 0,
                    isAutomated = false,
                    baseCost = 10000.0,
                    costMultiplier = 1.18,
                    productionRatePerLevel = 0.6,
                    resourceProduced = "Quantum Chip",
                    inputResource = "Energy Cell",
                    inputAmountPerSec = 1.5,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 5,
                    name = "Biosphere Dome",
                    level = 0,
                    isAutomated = false,
                    baseCost = 75000.0,
                    costMultiplier = 1.20,
                    productionRatePerLevel = 0.4,
                    resourceProduced = "Organic Feedstock",
                    inputResource = "Energy Cell",
                    inputAmountPerSec = 3.0,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 6,
                    name = "Cybernetics Lab",
                    level = 0,
                    isAutomated = false,
                    baseCost = 500000.0,
                    costMultiplier = 1.22,
                    productionRatePerLevel = 0.25,
                    resourceProduced = "Neural Implant",
                    inputResource = "Quantum Chip",
                    inputAmountPerSec = 0.1,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 7,
                    name = "Antimatter Reactor",
                    level = 0,
                    isAutomated = false,
                    baseCost = 4000000.0,
                    costMultiplier = 1.24,
                    productionRatePerLevel = 0.15,
                    resourceProduced = "Antimatter Containment",
                    inputResource = "Hyperalloy",
                    inputAmountPerSec = 0.2,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 8,
                    name = "Galactic Shipyard",
                    level = 0,
                    isAutomated = false,
                    baseCost = 35000000.0,
                    costMultiplier = 1.26,
                    productionRatePerLevel = 0.08,
                    resourceProduced = "Warp Drive",
                    inputResource = "Neural Implant",
                    inputAmountPerSec = 0.05,
                    isAutoSelling = false
                )
            ))

            // Seed Combat Units
            dao.saveCombatUnits(listOf(
                CombatUnit(1, "Titan Defender", "Heavy Mech", 1, 150, 18, 120.0),
                CombatUnit(2, "Aegis Vanguard", "Shield Troop", 1, 350, 8, 250.0),
                CombatUnit(3, "Plasma Striker", "Ranged DPS", 1, 90, 42, 500.0),
                CombatUnit(4, "EMP Interceptor", "Disrupter", 1, 120, 25, 1200.0)
            ))

            // Seed Researchers / Scientists
            dao.saveResearchers(listOf(
                ResearcherCard(1, "Elon Dusk-V", "Reduz o custo total de upgrades do Painel Solar em -15% por nível.", 1, 1, 2, 1, "COST_REDUCTION", "COMMON"),
                ResearcherCard(2, "Marie Curie-ous", "Duplica a taxa de faturamento da Broca de Perfuração.", 0, 0, 2, 2, "PRODUCTION", "COMMON"),
                ResearcherCard(3, "Ada Lovelace-space", "Aumenta a velocidade de refino do Fundidor de Liga em +50% por nível.", 0, 0, 3, 3, "SPEED", "RARE"),
                ResearcherCard(4, "Albert Ein-stone", "Duplica a taxa de processamento do Montador Quântico.", 0, 0, 3, 4, "PRODUCTION", "RARE"),
                ResearcherCard(5, "Karl Mark-IV", "Soma +1.0 Camarada/segundo de forma permanente na corporação.", 0, 0, 5, 0, "COMRADE_GEN", "EPIC"),
                ResearcherCard(6, "Nebula Quantum Core", "Multiplica a produção global de TODAS as fábricas em +100% por nível.", 0, 0, 5, 0, "PRODUCTION", "SUPREME")
            ))

            // Seed Active Missions
            dao.saveActiveMissions(listOf(
                ActiveMission(1, "Possuir 5 Sol-Power Arrays", 1.0, 5.0, false, false, "OWN_BUILDING", 1, "", 15, 50, "COMUM"),
                ActiveMission(2, "Coletar 250 de Célula de Energia", 100.0, 250.0, false, false, "COLLECT_RESOURCE", 0, "Energy Cell", 20, 80, "EPICA"),
                ActiveMission(3, "Acumular 8.000 de Créditos (C$)", 5000.0, 8000.0, false, false, "EARN_CASH", 0, "", 30, 150, "SUPREMA")
            ))
        }
    }

    // Process Offline Income up to 12h
    suspend fun processOfflineDifference(curTime: Long = System.currentTimeMillis()): OfflineEarnings? = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction null
        require(curTime >= 0)
        if (curTime <= state.lastSavedTime) return@withTransaction null
        val diffMs = curTime - state.lastSavedTime
        if (diffMs < 10000) { // minimum 10 seconds empty
            saveValidatedState(state.copy(lastSavedTime = curTime))
            return@withTransaction null
        }

        val maxSeconds = StellarLab.offlineHours(state) * 3600L
        val seconds = (diffMs / 1000).coerceAtMost(maxSeconds) // max 12h para VIP, 8h para padrão
        val buildings = dao.getBuildingsDirect()
        val researchers = dao.getResearchersDirect()
        val start = dao.getResourcesDirect()
        var resources = start
        var cash = 0.0
        var comrades = 0.0
        var rate = state.comradesPerSec
        // Bounded to 16 hours. Replaying the same one-second rule preserves input and
        // surplus autosell semantics even when a downstream factory runs out of stock.
        repeat(seconds.toInt()) {
            if (it % 256 == 0) kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val tick = ProductionEngine.step(state, buildings, resources, researchers)
            resources = tick.resources
            cash += tick.cash
            comrades += tick.comrades
            rate = tick.comrades
        }
        require((state.cash + cash).isFinite() && (state.comrades + comrades).isFinite())
        dao.saveResources(resources)
        saveValidatedState(state.copy(cash = state.cash + cash, comrades = state.comrades + comrades,
            comradesPerSec = rate, lastSavedTime = curTime))
        val initial = start.associate { it.name to it.quantity }
        val net = resources.associate { it.name to (it.quantity - initial.getValue(it.name)).coerceAtLeast(0.0) }
            .filterValues { it > 0 }
        OfflineEarnings(seconds, net, cash)
    }

    // Full writes are only for fresh snapshots read inside a progress/production transaction.
    // UI operations use ProgressActions; core progression remains owned by its dedicated transactions.
    suspend fun saveGameState(state: GameState) = db.withTransaction {
        val current = dao.getGameStateDirect()
        saveValidatedState(if (current == null) state else state.copy(
            nebulaCores = current.nebulaCores, availableCores = current.availableCores,
            industrialEngineeringLevel = current.industrialEngineeringLevel,
            tacticalLogisticsLevel = current.tacticalLogisticsLevel,
            offlineReserveLevel = current.offlineReserveLevel, claimedCoreEvents = current.claimedCoreEvents
        ))
    }

    suspend fun buyStellarUpgrade(upgrade: StellarUpgrade, expectedLevel: Int): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val level = upgrade.level(state)
        if (level != expectedLevel) return@withTransaction false
        val cost = upgrade.costs.getOrNull(level) ?: return@withTransaction false
        if (state.availableCores < cost) return@withTransaction false
        val paid = state.copy(availableCores = state.availableCores - cost)
        saveValidatedState(when (upgrade) {
            StellarUpgrade.INDUSTRIAL -> paid.copy(industrialEngineeringLevel = level + 1)
            StellarUpgrade.TACTICAL -> paid.copy(tacticalLogisticsLevel = level + 1)
            StellarUpgrade.OFFLINE -> paid.copy(offlineReserveLevel = level + 1)
        })
        true
    }

    suspend fun claimCoreEvent(taskId: String, completed: Boolean): Boolean = db.withTransaction {
        if (!completed || taskId !in setOf("Energy Supplier", "Mega Titan Upgrader", "Celestial Capitalist")) return@withTransaction false
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val claimed = state.claimedCoreEvents.split("|").filter { it.isNotEmpty() }
        if (taskId in claimed) return@withTransaction false
        saveValidatedState(StellarLab.grant(state, 2).copy(
            starGems = state.starGems + 50, claimedCoreEvents = (claimed + taskId).joinToString("|")
        ))
        true
    }

    suspend fun getResourcesDirect(): List<ResourceInventory> = withContext(Dispatchers.IO) {
        dao.getResourcesDirect()
    }

    suspend fun getBuildingsDirect(): List<BusinessBuilding> = withContext(Dispatchers.IO) {
        dao.getBuildingsDirect()
    }

    suspend fun getCombatUnitsDirect(): List<CombatUnit> = withContext(Dispatchers.IO) {
        dao.getCombatUnitsDirect()
    }

    suspend fun updateResources(res: List<ResourceInventory>) = withContext(Dispatchers.IO) {
        dao.saveResources(res)
    }

    suspend fun updateResource(res: ResourceInventory) = withContext(Dispatchers.IO) {
        dao.saveResource(res)
    }

    fun calculateMultiUpgrade(baseCost: Double, costMultiplier: Double, currentLevel: Int, currentCash: Double, multiplier: String) =
        EconomyCosts.calculateMultiUpgrade(baseCost, costMultiplier, currentLevel, currentCash, multiplier)

    fun getBuildingComradeCost(buildingId: Int, currentLevel: Int): Double {
        val base = when (buildingId) {
            1 -> 1.0
            2 -> 10.0
            3 -> 100.0
            4 -> 1000.0
            5 -> 10000.0
            6 -> 100000.0
            7 -> 1000000.0
            8 -> 10000000.0
            else -> 1.0
        }
        return base * (1.08.pow(currentLevel))
    }

    suspend fun upgradeBuilding(buildingId: Int, multiplier: String): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withTransaction false
        
        val resCardList = dao.getResearchersDirect()
        val discountedBaseCost = EconomyCosts.buildingBase(b, resCardList, state)

        val calc = calculateMultiUpgrade(discountedBaseCost, b.costMultiplier, b.level, state.cash, multiplier)
        val levelsGained = calc.first
        val totalCost = calc.second

        if (levelsGained <= 0) return@withTransaction false

        if (state.cash >= totalCost) {
            saveValidatedState(state.copy(
                cash = state.cash - totalCost
            ))
            dao.updateBuilding(b.copy(level = b.level + levelsGained))
            return@withTransaction true
        }
        return@withTransaction false
    }

    suspend fun automateBuilding(buildingId: Int, gemCost: Long): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withTransaction false

        if (gemCost > 0 && b.level > 0 && state.starGems >= gemCost && !b.isAutomated) {
            saveValidatedState(state.copy(starGems = state.starGems - gemCost))
            dao.updateBuilding(b.copy(isAutomated = true))
            return@withTransaction true
        }
        return@withTransaction false
    }

    suspend fun toggleAutoSelling(buildingId: Int, autoSell: Boolean): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        if (autoSell && !state.hasAutoSellLicense) return@withTransaction false
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withTransaction false
        val isHighLevelResource = b.resourceProduced != "Energy Cell" && b.resourceProduced != "Iron Ore"
        if (!isHighLevelResource && autoSell) {
            return@withTransaction false
        }
        dao.updateBuilding(b.copy(isAutoSelling = autoSell))
        return@withTransaction true
    }

    suspend fun beginCombat(sector: Int): CombatBattle? = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction null
        if (sector !in 1..4 || state.highestCombatStage < sector) return@withTransaction null
        val level = Math.addExact(CombatRewards.victories(state, sector), 1)
        val reward = CombatRewards.forLevel(sector, level)
        dao.abandonBattles()
        val battle = CombatBattle(java.util.UUID.randomUUID().toString(), sector, level,
            reward.credits, reward.diamonds, reward.guildTokens)
        dao.insertBattle(battle)
        battle
    }

    suspend fun completeCombat(id: String, victory: Boolean): CombatReward? = db.withTransaction {
        val battle = dao.getBattle(id) ?: return@withTransaction null
        if (battle.status != "pending") return@withTransaction null
        val state = dao.getGameStateDirect() ?: return@withTransaction null
        dao.finishBattle(id, if (victory) "won" else "lost")
        if (!victory) return@withTransaction null
        saveValidatedState(state.copy(
            cash = state.cash + battle.credits,
            starGems = Math.addExact(state.starGems, battle.diamonds),
            guildTokens = Math.addExact(state.guildTokens, battle.guildTokens),
            highestCombatStage = maxOf(state.highestCombatStage, battle.sector + 1),
            stage1CompletedCount = if (battle.sector == 1) Math.addExact(state.stage1CompletedCount, 1) else state.stage1CompletedCount,
            stage2CompletedCount = if (battle.sector == 2) Math.addExact(state.stage2CompletedCount, 1) else state.stage2CompletedCount,
            stage3CompletedCount = if (battle.sector == 3) Math.addExact(state.stage3CompletedCount, 1) else state.stage3CompletedCount,
            stage4CompletedCount = if (battle.sector == 4) Math.addExact(state.stage4CompletedCount, 1) else state.stage4CompletedCount
        ))
        battle.reward()
    }

    suspend fun upgradeCombatUnit(unitId: Int): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val unitsList = dao.getCombatUnitsDirect()
        val u = unitsList.find { it.id == unitId } ?: return@withTransaction false
        
        val cost = EconomyCosts.combat(u, state)
        if (state.cash >= cost) {
            saveValidatedState(state.copy(cash = state.cash - cost))
            dao.updateCombatUnit(u.copy(
                level = u.level + 1,
                health = (u.health * 1.15).toInt(),
                attack = (u.attack * 1.15).toInt()
            ))
            return@withTransaction true
        }
        return@withTransaction false
    }

    // Sell resources inside market
    suspend fun sellResource(resource: String, amount: Double, pricePerUnit: Double): Boolean = db.withTransaction {
        if (!amount.isFinite() || amount <= 0 || !pricePerUnit.isFinite() || pricePerUnit <= 0) return@withTransaction false
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val resourcesList = dao.getResourcesDirect()
        val res = resourcesList.find { it.name == resource } ?: return@withTransaction false

        if (res.quantity >= amount) {
            val earned = amount * pricePerUnit * (1.0 + state.scienceCreditsLevel * 0.10)
            dao.saveResource(res.copy(quantity = res.quantity - amount))
            saveValidatedState(state.copy(cash = state.cash + earned))
            return@withTransaction true
        }
        return@withTransaction false
    }

    suspend fun addSuppliesAndCash(gemCost: Long, addedResources: Map<String, Double>, addedCash: Double): Boolean = db.withTransaction {
        if (gemCost <= 0 || !addedCash.isFinite() || addedCash < 0 ||
            addedResources.any { it.key !in ProductionEngine.basePrices || !it.value.isFinite() || it.value < 0 }) return@withTransaction false
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        if (state.starGems >= gemCost) {
            saveValidatedState(state.copy(
                starGems = state.starGems - gemCost,
                cash = state.cash + addedCash
            ))
            val currentRes = dao.getResourcesDirect()
            val updated = currentRes.map { r ->
                val add = addedResources[r.name] ?: 0.0
                r.copy(quantity = r.quantity + add)
            }
            require(updated.all { it.quantity.isFinite() && it.quantity >= 0 })
            dao.saveResources(updated)
            return@withTransaction true
        }
        return@withTransaction false
    }

    suspend fun exchangeCashForGems(costCoins: Double, rewardGems: Long): Boolean = db.withTransaction {
        if (!costCoins.isFinite() || costCoins <= 0.0 || rewardGems <= 0) return@withTransaction false
        val current = dao.getGameStateDirect() ?: return@withTransaction false
        if (current.cash < costCoins || current.starGems > Long.MAX_VALUE - rewardGems) return@withTransaction false
        saveValidatedState(current.copy(
            cash = current.cash - costCoins,
            starGems = current.starGems + rewardGems
        ))
        true
    }

    suspend fun applyTimeWarp(gemCost: Long, hours: Int): Boolean = db.withTransaction {
        if (gemCost <= 0 || hours <= 0) return@withTransaction false
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        if (state.starGems >= gemCost) {
            val buildingsList = dao.getBuildingsDirect()
            var productionPerHour = 0.0
            for (b in buildingsList) {
                if (b.level > 0) {
                    val rate = b.getActualProductionRate()
                    val baseValue = when (b.resourceProduced) {
                        "Energy Cell" -> 0.5
                        "Iron Ore" -> 1.5
                        "Hyperalloy" -> 5.0
                        "Quantum Chip" -> 15.0
                        "Organic Feedstock" -> 45.0
                        "Neural Implant" -> 150.0
                        "Antimatter Containment" -> 600.0
                        "Warp Drive" -> 2500.0
                        else -> 1.0
                    }
                    productionPerHour += rate * baseValue * 3600.0
                }
            }
            if (productionPerHour < 500.0) {
                productionPerHour = 500.0
            }
            val totalCashGained = productionPerHour * hours
            
            val currentRes = dao.getResourcesDirect()
            val updated = currentRes.map { r ->
                val matchingBuilding = buildingsList.find { it.resourceProduced == r.name }
                val rate = if (matchingBuilding != null && matchingBuilding.level > 0) {
                    matchingBuilding.getActualProductionRate()
                } else 0.0
                val amountGained = rate * 3600.0 * hours * 0.5
                r.copy(quantity = r.quantity + amountGained)
            }
            
            saveValidatedState(state.copy(
                starGems = state.starGems - gemCost,
                cash = state.cash + totalCashGained
            ))
            require(updated.all { it.quantity.isFinite() && it.quantity >= 0 })
            dao.saveResources(updated)
            return@withTransaction true
        }
        return@withTransaction false
    }

    fun getPrestigeMinCash(nebulaCores: Long): Double {
        // First ascension at 100k; quadratic growth instead of an exponential wall.
        return 100000.0 * (1.0 + nebulaCores.coerceAtLeast(0L) * 0.10).pow(2)
    }

    suspend fun performPrestige(): Boolean = db.withTransaction {
        val state = dao.getGameStateDirect() ?: return@withTransaction false
        val minCashRequired = getPrestigeMinCash(state.nebulaCores)
        if (state.cash < minCashRequired) return@withTransaction false
        
        val earnedCores = kotlin.math.floor(kotlin.math.sqrt(state.cash / minCashRequired)).toLong()
        saveValidatedState(StellarLab.grant(state, earnedCores).copy(cash = 5000.0))
        
        dao.saveResources(listOf(
            ResourceInventory("Energy Cell", 100.0),
            ResourceInventory("Iron Ore", 0.0),
            ResourceInventory("Hyperalloy", 0.0),
            ResourceInventory("Quantum Chip", 0.0),
            ResourceInventory("Organic Feedstock", 0.0),
            ResourceInventory("Neural Implant", 0.0),
            ResourceInventory("Antimatter Containment", 0.0),
            ResourceInventory("Warp Drive", 0.0)
        ))
        
        dao.saveBuildings(listOf(
            BusinessBuilding(
                id = 1,
                name = "Sol-Power Array",
                level = 1,
                isAutomated = true,
                baseCost = 80.0,
                costMultiplier = 1.12,
                productionRatePerLevel = 2.0,
                resourceProduced = "Energy Cell",
                inputResource = null,
                inputAmountPerSec = 0.0,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 2,
                name = "Deep-Core Drill",
                level = 0,
                isAutomated = false,
                baseCost = 350.0,
                costMultiplier = 1.14,
                productionRatePerLevel = 1.5,
                resourceProduced = "Iron Ore",
                inputResource = "Energy Cell",
                inputAmountPerSec = 0.5,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 3,
                name = "Alloy Smelter",
                level = 0,
                isAutomated = false,
                baseCost = 1500.0,
                costMultiplier = 1.16,
                productionRatePerLevel = 1.0,
                resourceProduced = "Hyperalloy",
                inputResource = "Iron Ore",
                inputAmountPerSec = 0.3,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 4,
                name = "Quantum Assembler",
                level = 0,
                isAutomated = false,
                baseCost = 10000.0,
                costMultiplier = 1.18,
                productionRatePerLevel = 0.6,
                resourceProduced = "Quantum Chip",
                inputResource = "Energy Cell",
                inputAmountPerSec = 1.5,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 5,
                name = "Biosphere Dome",
                level = 0,
                isAutomated = false,
                baseCost = 75000.0,
                costMultiplier = 1.20,
                productionRatePerLevel = 0.4,
                resourceProduced = "Organic Feedstock",
                inputResource = "Energy Cell",
                inputAmountPerSec = 3.0,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 6,
                name = "Cybernetics Lab",
                level = 0,
                isAutomated = false,
                baseCost = 500000.0,
                costMultiplier = 1.22,
                productionRatePerLevel = 0.25,
                resourceProduced = "Neural Implant",
                inputResource = "Quantum Chip",
                inputAmountPerSec = 0.1,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 7,
                name = "Antimatter Reactor",
                level = 0,
                isAutomated = false,
                baseCost = 4000000.0,
                costMultiplier = 1.24,
                productionRatePerLevel = 0.15,
                resourceProduced = "Antimatter Containment",
                inputResource = "Hyperalloy",
                inputAmountPerSec = 0.2,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 8,
                name = "Galactic Shipyard",
                level = 0,
                isAutomated = false,
                baseCost = 35000000.0,
                costMultiplier = 1.26,
                productionRatePerLevel = 0.08,
                resourceProduced = "Warp Drive",
                inputResource = "Neural Implant",
                inputAmountPerSec = 0.05,
                isAutoSelling = false
            )
        ))
        
        return@withTransaction true
    }

    // Reads and writes of a production tick must not straddle a restore transaction.
    suspend fun <T> runProgressTransaction(block: suspend () -> T): T = db.withTransaction {
        val result = block()
        BackupValidation.snapshot(requireNotNull(dao.getGameStateDirect()), dao.getResourcesDirect(),
            dao.getBuildingsDirect(), dao.getCombatUnitsDirect(), dao.getResearchersDirect(), dao.getActiveMissionsDirect())
        result
    }

    suspend fun <T> runProductionTransaction(block: suspend () -> T): T = db.withTransaction { block() }

    suspend fun updateProfile(avatarId: Int, skinId: Int, name: String) = db.withTransaction {
        val current = dao.getGameStateDirect() ?: return@withTransaction
        saveValidatedState(current.copy(
            selectedAvatarId = avatarId, selectedSkinId = skinId,
            companyName = name.takeIf { it.isNotBlank() } ?: current.companyName
        ))
    }

    suspend fun grantAdReward(token: String): Boolean = db.withTransaction {
        require(token.isNotBlank())
        check(dao.getGameStateDirect() != null) { "Jogo ainda não inicializado." }
        if (dao.insertAdReward(AdRewardReceipt(token, System.currentTimeMillis())) == -1L) {
            return@withTransaction false
        }
        check(dao.creditAdReward() == 1)
        true
    }

    /** Call only after a trusted server verified the token and product for this account. */
    suspend fun applyVerifiedPlayPurchase(tokenHash: String, productId: String,
        vip: Boolean, firstVipGift: Boolean): Boolean = db.withTransaction {
        require(tokenHash.matches(Regex("[0-9a-f]{64}")) && productId.isNotBlank())
        val current = dao.getGameStateDirect() ?: return@withTransaction false
        val firstLocalGrant = dao.insertPlayReceipt(
            PlayPurchaseReceipt(tokenHash, productId, System.currentTimeMillis())) != -1L
        val gift = if (vip && firstLocalGrant && firstVipGift) 1000L else 0L
        saveValidatedState(current.copy(
            isSubscribed = if (vip) true else current.isSubscribed,
            hasAutoSellLicense = if (vip) current.hasAutoSellLicense else true,
            autoSellLicenseSource = if (vip || current.hasAutoSellLicense) current.autoSellLicenseSource else "PLAY",
            starGems = Math.addExact(current.starGems, gift)
        ))
        firstLocalGrant
    }

    suspend fun revokeVipWhenAbsent() = db.withTransaction {
        val current = dao.getGameStateDirect() ?: return@withTransaction
        if (current.isSubscribed) saveValidatedState(current.copy(isSubscribed = false))
    }

    suspend fun revokePlayLicenseWhenAbsent() = db.withTransaction {
        val current = dao.getGameStateDirect() ?: return@withTransaction
        if (current.autoSellLicenseSource == "PLAY") saveValidatedState(current.copy(
            hasAutoSellLicense = false, autoSellLicenseSource = "NONE"))
    }

    suspend fun exportBackup(): BackupSnapshot = db.withTransaction {
        BackupSnapshot(
            requireNotNull(dao.getGameStateDirect()) { "Jogo ainda não inicializado." },
            dao.getResourcesDirect(), dao.getBuildingsDirect(), dao.getCombatUnitsDirect(),
            dao.getResearchersDirect(), dao.getActiveMissionsDirect(), dao.getAdReceipts()
        )
    }

    suspend fun restoreBackup(
        state: GameState,
        resources: List<ResourceInventory>,
        buildings: List<BusinessBuilding>,
        combatUnits: List<CombatUnit>,
        researchers: List<ResearcherCard>,
        missions: List<ActiveMission>,
        adReceipts: List<AdRewardReceipt> = emptyList()
    ) = db.withTransaction {
        BackupValidation.snapshot(state, resources, buildings, combatUnits, researchers, missions)
        require(adReceipts.all { it.token.isNotBlank() && it.rewardedAt >= 0 })
        require(adReceipts.map { it.token }.distinct().size == adReceipts.size)
        adReceipts.forEach { dao.insertAdReward(it) } // Union: never forget an already-paid local callback.
        dao.abandonBattles()
        dao.clearResources()
        dao.clearBuildings()
        dao.clearCombatUnits()
        dao.clearResearchers()
        dao.clearMissions()
        saveValidatedState(if (com.example.BuildConfig.DEBUG) state else state.copy(
            isSubscribed = false,
            hasAutoSellLicense = if (state.autoSellLicenseSource == "DEMO") false else state.hasAutoSellLicense,
            autoSellLicenseSource = if (state.autoSellLicenseSource == "DEMO") "NONE" else state.autoSellLicenseSource))
        dao.saveResources(resources)
        dao.saveBuildings(buildings)
        dao.saveCombatUnits(combatUnits)
        dao.saveResearchers(researchers)
        dao.saveActiveMissions(missions)
        check(dao.getBuildingsDirect() == buildings.sortedBy { it.id }) {
            "Os níveis das fábricas não puderam ser restaurados."
        }
        check(dao.getResourcesDirect().sortedBy { it.name } == resources.sortedBy { it.name }) {
            "Os estoques não puderam ser restaurados."
        }
    }

    // Support functions for Researchers
    suspend fun updateResearcher(r: ResearcherCard) = dao.updateResearcher(r)
    suspend fun saveResearchers(list: List<ResearcherCard>) = dao.saveResearchers(list)
    suspend fun getResearchersDirect(): List<ResearcherCard> = dao.getResearchersDirect()

    // Support functions for ActiveMissions
    suspend fun updateActiveMission(m: ActiveMission) = dao.updateActiveMission(m)
    suspend fun saveActiveMissions(list: List<ActiveMission>) = dao.saveActiveMissions(list)
    suspend fun getActiveMissionsDirect(): List<ActiveMission> = dao.getActiveMissionsDirect()

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                // Unknown schema versions must fail without deleting the player's save.
                val db = Room.databaseBuilder(
                    context.applicationContext, GameDatabase::class.java, "nexus_tycoon.db"
                ).addMigrations(GameDatabase.MIGRATION_8_9, GameDatabase.MIGRATION_9_10,
                    GameDatabase.MIGRATION_10_11, GameDatabase.MIGRATION_11_12,
                    GameDatabase.MIGRATION_12_13).build()
                val repo = GameRepository(db)
                INSTANCE = repo
                repo
            }
        }
    }
}

data class OfflineEarnings(
    val offlineSeconds: Long,
    val resourcesEarned: Map<String, Double>,
    val cashEarned: Double
)
