package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GameRepository.getInstance(application)
    val cloudSaveManager = CloudSaveManager(application)

    // State flows
    val gameState = repository.gameState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val resources = repository.resources.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val buildings = repository.buildings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val combatUnits = repository.combatUnits.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Offline income alert state
    private val _offlineEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val offlineEarnings: StateFlow<OfflineEarnings?> = _offlineEarnings.asStateFlow()

    // Fluctuating market prices
    private val _marketPrices = MutableStateFlow<Map<String, Double>>(
        mapOf("Energy Cell" to 1.5, "Iron Ore" to 6.0, "Hyperalloy" to 18.0, "Quantum Chip" to 125.0)
    )
    val marketPrices: StateFlow<Map<String, Double>> = _marketPrices.asStateFlow()

    // Fluctuating market trends
    private val _marketTrends = MutableStateFlow<Map<String, String>>(
        mapOf("Energy Cell" to "STEADY", "Iron Ore" to "STEADY", "Hyperalloy" to "STEADY", "Quantum Chip" to "STEADY")
    )
    val marketTrends: StateFlow<Map<String, String>> = _marketTrends.asStateFlow()

    // Combat game state
    private val _combatActive = MutableStateFlow(false)
    val combatActive: StateFlow<Boolean> = _combatActive.asStateFlow()

    private val _combatLogs = MutableStateFlow<List<String>>(emptyList())
    val combatLogs: StateFlow<List<String>> = _combatLogs.asStateFlow()

    private val _combatStatus = MutableStateFlow<CombatStatus>(CombatStatus.Idle)
    val combatStatus: StateFlow<CombatStatus> = _combatStatus.asStateFlow()

    val combatStages = listOf(
        CombatStage(1, "Scout Outpost", 350, 40, "Sol Star Core"),
        CombatStage(2, "Asteroid Drill Site", 750, 110, "Chronolith shard"),
        CombatStage(3, "Nebula Trading Port", 1800, 280, "Plasma Coil Node"),
        CombatStage(4, "Deep Sector Command", 4500, 750, "Nova Singularity Core")
    )

    private val _selectedStage = MutableStateFlow(1)
    val selectedStage: StateFlow<Int> = _selectedStage.asStateFlow()

    // Tactical Active Skills CD / State
    private val _shieldCooldown = MutableStateFlow(0) // seconds remaining
    val shieldCooldown: StateFlow<Int> = _shieldCooldown.asStateFlow()

    private val _beamCooldown = MutableStateFlow(0)
    val beamCooldown: StateFlow<Int> = _beamCooldown.asStateFlow()

    private val _overchargeCooldown = MutableStateFlow(0)
    val overchargeCooldown: StateFlow<Int> = _overchargeCooldown.asStateFlow()

    // Guild and Seasonal Missions State
    private val _guildMissions = MutableStateFlow(listOf(
        GuildMission("Iron Alliance Export", "Contribute 2,000 Iron Ore to the Alliance", 0.0, 2000.0, 30, "COMPLETE_EXPORT_IRON"),
        GuildMission("Guild Fleet Defense", "Win 3 Sector Security Patrols", 0.0, 3.0, 50, "WIN_PATROLS"),
        GuildMission("Quantum Syndicate Research", "Synthesize 100 Quantum Chips", 0.0, 100.0, 100, "RESEARCH_QU_CHIPS")
    ))
    val guildMissions: StateFlow<List<GuildMission>> = _guildMissions.asStateFlow()

    private val _seasonalChampionship = MutableStateFlow(
        SeasonalEvent("Nebula Star Event", 3, mutableListOf(
            EventTask("Energy Supplier", "Produce 1,500 Energy Cells", 0.0, 1500.0, false),
            EventTask("Mega Titan Upgrader", "Get any Security combat unit to level 5", 1.0, 5.0, false),
            EventTask("Celestial Capitalist", "Reach 50,000 Cash", 5000.0, 50000.0, false)
        ))
    )
    val seasonalChampionship: StateFlow<SeasonalEvent> = _seasonalChampionship.asStateFlow()

    // Global Trade Competition Board simulator (simulating live price actions from other factions)
    private val _globalTradeStandings = MutableStateFlow(listOf(
        LeaderboardEntry("Nova Core Inc (You)", 5000.0, true),
        LeaderboardEntry("Apex Industries", 12400.0, false),
        LeaderboardEntry("Sol Star Sagas", 8950.0, false),
        LeaderboardEntry("Hydra Conglomerate", 3200.0, false)
    ))
    val globalTradeStandings: StateFlow<List<LeaderboardEntry>> = _globalTradeStandings.asStateFlow()

    // Last combat replay info
    private val _lastCombatReplay = MutableStateFlow<CombatReplay?>(null)
    val lastCombatReplay: StateFlow<CombatReplay?> = _lastCombatReplay.asStateFlow()

    init {
        viewModelScope.launch {
            // Setup DB and local data seeding
            repository.checkAndInitialize()
            
            // Calculate Offline progress
            val earnings = repository.processOfflineDifference()
            if (earnings != null && (earnings.cashEarned > 0 || earnings.resourcesEarned.any { it.value > 0 })) {
                _offlineEarnings.value = earnings
            }

            // Launch Active Game Loops
            launch { startResourceTickLoop() }
            launch { startMarketVolatilityLoop() }
            launch { startMissionsProgressTracker() }
            launch { startCombatSkillCooldownLoop() }
        }
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    private suspend fun startResourceTickLoop() {
        while (true) {
            delay(1000)
            val activeState = repository.getGameStateDirect() ?: continue
            val bList = repository.getBuildingsDirect()
            if (bList.isEmpty()) continue

            // Fetch inventory map
            val resList = repository.getResourcesDirect()
            val startResMap = resList.associate { it.name to it.quantity }
            val resMap = startResMap.toMutableMap()
            var creditsGainedThisTick = 0.0

            // 1. Calculate production and update inventories (temporary)
            val actualProducedMap = mutableMapOf<String, Double>()
            for (building in bList) {
                if (building.level == 0) continue
                if (!building.isAutomated) continue

                val prestigeBonus = 1.0 + (activeState.nebulaCores * 0.10)
                val energyFactor = (if (activeState.isSubscribed) 1.5 else 1.0) * prestigeBonus
                val baseProduced = building.productionRatePerLevel * building.level * energyFactor
                
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
                        // Input starved: partial or zero production
                        scale = if (inputNeeded > 0) inputAvailable / inputNeeded else 0.0
                        resMap[inputName] = 0.0
                    }
                }

                val actualProduced = baseProduced * scale
                actualProducedMap[building.id.toString()] = actualProduced
                
                // Always add the production to resMap so downstream buildings can use it as input!
                resMap[building.resourceProduced] = (resMap[building.resourceProduced] ?: 0.0) + actualProduced
                
                incrementSeasonalProgress(building.resourceProduced, actualProduced)
                incrementGuildProgress(building.resourceProduced, actualProduced)
            }

            // 2. Process autosells of SURPLUS
            for (building in bList) {
                if (building.level == 0 || !building.isAutomated) continue
                val actualProduced = actualProducedMap[building.id.toString()] ?: 0.0
                if (actualProduced <= 0.0) continue

                val isHighLevelResource = building.resourceProduced == "Hyperalloy" || building.resourceProduced == "Quantum Chip"
                if (building.isAutoSelling && activeState.hasAutoSellLicense && isHighLevelResource) {
                    // How much did we net gain after this tick?
                    val startQty = startResMap[building.resourceProduced] ?: 0.0
                    val currentQty = resMap[building.resourceProduced] ?: 0.0
                    val netSurplus = (currentQty - startQty).coerceAtLeast(0.0)
                    
                    // The sellable amount is the minimum of the net surplus and the actual amount produced by this building
                    val sellAmount = minOf(actualProduced, netSurplus)
                    if (sellAmount > 0.0) {
                        val price = marketPrices.value[building.resourceProduced] ?: 1.0
                        creditsGainedThisTick += sellAmount * price
                        resMap[building.resourceProduced] = currentQty - sellAmount
                    }
                }
            }

            // Save updated resource quantities back to repository
            val updated = resMap.map { ResourceInventory(it.key, it.value) }
            repository.updateResources(updated)

            // Auto-save GameState with current time and added cash
            repository.saveGameState(activeState.copy(
                cash = activeState.cash + creditsGainedThisTick,
                lastSavedTime = System.currentTimeMillis()
            ))

            // Update Global standings in leaderboard
            val scoreVal = activeState.cash + (resMap["Energy Cell"] ?: 0.0) * 1.5 + 
                           (resMap["Iron Ore"] ?: 0.0) * 6.0 + 
                           (resMap["Hyperalloy"] ?: 0.0) * 18.0 + 
                           (resMap["Quantum Chip"] ?: 0.0) * 125.0
            
            _globalTradeStandings.update { current ->
                current.map { entry ->
                    if (entry.isPlayer) entry.copy(score = scoreVal)
                    else entry.copy(score = entry.score + Random.nextDouble(5.0, 25.0))
                }.sortedByDescending { it.score }
            }
            incrementSeasonalProgress("CREDITS_REACH", activeState.cash)
        }
    }

    private suspend fun startMarketVolatilityLoop() {
        val basePrices = mapOf(
            "Energy Cell" to 1.0,
            "Iron Ore" to 5.0,
            "Hyperalloy" to 15.0,
            "Quantum Chip" to 100.0
        )
        while (true) {
            delay(12000) // Price fluctuations every 12 seconds
            _marketPrices.update { current ->
                current.mapValues { (name, oldPrice) ->
                    val base = basePrices[name] ?: 1.0
                    val changePercent = Random.nextDouble(-0.15, 0.18) // -15% to +18%
                    val newPrice = (oldPrice + base * changePercent).coerceIn(base * 0.5, base * 2.5)
                    newPrice
                }
            }

            _marketTrends.update { current ->
                current.mapValues { (name, trend) ->
                    val oldVal = _marketPrices.value[name] ?: 1.0
                    val baseVal = basePrices[name] ?: 1.0
                    when {
                        oldVal > baseVal * 1.3 -> "BOOMING 📈"
                        oldVal < baseVal * 0.7 -> "DUMPING 📉"
                        else -> if (Random.nextBoolean()) "RISING ▲" else "FALLING ▼"
                    }
                }
            }
        }
    }

    private suspend fun startMissionsProgressTracker() {
        while (true) {
            delay(5000)
            // Periodic simulation of Guild companion activities!
            // Adds small increments to help complete Guild goals cooperatively
            _guildMissions.update { list ->
                list.map { m ->
                    if (m.progress < m.target) {
                        val externalContribution = when (m.taskId) {
                            "COMPLETE_EXPORT_IRON" -> Random.nextDouble(5.0, 20.0)
                            "RESEARCH_QU_CHIPS" -> Random.nextDouble(0.1, 0.5)
                            else -> 0.0
                        }
                        m.copy(progress = min(m.target, m.progress + externalContribution))
                    } else m
                }
            }
        }
    }

    private suspend fun startCombatSkillCooldownLoop() {
        while (true) {
            delay(1000)
            _shieldCooldown.update { if (it > 0) it - 1 else 0 }
            _beamCooldown.update { if (it > 0) it - 1 else 0 }
            _overchargeCooldown.update { if (it > 0) it - 1 else 0 }
        }
    }

    private fun incrementSeasonalProgress(key: String, amount: Double) {
        val currentEvent = _seasonalChampionship.value
        val list = currentEvent.tasks.map { task ->
            val match = when (task.taskId) {
                "Energy Supplier" -> key == "Energy Cell"
                "Celestial Capitalist" -> key == "CREDITS_REACH"
                else -> false
            }
            if (match) {
                val nextProg = if (key == "CREDITS_REACH") amount else task.progress + amount
                task.copy(progress = min(task.target, nextProg), isClaimed = nextProg >= task.target)
            } else {
                task
            }
        }.toMutableList()
        _seasonalChampionship.value = currentEvent.copy(tasks = list)
    }

    private fun incrementGuildProgress(key: String, amount: Double) {
        _guildMissions.update { list ->
            list.map { m ->
                val match = when (m.taskId) {
                    "COMPLETE_EXPORT_IRON" -> key == "Iron Ore"
                    "RESEARCH_QU_CHIPS" -> key == "Quantum Chip"
                    else -> false
                }
                if (match) {
                    m.copy(progress = min(m.target, m.progress + amount))
                } else m
            }
        }
    }

    val liveCombatStats = MutableStateFlow<ActiveBattleState?>(null)

    // User triggers Combat Mission Stage
    fun startCombatBattle(stageId: Int) {
        val stage = combatStages.find { it.id == stageId } ?: return
        val units = combatUnits.value
        val totalHealth = units.sumOf { it.health }
        val totalAttack = units.sumOf { it.attack }

        if (totalHealth == 0) {
            _combatLogs.value = listOf("Deploy error: Hire units in barracks first.")
            return
        }

        _selectedStage.value = stageId
        _combatActive.value = true
        _combatLogs.value = listOf(
            "INITIATING MISSION: ${stage.name}...",
            "Deploying Security squad... Total HP: $totalHealth, Attack Power: $totalAttack",
            "Enemy threat spotted: Raider Vanguard HP: ${stage.enemyHp}, Power: ${stage.enemyDmg}"
        )

        val battleState = ActiveBattleState(
            stageId = stageId,
            allyHp = totalHealth,
            allyMaxHp = totalHealth,
            allyDmg = totalAttack,
            enemyHp = stage.enemyHp,
            enemyMaxHp = stage.enemyHp,
            enemyDmg = stage.enemyDmg,
            round = 1,
            isShieldActive = false
        )
        liveCombatStats.value = battleState
        _combatStatus.value = CombatStatus.Running

        viewModelScope.launch {
            runBattleLoop(battleState)
        }
    }

    private suspend fun runBattleLoop(state: ActiveBattleState) {
        var current = state
        while (current.allyHp > 0 && current.enemyHp > 0 && _combatActive.value) {
            delay(1500) // combat round speed
            val roundLogs = mutableListOf<String>()
            
            // Player attacks Enemy
            val damageToEnemy = (current.allyDmg * Random.nextDouble(0.85, 1.15)).toInt()
            val newEnemyHp = max(0, current.enemyHp - damageToEnemy)
            roundLogs.add("• Ally squad attacks doing $damageToEnemy DMG!")
            
            // Counter check
            if (newEnemyHp <= 0) {
                roundLogs.add("🏆 Raider vanguard neutralized!")
                _combatLogs.update { it + roundLogs }
                current = current.copy(enemyHp = 0, round = current.round + 1)
                liveCombatStats.value = current
                handleBattleVictory(current.stageId)
                break
            }

            // Enemy attacks Player (shield blocks major part)
            val baseEnemyDmg = (current.enemyDmg * Random.nextDouble(0.9, 1.1)).toInt()
            val finalEnemyDmg = if (current.isShieldActive) {
                roundLogs.add("🛡️ Shield absorb activated! Deflecting 80% damage!")
                (baseEnemyDmg * 0.2).toInt()
            } else {
                baseEnemyDmg
            }
            
            val newAllyHp = max(0, current.allyHp - finalEnemyDmg)
            roundLogs.add("⚡ Raiders retaliate! Inflicting $finalEnemyDmg DMG to squad!")

            if (newAllyHp <= 0) {
                roundLogs.add("❌ Tactical squad retreated! Units damaged. Upgrade base defenses.")
                _combatLogs.update { it + roundLogs }
                current = current.copy(allyHp = 0, round = current.round + 1)
                liveCombatStats.value = current
                _combatStatus.value = CombatStatus.Defeated
                saveCombatReplay(current, victory = false)
                break
            }

            // Reset shield status after the turn it was active
            current = current.copy(
                allyHp = newAllyHp,
                enemyHp = newEnemyHp,
                round = current.round + 1,
                isShieldActive = false
            )
            liveCombatStats.value = current
            _combatLogs.update { it + roundLogs }
        }
    }

    // Play active Strategic tactical skills
    fun triggerShieldSkill() {
        if (_shieldCooldown.value > 0) return
        val current = liveCombatStats.value ?: return
        
        liveCombatStats.value = current.copy(isShieldActive = true)
        _shieldCooldown.value = 10 // 10s cooldown
        _combatLogs.update { it + "► Tactical Action: DEFENSIVE ENERGY SHIELD DEPLOYED!" }
    }

    fun triggerLaserStrikeSkill() {
        if (_beamCooldown.value > 0) return
        val current = liveCombatStats.value ?: return
        
        val blastDmg = current.allyDmg * 2
        val nextEnemyHp = max(0, current.enemyHp - blastDmg)
        liveCombatStats.value = current.copy(enemyHp = nextEnemyHp)
        _beamCooldown.value = 14 // 14s CD
        _combatLogs.update { it + "► Tactical Action: ORBITAL ION LASER STRIKE! Deals $blastDmg DMG!" }

        if (nextEnemyHp <= 0) {
            viewModelScope.launch {
                handleBattleVictory(current.stageId)
            }
        }
    }

    fun triggerOverchargeHealSkill() {
        if (_overchargeCooldown.value > 0) return
        val current = liveCombatStats.value ?: return
        
        val healAmt = (current.allyMaxHp * 0.4).toInt()
        val nextAllyHp = min(current.allyMaxHp, current.allyHp + healAmt)
        liveCombatStats.value = current.copy(allyHp = nextAllyHp)
        _overchargeCooldown.value = 18 // 18s CD
        _combatLogs.update { it + "► Tactical Action: NANITE AUTO-REPAIR SYSTEM. Restored $healAmt HP!" }
    }

    private suspend fun handleBattleVictory(stageId: Int) {
        val stage = combatStages.find { it.id == stageId } ?: return
        val state = gameState.value ?: return

        val lootCash = stageId * 2500.0
        val lootGems = stageId * 20L
        val lootTokens = stageId * 5L
        
        repository.saveGameState(state.copy(
            cash = state.cash + lootCash,
            starGems = state.starGems + lootGems,
            guildTokens = state.guildTokens + lootTokens,
            highestCombatStage = max(state.highestCombatStage, stageId + 1)
        ))

        _combatStatus.value = CombatStatus.Victory(lootCash, lootGems, lootTokens)
        
        // Save the replay details
        val finalState = liveCombatStats.value
        if (finalState != null) {
            saveCombatReplay(finalState, victory = true)
        }

        // Track missions progress
        _guildMissions.update { current ->
            current.map { m ->
                if (m.taskId == "WIN_PATROLS") m.copy(progress = min(m.target, m.progress + 1))
                else m
            }
        }

        // Update seasonal level tasks
        val currentEvent = _seasonalChampionship.value
        val list = currentEvent.tasks.map { task ->
            if (task.taskId == "Mega Titan Upgrader") {
                val maxLevel = combatUnits.value.maxOfOrNull { it.level } ?: 1
                task.copy(progress = maxLevel.toDouble(), isClaimed = maxLevel >= 5)
            } else task
        }.toMutableList()
        _seasonalChampionship.value = currentEvent.copy(tasks = list)
    }

    private fun saveCombatReplay(state: ActiveBattleState, victory: Boolean) {
        val replay = CombatReplay(
            stageName = combatStages.find { it.id == state.stageId }?.name ?: "Unknown Stage",
            roundsCount = state.round - 1,
            victory = victory,
            logsSnapshot = _combatLogs.value,
            timestamp = System.currentTimeMillis()
        )
        _lastCombatReplay.value = replay
    }

    fun closeBattle() {
        _combatActive.value = false
        _combatStatus.value = CombatStatus.Idle
        liveCombatStats.value = null
    }

    // Upgrades handlers
    fun upgradeBuilding(id: Int) {
        viewModelScope.launch {
            repository.upgradeBuilding(id)
        }
    }

    fun automateBuildingWithGems(id: Int) {
        viewModelScope.launch {
            repository.automateBuilding(id, gemCost = 35) // Cost 35 Gems
        }
    }

    fun toggleAutoSelling(id: Int, autoSell: Boolean) {
        val activeState = gameState.value ?: return
        if (autoSell && !activeState.hasAutoSellLicense) return // block if they don't have license
        viewModelScope.launch {
            repository.toggleAutoSelling(id, autoSell)
        }
    }

    fun buyAutoSellLicense() {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(
                hasAutoSellLicense = true
            ))
        }
    }

    fun buyAutoSellLicenseWithGems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        if (current.hasAutoSellLicense) {
            onError("Você já possui esta licença!")
            return
        }
        if (current.starGems < 300) {
            onError("Gemas Estelares insuficientes (necessário: 300 💎)!")
            return
        }
        viewModelScope.launch {
            repository.saveGameState(current.copy(
                starGems = current.starGems - 300,
                hasAutoSellLicense = true
            ))
            onSuccess()
        }
    }

    fun purchaseSupplyCrateWithGems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        if (current.starGems < 100) {
            onError("Gemas Estelares insuficientes (necessário: 100 💎)!")
            return
        }
        viewModelScope.launch {
            val resourcesMap = mapOf(
                "Energy Cell" to 500.0,
                "Iron Ore" to 300.0,
                "Hyperalloy" to 100.0,
                "Quantum Chip" to 25.0
            )
            val addedCash = 25000.0
            val success = repository.addSuppliesAndCash(100L, resourcesMap, addedCash)
            if (success) {
                onSuccess()
            } else {
                onError("Falha ao debitar gemas!")
            }
        }
    }

    fun purchaseTimeWarpWithGems(hours: Int, gemCost: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        if (current.starGems < gemCost) {
            onError("Gemas Estelares insuficientes (necessário: $gemCost 💎)!")
            return
        }
        viewModelScope.launch {
            val success = repository.applyTimeWarp(gemCost, hours)
            if (success) {
                onSuccess()
            } else {
                onError("Falha ao aplicar Dobra Temporal!")
            }
        }
    }

    fun performPrestigeAction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        if (current.cash < 100000.0) {
            onError("Você precisa de pelo menos 100.000 C$ para realizar a Ascensão Estelar!")
            return
        }
        val earnedCores = kotlin.math.floor(kotlin.math.sqrt(current.cash / 100000.0)).toLong()
        viewModelScope.launch {
            val success = repository.performPrestige(earnedCores)
            if (success) {
                onSuccess()
            } else {
                onError("Erro ao realizar a Ascensão Estelar!")
            }
        }
    }

    fun produceManual(buildingId: Int) {
        val activeState = gameState.value ?: return
        val bList = buildings.value
        val building = bList.find { it.id == buildingId } ?: return
        if (building.level == 0) return

        viewModelScope.launch {
            // Fetch inventory map
            val resList = resources.value
            val resMap = resList.associate { it.name to it.quantity }.toMutableMap()

            val prestigeBonus = 1.0 + (activeState.nebulaCores * 0.10)
            val energyFactor = (if (activeState.isSubscribed) 1.5 else 1.0) * prestigeBonus
            val baseProduced = building.productionRatePerLevel * building.level * energyFactor
            
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
            var cashGained = 0.0
            if (building.isAutoSelling && activeState.hasAutoSellLicense) {
                val price = marketPrices.value[building.resourceProduced] ?: 1.0
                cashGained = actualProduced * price
                incrementSeasonalProgress(building.resourceProduced, actualProduced)
                incrementGuildProgress(building.resourceProduced, actualProduced)
            } else {
                resMap[building.resourceProduced] = (resMap[building.resourceProduced] ?: 0.0) + actualProduced
                incrementSeasonalProgress(building.resourceProduced, actualProduced)
                incrementGuildProgress(building.resourceProduced, actualProduced)
            }

            // Save updated resource quantities back to repository
            val updated = resMap.map { ResourceInventory(it.key, it.value) }
            repository.updateResources(updated)

            if (cashGained > 0.0 || actualProduced > 0.0) {
                repository.saveGameState(activeState.copy(
                    cash = activeState.cash + cashGained
                ))
            }
        }
    }

    fun upgradeCombatUnit(id: Int) {
        viewModelScope.launch {
            repository.upgradeCombatUnit(id)
        }
    }

    fun sellResourceToMarket(name: String, amount: Double) {
        val price = marketPrices.value[name] ?: 1.0
        viewModelScope.launch {
            repository.sellResource(name, amount, price)
        }
    }

    // Profile skin changing & Avatars customization
    fun customizeProfile(avatarId: Int, skinId: Int, newName: String) {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(
                selectedAvatarId = avatarId,
                selectedSkinId = skinId,
                companyName = newName.takeIf { it.isNotBlank() } ?: current.companyName
            ))
        }
    }

    // Subscription & monetization interactions
    fun buySubscription() {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(
                isSubscribed = true,
                starGems = current.starGems + 1000 // VIP starter gift!
            ))
        }
    }

    fun buyPremiumPack(costCoins: Double, rewardGems: Long, packName: String) {
        val current = gameState.value ?: return
        if (current.cash >= costCoins) {
            viewModelScope.launch {
                repository.saveGameState(current.copy(
                    cash = current.cash - costCoins,
                    starGems = current.starGems + rewardGems
                ))
            }
        }
    }

    // Force claim seasonal item
    fun claimEventReward(taskId: String) {
        val current = gameState.value ?: return
        val event = _seasonalChampionship.value
        val task = event.tasks.find { it.taskId == taskId }
        if (task != null && task.progress >= task.target && !task.isClaimed) {
            viewModelScope.launch {
                repository.saveGameState(current.copy(
                    starGems = current.starGems + 50L,
                    nebulaCores = current.nebulaCores + 2
                ))
                val updatedTasks = event.tasks.map {
                    if (it.taskId == taskId) it.copy(isClaimed = true) else it
                }.toMutableList()
                _seasonalChampionship.value = event.copy(tasks = updatedTasks)
            }
        }
    }

    // Join guild simulator
    fun joinGuild(guildName: String) {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(guildName = guildName))
        }
    }

    fun googleSignIn(email: String) {
        viewModelScope.launch {
            cloudSaveManager.handleGoogleSignInSuccess(email)
        }
    }

    fun logoutFromCloud() {
        cloudSaveManager.logout()
    }

    fun uploadToCloud(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            cloudSaveManager.uploadBackup(repository, onSuccess, onError)
        }
    }

    fun downloadFromCloud(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            cloudSaveManager.downloadBackup(repository, onSuccess, onError)
        }
    }

    fun restoreRawPayload(payload: Map<String, Any>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                cloudSaveManager.restoreFromPayload(repository, payload)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erro desconhecido")
            }
        }
    }
}

