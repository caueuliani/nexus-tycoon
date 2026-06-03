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
import kotlin.math.pow
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
    val researchers = repository.researchers.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val activeMissions = repository.activeMissions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // Offline income alert state
    private val _upgradeMultiplier = MutableStateFlow("x1")
    val upgradeMultiplier: StateFlow<String> = _upgradeMultiplier.asStateFlow()

    fun setUpgradeMultiplier(multiplier: String) {
        _upgradeMultiplier.value = multiplier
    }

    private val _offlineEarnings = MutableStateFlow<OfflineEarnings?>(null)
    val offlineEarnings: StateFlow<OfflineEarnings?> = _offlineEarnings.asStateFlow()

    // Fluctuating market prices
    private val _marketPrices = MutableStateFlow<Map<String, Double>>(
        mapOf(
            "Energy Cell" to 1.5,
            "Iron Ore" to 6.0,
            "Hyperalloy" to 18.0,
            "Quantum Chip" to 125.0,
            "Organic Feedstock" to 550.0,
            "Neural Implant" to 2200.0,
            "Antimatter Containment" to 14000.0,
            "Warp Drive" to 95000.0
        )
    )
    val marketPrices: StateFlow<Map<String, Double>> = _marketPrices.asStateFlow()

    // Fluctuating market trends
    private val _marketTrends = MutableStateFlow<Map<String, String>>(
        mapOf(
            "Energy Cell" to "STEADY",
            "Iron Ore" to "STEADY",
            "Hyperalloy" to "STEADY",
            "Quantum Chip" to "STEADY",
            "Organic Feedstock" to "STEADY",
            "Neural Implant" to "STEADY",
            "Antimatter Containment" to "STEADY",
            "Warp Drive" to "STEADY"
        )
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
            try {
                // Setup DB and local data seeding
                repository.checkAndInitialize()
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "DB checkAndInitialize failed", e)
            }
            
            try {
                // Calculate Offline progress
                val earnings = repository.processOfflineDifference()
                if (earnings != null && (earnings.cashEarned > 0 || earnings.resourcesEarned.any { it.value > 0 })) {
                    _offlineEarnings.value = earnings
                }
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "processOfflineDifference failed", e)
            }

            // Launch Active Game Loops with individual safety wrappers
            launch {
                try {
                    startResourceTickLoop()
                } catch (e: Exception) {
                    android.util.Log.e("GameViewModel", "startResourceTickLoop failed", e)
                }
            }
            launch {
                try {
                    startMarketVolatilityLoop()
                } catch (e: Exception) {
                    android.util.Log.e("GameViewModel", "startMarketVolatilityLoop failed", e)
                }
            }
            launch {
                try {
                    startMissionsProgressTracker()
                } catch (e: Exception) {
                    android.util.Log.e("GameViewModel", "startMissionsProgressTracker failed", e)
                }
            }
            launch {
                try {
                    startCombatSkillCooldownLoop()
                } catch (e: Exception) {
                    android.util.Log.e("GameViewModel", "startCombatSkillCooldownLoop failed", e)
                }
            }
        }
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    private suspend fun startResourceTickLoop() {
        while (true) {
            try {
                delay(1000)
                val activeState = repository.getGameStateDirect() ?: continue
                val bList = repository.getBuildingsDirect()
                if (bList.isEmpty()) continue

                // Fetch scientists / researchers list 
                val resCardList = repository.getResearchersDirect()
                
                // Calculate Comrade passive generation multiplier (ID 5)
                val comradeGenCard = resCardList.find { it.id == 5 }
                val comradeScalar = 1.0 + (if (comradeGenCard != null && comradeGenCard.level > 0) comradeGenCard.getBoostValue() else 0.0)
                val scienceComradeMulti = 1.0 + (activeState.scienceComradeLevel * 0.20)
                val comradeRate = comradeScalar * 1.0 * scienceComradeMulti
                val newComrades = activeState.comrades + comradeRate

                // Fetch inventory map
                val resList = repository.getResourcesDirect()
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
                    
                    incrementSeasonalProgress(building.resourceProduced, actualProduced)
                    incrementGuildProgress(building.resourceProduced, actualProduced)
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
                            val price = marketPrices.value[building.resourceProduced] ?: 1.0
                            val scienceCreditsMulti = 1.0 + (activeState.scienceCreditsLevel * 0.10)
                            creditsGainedThisTick += sellAmount * price * scienceCreditsMulti
                            resMap[building.resourceProduced] = currentQty - sellAmount
                        }
                    }
                }

                // Save updated resource quantities back to repository
                val updated = resMap.map { ResourceInventory(it.key, it.value) }
                repository.updateResources(updated)

                // Save updated GameState along with passive comrades gains
                val newestState = repository.getGameStateDirect() ?: activeState
                val nextCash = newestState.cash + creditsGainedThisTick
                val nextComrades = newestState.comrades + comradeRate
                repository.saveGameState(newestState.copy(
                    cash = nextCash,
                    comrades = nextComrades,
                    comradesPerSec = comradeRate,
                    lastSavedTime = System.currentTimeMillis()
                ))

                // 3. Process Active Missions
                val activeMissionsList = repository.getActiveMissionsDirect()
                var missionUpdated = false
                val checkedMissions = activeMissionsList.map { m ->
                    if (!m.isCompleted) {
                        val latestProg = when (m.missionType) {
                            "OWN_BUILDING" -> {
                                val b = bList.find { it.id == m.targetId }
                                b?.level?.toDouble() ?: 0.0
                            }
                            "COLLECT_RESOURCE" -> {
                                resMap[m.targetString] ?: 0.0
                            }
                            "EARN_CASH" -> {
                                nextCash
                            }
                            else -> m.progress
                        }
                        val boundedProg = latestProg.coerceAtMost(m.target)
                        if (boundedProg != m.progress) {
                            missionUpdated = true
                            val isFinished = boundedProg >= m.target
                            m.copy(progress = boundedProg, isCompleted = isFinished)
                        } else m
                    } else m
                }
                if (missionUpdated) {
                    repository.saveActiveMissions(checkedMissions)
                }

                // Update Global standings in leaderboard
                var scoreVal = nextCash
                for ((resName, qty) in resMap) {
                    val price = marketPrices.value[resName] ?: 1.0
                    scoreVal += qty * price
                }
                
                _globalTradeStandings.update { current ->
                    current.map { entry ->
                        if (entry.isPlayer) entry.copy(score = scoreVal)
                        else entry.copy(score = entry.score + Random.nextDouble(5.0, 25.0))
                    }.sortedByDescending { it.score }
                }
                incrementSeasonalProgress("CREDITS_REACH", nextCash)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GameViewModel", "Error in Resource Tick Loop: ${e.message}", e)
            }
        }
    }

    private suspend fun startMarketVolatilityLoop() {
        val basePrices = mapOf(
            "Energy Cell" to 1.0,
            "Iron Ore" to 5.0,
            "Hyperalloy" to 15.0,
            "Quantum Chip" to 100.0,
            "Organic Feedstock" to 400.0,
            "Neural Implant" to 2000.0,
            "Antimatter Containment" to 12000.0,
            "Warp Drive" to 80000.0
        )
        while (true) {
            try {
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
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GameViewModel", "Error in market volatility loop iteration", e)
            }
        }
    }

    private suspend fun startMissionsProgressTracker() {
        while (true) {
            try {
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
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GameViewModel", "Error in missions progress loop iteration", e)
            }
        }
    }

    private suspend fun startCombatSkillCooldownLoop() {
        while (true) {
            try {
                delay(1000)
                _shieldCooldown.update { if (it > 0) it - 1 else 0 }
                _beamCooldown.update { if (it > 0) it - 1 else 0 }
                _overchargeCooldown.update { if (it > 0) it - 1 else 0 }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("GameViewModel", "Error in combat skill cooldown loop iteration", e)
            }
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
        val state = gameState.value ?: com.example.data.GameState()
        val completedCount = when (stageId) {
            1 -> state.stage1CompletedCount
            2 -> state.stage2CompletedCount
            3 -> state.stage3CompletedCount
            4 -> state.stage4CompletedCount
            else -> 0
        }

        val scaleHp = 1.0 + 0.15 * completedCount
        val scaleDmg = 1.0 + 0.12 * completedCount
        val scaledEnemyHp = (stage.enemyHp * scaleHp).toInt()
        val scaledEnemyDmg = (stage.enemyDmg * scaleDmg).toInt()

        val scienceCombatMulti = 1.0 + (state.scienceCombatLevel * 0.15)
        val units = combatUnits.value
        val totalHealth = (units.sumOf { it.health } * scienceCombatMulti).toInt()
        val totalAttack = (units.sumOf { it.attack } * scienceCombatMulti).toInt()

        if (totalHealth == 0) {
            _combatLogs.value = listOf("Deploy error: Hire units in barracks first.")
            return
        }

        _selectedStage.value = stageId
        _combatActive.value = true
        _combatLogs.value = listOf(
            "INITIATING MISSION: ${stage.name} (Nível de Ameaça: ${completedCount + 1})...",
            "Deploying Security squad... Total HP: $totalHealth, Attack Power: $totalAttack",
            "Enemy threat spotted: Raider Vanguard HP: $scaledEnemyHp, Power: $scaledEnemyDmg"
        )

        val battleState = ActiveBattleState(
            stageId = stageId,
            allyHp = totalHealth,
            allyMaxHp = totalHealth,
            allyDmg = totalAttack,
            enemyHp = scaledEnemyHp,
            enemyMaxHp = scaledEnemyHp,
            enemyDmg = scaledEnemyDmg,
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

        val completedCount = when (stageId) {
            1 -> state.stage1CompletedCount
            2 -> state.stage2CompletedCount
            3 -> state.stage3CompletedCount
            4 -> state.stage4CompletedCount
            else -> 0
        }

        val scaleCash = 1.0 + 0.06 * completedCount
        val scaleGems = 1.0 + 0.05 * completedCount
        val scaleTokens = 1.0 + 0.05 * completedCount

        val lootCash = (stageId * 2500.0) * scaleCash
        val lootGems = ((stageId * 20L) * scaleGems).toLong()
        val lootTokens = ((stageId * 5L) * scaleTokens).toLong()
        
        val nextState = state.copy(
            cash = state.cash + lootCash,
            starGems = state.starGems + lootGems,
            guildTokens = state.guildTokens + lootTokens,
            highestCombatStage = max(state.highestCombatStage, stageId + 1),
            stage1CompletedCount = if (stageId == 1) state.stage1CompletedCount + 1 else state.stage1CompletedCount,
            stage2CompletedCount = if (stageId == 2) state.stage2CompletedCount + 1 else state.stage2CompletedCount,
            stage3CompletedCount = if (stageId == 3) state.stage3CompletedCount + 1 else state.stage3CompletedCount,
            stage4CompletedCount = if (stageId == 4) state.stage4CompletedCount + 1 else state.stage4CompletedCount
        )
        repository.saveGameState(nextState)

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
            repository.upgradeBuilding(id, _upgradeMultiplier.value)
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
                "Quantum Chip" to 25.0,
                "Organic Feedstock" to 10.0,
                "Neural Implant" to 5.0,
                "Antimatter Containment" to 2.0,
                "Warp Drive" to 0.0
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

    fun getPrestigeMinCash(nebulaCores: Long): Double {
        return repository.getPrestigeMinCash(nebulaCores)
    }

    fun performPrestigeAction(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        val minCashRequired = getPrestigeMinCash(current.nebulaCores)
        if (current.cash < minCashRequired) {
            val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(minCashRequired)
            onError("Você precisa de pelo menos $formatted C$ para realizar a Ascensão Estelar!")
            return
        }
        val earnedCores = kotlin.math.floor(kotlin.math.sqrt(current.cash / minCashRequired)).toLong()
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
            val baseProduced = building.getActualProductionRate() * energyFactor
            
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
        val current = gameState.value
        val scienceCreditsMulti = 1.0 + (current?.scienceCreditsLevel ?: 0) * 0.10
        viewModelScope.launch {
            repository.sellResource(name, amount, price * scienceCreditsMulti)
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

    fun completeTutorial() {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(hasCompletedTutorial = true))
        }
    }

    fun resetTutorial() {
        val current = gameState.value ?: return
        viewModelScope.launch {
            repository.saveGameState(current.copy(hasCompletedTutorial = false))
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

    // --- ADVENTURE COMMUNIST MECHANICS ---

    fun buyAndOpenCapsuleWithGems(type: String, onSuccess: (Long, List<String>) -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        val cost = when (type) {
            "COMUM" -> 50
            "EPICA" -> 150
            "SUPREMA" -> 400
            else -> 0
        }
        if (current.starGems < cost) {
            onError("Gemas insuficientes para abrir Cápsula ${type}!")
            return
        }
        
        viewModelScope.launch {
            val scientists = repository.getResearchersDirect()
            val scienceGain = when (type) {
                "COMUM" -> Random.nextLong(15, 40)
                "EPICA" -> Random.nextLong(50, 110)
                "SUPREMA" -> Random.nextLong(150, 350)
                else -> 10L
            }
            
            val cardsNumber = when (type) {
                "COMUM" -> 3
                "EPICA" -> 6
                "SUPREMA" -> 12
                else -> 1
            }
            
            val obtainedCards = mutableListOf<String>()
            val updatedScientists = scientists.map { s ->
                var gained = 0
                for (i in 0 until cardsNumber) {
                    val roll = Random.nextDouble()
                    val matchChance = when (s.rarity) {
                        "COMMON" -> 0.40
                        "RARE" -> 0.20
                        "EPIC" -> 0.10
                        "SUPREME" -> if (type == "SUPREMA") 0.15 else 0.03
                        else -> 0.15
                    }
                    if (roll < matchChance) gained++
                }
                
                if (gained > 0) {
                    obtainedCards.add("${s.name} (x$gained)")
                    s.copy(cardsCollected = s.cardsCollected + gained)
                } else s
            }
            
            repository.saveResearchers(updatedScientists)
            repository.saveGameState(current.copy(
                starGems = current.starGems - cost,
                science = current.science + scienceGain
            ))
            
            onSuccess(scienceGain, obtainedCards)
        }
    }

    fun upgradeResearcherCard(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val current = gameState.value ?: return
        viewModelScope.launch {
            val scientists = repository.getResearchersDirect()
            val researcher = scientists.find { it.id == id } ?: return@launch
            
            val cost = researcher.getUpgradeScienceCost()
            if (current.science < cost) {
                onError("Ciência insuficiente! Requer: ${cost} 🧪")
                return@launch
            }
            if (researcher.cardsCollected < researcher.cardsNeededToUpgrade) {
                onError("Cartas insuficientes! Requer: ${researcher.cardsCollected}/${researcher.cardsNeededToUpgrade} 📇")
                return@launch
            }
            
            val nextLevel = researcher.level + 1
            val cardsUsed = researcher.cardsNeededToUpgrade
            val nextCardsNeeded = researcher.cardsNeededToUpgrade + (4 * nextLevel)
            
            val updated = researcher.copy(
                level = nextLevel,
                cardsCollected = researcher.cardsCollected - cardsUsed,
                cardsNeededToUpgrade = nextCardsNeeded
            )
            
            repository.updateResearcher(updated)
            repository.saveGameState(current.copy(
                science = current.science - cost
            ))
            onSuccess()
        }
    }

    fun claimMissionReward(missionId: Int, onSuccess: (Long, Long, String, List<String>) -> Unit) {
        viewModelScope.launch {
            val freshState = repository.getGameStateDirect() ?: return@launch
            val m = repository.getActiveMissionsDirect().find { it.id == missionId } ?: return@launch
            if (!m.isCompleted || m.isClaimed) return@launch
            
            // Mark as claimed
            repository.updateActiveMission(m.copy(isClaimed = true))
            
            val scienceReward = m.rewardScience
            val gemsReward = m.rewardGems
            
            val scientists = repository.getResearchersDirect()
            val cardsNumber = when (m.rewardCapsuleType) {
                "COMUM" -> 2
                "EPICA" -> 5
                "SUPREMA" -> 10
                else -> 1
            }
            
            val obtainedCards = mutableListOf<String>()
            val updatedScientists = scientists.map { s ->
                var gained = 0
                for (i in 0 until cardsNumber) {
                    val roll = Random.nextDouble()
                    val matchChance = when (s.rarity) {
                        "COMMON" -> 0.45
                        "RARE" -> 0.22
                        "EPIC" -> 0.12
                        "SUPREME" -> 0.05
                        else -> 0.15
                    }
                    if (roll < matchChance) gained++
                }
                
                if (gained > 0) {
                    obtainedCards.add("${s.name} (x$gained)")
                    s.copy(cardsCollected = s.cardsCollected + gained)
                } else s
            }
            
            repository.saveResearchers(updatedScientists)
            
            val nextCompleted = freshState.completedRankMissions + 1
            repository.saveGameState(freshState.copy(
                starGems = freshState.starGems + gemsReward,
                science = freshState.science + scienceReward,
                completedRankMissions = nextCompleted
            ))
            
            onSuccess(gemsReward, scienceReward, m.rewardCapsuleType, obtainedCards)
        }
    }

    fun rankUpPlayer(onSuccess: (Int, Long, List<String>) -> Unit) {
        viewModelScope.launch {
            val freshState = repository.getGameStateDirect() ?: return@launch
            val nextRank = freshState.playerRank + 1
            val newMissions = listOf(
                ActiveMission(
                    id = 1,
                    description = "Possuir ${3 + nextRank * 2} Sol-Power Arrays",
                    progress = 0.0,
                    target = (3 + nextRank * 2).toDouble(),
                    isCompleted = false,
                    isClaimed = false,
                    missionType = "OWN_BUILDING",
                    targetId = 1,
                    rewardGems = (10 + nextRank * 5).toLong(),
                    rewardScience = (40 + nextRank * 20).toLong(),
                    rewardCapsuleType = if (nextRank % 3 == 0) "SUPREMA" else "EPICA"
                ),
                ActiveMission(
                    id = 2,
                    description = "Coletar ${formatCredits(500.0 * 2.5.pow(nextRank - 1))} de ${getBuildingResourceNameByRank(nextRank)}",
                    progress = 0.0,
                    target = 500.0 * 2.5.pow(nextRank - 1),
                    isCompleted = false,
                    isClaimed = false,
                    missionType = "COLLECT_RESOURCE",
                    targetString = getBuildingResourceNameByRank(nextRank),
                    rewardGems = (15 + nextRank * 5).toLong(),
                    rewardScience = (50 + nextRank * 20).toLong(),
                    rewardCapsuleType = "EPICA"
                ),
                ActiveMission(
                    id = 3,
                    description = "Acumular ${formatCredits(8000.0 * 4.0.pow(nextRank - 1))} de Créditos (C$)",
                    progress = 0.0,
                    target = 8000.0 * 4.0.pow(nextRank - 1),
                    isCompleted = false,
                    isClaimed = false,
                    missionType = "EARN_CASH",
                    rewardGems = (20 + nextRank * 5).toLong(),
                    rewardScience = (60 + nextRank * 20).toLong(),
                    rewardCapsuleType = "SUPREMA"
                )
            )
            repository.saveActiveMissions(newMissions)
            
            val scientists = repository.getResearchersDirect()
            val rewardScienceGain = (100 * nextRank).toLong()
            val rewardGemsGain = (50 + nextRank * 10).toLong()
            
            val obtainedCards = mutableListOf<String>()
            val updatedScientists = scientists.map { s ->
                val gained = Random.nextInt(1, 3)
                obtainedCards.add("${s.name} (x$gained)")
                s.copy(cardsCollected = s.cardsCollected + gained)
            }
            repository.saveResearchers(updatedScientists)
            
            repository.saveGameState(freshState.copy(
                playerRank = nextRank,
                completedRankMissions = 0,
                starGems = freshState.starGems + rewardGemsGain,
                science = freshState.science + rewardScienceGain
            ))
            
            onSuccess(nextRank, rewardScienceGain, obtainedCards)
        }
    }

    fun purchaseScienceUpgrade(upgradeType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val freshState = repository.getGameStateDirect() ?: return@launch
            val currentLevel = when (upgradeType) {
                "PROD" -> freshState.scienceProdLevel
                "CREDITS" -> freshState.scienceCreditsLevel
                "COMRADE" -> freshState.scienceComradeLevel
                "COMBAT" -> freshState.scienceCombatLevel
                else -> 0
            }
            val cost = when (upgradeType) {
                "PROD" -> (100 * 1.5.pow(currentLevel)).toLong()
                "CREDITS" -> (150 * 1.6.pow(currentLevel)).toLong()
                "COMRADE" -> (80 * 1.5.pow(currentLevel)).toLong()
                "COMBAT" -> (200 * 1.7.pow(currentLevel)).toLong()
                else -> 999999L
            }
            if (freshState.science < cost) {
                onError("Ciência insuficiente! Requer: $cost 🧪")
                return@launch
            }
            
            val updatedState = when (upgradeType) {
                "PROD" -> freshState.copy(science = freshState.science - cost, scienceProdLevel = currentLevel + 1)
                "CREDITS" -> freshState.copy(science = freshState.science - cost, scienceCreditsLevel = currentLevel + 1)
                "COMRADE" -> freshState.copy(science = freshState.science - cost, scienceComradeLevel = currentLevel + 1)
                "COMBAT" -> freshState.copy(science = freshState.science - cost, scienceCombatLevel = currentLevel + 1)
                else -> freshState
            }
            repository.saveGameState(updatedState)
            onSuccess()
        }
    }
    
    private fun getBuildingResourceNameByRank(rank: Int): String {
        return when (rank % 8) {
            1 -> "Energy Cell"
            2 -> "Iron Ore"
            3 -> "Hyperalloy"
            4 -> "Quantum Chip"
            5 -> "Organic Feedstock"
            6 -> "Neural Implant"
            7 -> "Antimatter Containment"
            0 -> "Warp Drive"
            else -> "Energy Cell"
        }
    }

    private fun formatCredits(amount: Double): String {
        return when {
            amount >= 1_000_000_000_000.0 -> String.format("%.2f T", amount / 1_000_000_000_000.0)
            amount >= 1_000_000_000.0 -> String.format("%.2f B", amount / 1_000_000_000.0)
            amount >= 1_000_000.0 -> String.format("%.2f M", amount / 1_000_000.0)
            amount >= 1_000.0 -> String.format("%.2f K", amount / 1_000.0)
            else -> String.format("%.1f", amount)
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
