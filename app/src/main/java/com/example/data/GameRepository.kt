package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class GameRepository(private val db: GameDatabase) {
    private val dao = db.gameDao

    val gameState: Flow<GameState?> = dao.getGameStateFlow()
    val resources: Flow<List<ResourceInventory>> = dao.getResourcesFlow()
    val buildings: Flow<List<BusinessBuilding>> = dao.getBuildingsFlow()
    val combatUnits: Flow<List<CombatUnit>> = dao.getCombatUnitsFlow()

    suspend fun getGameStateDirect(): GameState? = dao.getGameStateDirect()

    suspend fun checkAndInitialize() = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect()
        if (state == null) {
            // Seed base state
            dao.saveGameState(GameState())
            
            // Seed Resources
            dao.saveResources(listOf(
                ResourceInventory("Energy Cell", 100.0),
                ResourceInventory("Iron Ore", 0.0),
                ResourceInventory("Hyperalloy", 0.0),
                ResourceInventory("Quantum Chip", 0.0)
            ))

            // Seed Buildings
            dao.saveBuildings(listOf(
                BusinessBuilding(
                    id = 1,
                    name = "Sol-Power Array",
                    level = 1,
                    isAutomated = true,
                    baseCost = 80.0,
                    costMultiplier = 1.13,
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
                    baseCost = 250.0,
                    costMultiplier = 1.16,
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
                    baseCost = 1200.0,
                    costMultiplier = 1.20,
                    productionRatePerLevel = 0.8,
                    resourceProduced = "Hyperalloy",
                    inputResource = "Iron Ore",
                    inputAmountPerSec = 1.2,
                    isAutoSelling = false
                ),
                BusinessBuilding(
                    id = 4,
                    name = "Quantum Assembler",
                    level = 0,
                    isAutomated = false,
                    baseCost = 8500.0,
                    costMultiplier = 1.25,
                    productionRatePerLevel = 0.2,
                    resourceProduced = "Quantum Chip",
                    inputResource = "Energy Cell",
                    inputAmountPerSec = 4.0,
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
        }
    }

    // Process Offline Income up to 12h
    suspend fun processOfflineDifference(): OfflineEarnings? = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext null
        val curTime = System.currentTimeMillis()
        val diffMs = curTime - state.lastSavedTime
        if (diffMs < 10000) { // minimum 10 seconds empty
            dao.saveGameState(state.copy(lastSavedTime = curTime))
            return@withContext null
        }

        val maxSeconds = if (state.isSubscribed) 43200L else 28800L
        val seconds = (diffMs / 1000).coerceAtMost(maxSeconds) // max 12h para VIP, 8h para padrão
        val buildingsList = dao.getBuildingsDirect()
        val currentResources = dao.getResourcesDirect().associate { it.name to it.quantity }.toMutableMap()

        val startResources = currentResources.toMap()
        val earnings = mutableMapOf<String, Double>()
        var creditEarned = 0.0
        val tempProducedMap = mutableMapOf<Int, Double>()
        val prestigeBonus = 1.0 + (state.nebulaCores * 0.10)
        val factor = (if (state.isSubscribed) 1.5 else 1.0) * prestigeBonus

        // 1. Calculate production with input dependencies sequentially (upstream to downstream)
        for (building in buildingsList) {
            if (building.level == 0 || !building.isAutomated) continue
            var totalProduction = building.productionRatePerLevel * building.level * seconds * factor
            
            // Check inputs if any
            val input = building.inputResource
            if (input != null) {
                val inputAvail = currentResources[input] ?: 0.0
                val totalInputNeeded = building.inputAmountPerSec * building.level * seconds
                if (totalInputNeeded > inputAvail) {
                    // Production is limited by available input
                    val efficiency = if (totalInputNeeded > 0) inputAvail / totalInputNeeded else 0.0
                    totalProduction *= efficiency
                    // Reduce input to 0
                    currentResources[input] = 0.0
                } else {
                    currentResources[input] = inputAvail - totalInputNeeded
                }
            }
            
            val prodRes = building.resourceProduced
            tempProducedMap[building.id] = totalProduction
            currentResources[prodRes] = (currentResources[prodRes] ?: 0.0) + totalProduction
        }

        // 2. Process Auto-Sells and actual increases of SURPLUS
        val basePrices = mapOf(
            "Energy Cell" to 1.0,
            "Iron Ore" to 5.0,
            "Hyperalloy" to 15.0,
            "Quantum Chip" to 100.0
        )
        for (building in buildingsList) {
            if (building.level == 0 || !building.isAutomated) continue
            val actualProduced = tempProducedMap[building.id] ?: 0.0
            if (actualProduced <= 0.0) continue

            val prodRes = building.resourceProduced
            val isHighLevelResource = prodRes == "Hyperalloy" || prodRes == "Quantum Chip"
            if (building.isAutoSelling && state.hasAutoSellLicense && isHighLevelResource) {
                val startQty = startResources[prodRes] ?: 0.0
                val currentQty = currentResources[prodRes] ?: 0.0
                val netSurplus = (currentQty - startQty).coerceAtLeast(0.0)
                
                val sellAmount = minOf(actualProduced, netSurplus)
                if (sellAmount > 0.0) {
                    val basePrice = basePrices[prodRes] ?: 1.0
                    creditEarned += sellAmount * basePrice
                    currentResources[prodRes] = currentQty - sellAmount
                }
                
                // If any portion of the production was kept (e.g., due to rounding or limited sell), track it
                val netIncrease = (currentResources[prodRes] ?: 0.0) - startQty
                if (netIncrease > 0.1) {
                    earnings[prodRes] = (earnings[prodRes] ?: 0.0) + netIncrease
                }
            } else {
                earnings[prodRes] = (earnings[prodRes] ?: 0.0) + actualProduced
            }
        }

        // Save resources and updated state
        val updatedResources = currentResources.map { ResourceInventory(it.key, it.value) }
        dao.saveResources(updatedResources)
        dao.saveGameState(state.copy(
            cash = state.cash + creditEarned,
            lastSavedTime = curTime
        ))

        return@withContext OfflineEarnings(seconds, earnings, creditEarned)
    }

    suspend fun saveGameState(state: GameState) = withContext(Dispatchers.IO) {
        dao.saveGameState(state)
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

    suspend fun upgradeBuilding(buildingId: Int): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withContext false
        val cost = b.baseCost * b.costMultiplier.pow(b.level)

        if (state.cash >= cost) {
            dao.saveGameState(state.copy(cash = state.cash - cost))
            dao.updateBuilding(b.copy(level = b.level + 1))
            return@withContext true
        }
        return@withContext false
    }

    suspend fun automateBuilding(buildingId: Int, gemCost: Long): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withContext false

        if (state.starGems >= gemCost && !b.isAutomated) {
            dao.saveGameState(state.copy(starGems = state.starGems - gemCost))
            dao.updateBuilding(b.copy(isAutomated = true))
            return@withContext true
        }
        return@withContext false
    }

    suspend fun toggleAutoSelling(buildingId: Int, autoSell: Boolean): Boolean = withContext(Dispatchers.IO) {
        val buildingsList = dao.getBuildingsDirect()
        val b = buildingsList.find { it.id == buildingId } ?: return@withContext false
        val isHighLevelResource = b.resourceProduced == "Hyperalloy" || b.resourceProduced == "Quantum Chip"
        if (!isHighLevelResource && autoSell) {
            return@withContext false
        }
        dao.updateBuilding(b.copy(isAutoSelling = autoSell))
        return@withContext true
    }

    suspend fun upgradeCombatUnit(unitId: Int): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        val unitsList = dao.getCombatUnitsDirect()
        val u = unitsList.find { it.id == unitId } ?: return@withContext false
        
        val cost = u.upgradeCost * u.costMultiplier.pow(u.level - 1)
        if (state.cash >= cost) {
            dao.saveGameState(state.copy(cash = state.cash - cost))
            dao.updateCombatUnit(u.copy(
                level = u.level + 1,
                health = (u.health * 1.15).toInt(),
                attack = (u.attack * 1.15).toInt()
            ))
            return@withContext true
        }
        return@withContext false
    }

    // Sell resources inside market
    suspend fun sellResource(resource: String, amount: Double, pricePerUnit: Double): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        val resourcesList = dao.getResourcesDirect()
        val res = resourcesList.find { it.name == resource } ?: return@withContext false

        if (res.quantity >= amount) {
            val earned = amount * pricePerUnit
            dao.saveResource(res.copy(quantity = res.quantity - amount))
            dao.saveGameState(state.copy(cash = state.cash + earned))
            return@withContext true
        }
        return@withContext false
    }

    suspend fun addSuppliesAndCash(gemCost: Long, addedResources: Map<String, Double>, addedCash: Double): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        if (state.starGems >= gemCost) {
            dao.saveGameState(state.copy(
                starGems = state.starGems - gemCost,
                cash = state.cash + addedCash
            ))
            val currentRes = dao.getResourcesDirect()
            val updated = currentRes.map { r ->
                val add = addedResources[r.name] ?: 0.0
                r.copy(quantity = r.quantity + add)
            }
            dao.saveResources(updated)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun applyTimeWarp(gemCost: Long, hours: Int): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        if (state.starGems >= gemCost) {
            val buildingsList = dao.getBuildingsDirect()
            var productionPerHour = 0.0
            for (b in buildingsList) {
                if (b.level > 0) {
                    val rate = b.productionRatePerLevel * b.level
                    val baseValue = when (b.resourceProduced) {
                        "Energy Cell" -> 0.5
                        "Iron Ore" -> 1.5
                        "Hyperalloy" -> 4.5
                        "Quantum Chip" -> 15.0
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
                    matchingBuilding.productionRatePerLevel * matchingBuilding.level
                } else 0.0
                val amountGained = rate * 3600.0 * hours * 0.5
                r.copy(quantity = r.quantity + amountGained)
            }
            
            dao.saveGameState(state.copy(
                starGems = state.starGems - gemCost,
                cash = state.cash + totalCashGained
            ))
            dao.saveResources(updated)
            return@withContext true
        }
        return@withContext false
    }

    suspend fun performPrestige(earnedCores: Long): Boolean = withContext(Dispatchers.IO) {
        val state = dao.getGameStateDirect() ?: return@withContext false
        if (state.cash < 100000.0) return@withContext false
        
        dao.saveGameState(state.copy(
            cash = 5000.0,
            nebulaCores = state.nebulaCores + earnedCores
        ))
        
        dao.saveResources(listOf(
            ResourceInventory("Energy Cell", 100.0),
            ResourceInventory("Iron Ore", 0.0),
            ResourceInventory("Hyperalloy", 0.0),
            ResourceInventory("Quantum Chip", 0.0)
        ))
        
        dao.saveBuildings(listOf(
            BusinessBuilding(
                id = 1,
                name = "Sol-Power Array",
                level = 1,
                isAutomated = true,
                baseCost = 80.0,
                costMultiplier = 1.13,
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
                baseCost = 250.0,
                costMultiplier = 1.16,
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
                baseCost = 1200.0,
                costMultiplier = 1.20,
                productionRatePerLevel = 0.8,
                resourceProduced = "Hyperalloy",
                inputResource = "Iron Ore",
                inputAmountPerSec = 1.2,
                isAutoSelling = false
            ),
            BusinessBuilding(
                id = 4,
                name = "Quantum Assembler",
                level = 0,
                isAutomated = false,
                baseCost = 8500.0,
                costMultiplier = 1.25,
                productionRatePerLevel = 0.2,
                resourceProduced = "Quantum Chip",
                inputResource = "Energy Cell",
                inputAmountPerSec = 4.0,
                isAutoSelling = false
            )
        ))
        
        return@withContext true
    }

    suspend fun restoreBackup(
        state: GameState,
        resources: List<ResourceInventory>,
        buildings: List<BusinessBuilding>,
        combatUnits: List<CombatUnit>
    ) = withContext(Dispatchers.IO) {
        dao.saveGameState(state)
        dao.saveResources(resources)
        dao.saveBuildings(buildings)
        dao.saveCombatUnits(combatUnits)
    }

    companion object {
        @Volatile
        private var INSTANCE: GameRepository? = null

        fun getInstance(context: Context): GameRepository {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "nexus_tycoon.db"
                ).fallbackToDestructiveMigration().build()
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