// Sealed status class for active fights
sealed class CombatStatus {
    object Idle : CombatStatus()
    object Running : CombatStatus()
    data class Victory(val credits: Double, val gems: Long, val guildTokens: Long) : CombatStatus()
    object Defeated : CombatStatus()
}

data class LeaderboardEntry(
    val companyName: String,
    val score: Double,
    val isPlayer: Boolean
)

data class CombatStage(
    val id: Int,
    val name: String,
    val enemyHp: Int,
    val enemyDmg: Int,
    val rewardLoot: String
)

data class ActiveBattleState(
    val stageId: Int,
    val allyHp: Int,
    val allyMaxHp: Int,
    val allyDmg: Int,
    val enemyHp: Int,
    val enemyMaxHp: Int,
    val enemyDmg: Int,
    val round: Int,
    val isShieldActive: Boolean
)

data class CombatReplay(
    val stageName: String,
    val roundsCount: Int,
    val victory: Boolean,
    val logsSnapshot: List<String>,
    val timestamp: Long
)

// Guild goals
data class GuildMission(
    val name: String,
    val titleDescription: String,
    val progress: Double,
    val target: Double,
    val rewardContribution: Int,
    val taskId: String
)

data class SeasonalEvent(
    val name: String,
    val activeDaysRemaining: Int,
    val tasks: MutableList<EventTask>
)

data class EventTask(
    val taskId: String,
    val description: String,
    val progress: Double,
    val target: Double,
    val isClaimed: Boolean
)
