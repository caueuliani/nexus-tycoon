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

    private val _storageError = MutableStateFlow<String?>(null)
    val storageError: StateFlow<String?> = _storageError.asStateFlow()

    // State flows
    val gameState = repository.gameState.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(null)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val resources = repository.resources.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(emptyList())
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val buildings = repository.buildings.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(emptyList())
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val combatUnits = repository.combatUnits.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(emptyList())
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val researchers = repository.researchers.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(emptyList())
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val activeMissions = repository.activeMissions.catch {
        _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados."
        emit(emptyList())
    }.stateIn(
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

    private val combat = CombatEngine(viewModelScope, ::prepareBattle, repository::completeCombat, ::recordCombatVictory)
    val combatActive = combat.active
    val combatLogs = combat.logs
    val combatStatus = combat.status
    val liveCombatStats = combat.live
    val selectedStage = combat.selected
    val shieldCooldown = combat.shieldCooldown
    val beamCooldown = combat.beamCooldown
    val overchargeCooldown = combat.healCooldown
    val lastCombatReplay = combat.replay

    val combatStages = listOf(
        CombatStage(1, "Scout Outpost", 350, 40, "Sol Star Core"),
        CombatStage(2, "Asteroid Drill Site", 750, 110, "Chronolith shard"),
        CombatStage(3, "Nebula Trading Port", 1800, 280, "Plasma Coil Node"),
        CombatStage(4, "Deep Sector Command", 4500, 750, "Nova Singularity Core")
    )

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
    val seasonalChampionship: StateFlow<SeasonalEvent> = combine(_seasonalChampionship, gameState) { event, state ->
        val claimed = state?.claimedCoreEvents?.split("|").orEmpty()
        event.copy(tasks = event.tasks.map { it.copy(isClaimed = it.taskId in claimed) }.toMutableList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _seasonalChampionship.value)

    // Global Trade Competition Board simulator (simulating live price actions from other factions)
    private val _globalTradeStandings = MutableStateFlow(listOf(
        LeaderboardEntry("Nova Core Inc (You)", 5000.0, true),
        LeaderboardEntry("Apex Industries", 12400.0, false),
        LeaderboardEntry("Sol Star Sagas", 8950.0, false),
        LeaderboardEntry("Hydra Conglomerate", 3200.0, false)
    ))
    val globalTradeStandings: StateFlow<List<LeaderboardEntry>> = _globalTradeStandings.asStateFlow()

    // Last combat replay info

    init {
        cloudSaveManager.beforeRestore = {
            closeBattle()
            _offlineEarnings.value = null
            _seasonalChampionship.update { event -> event.copy(tasks = event.tasks.map {
                it.copy(progress = 0.0, isClaimed = false)
            }.toMutableList()) }
            _guildMissions.update { list -> list.map { it.copy(progress = 0.0) } }
        }
        viewModelScope.launch {
            try {
                // Setup DB and local data seeding
                repository.checkAndInitialize()
            } catch (e: Exception) {
                android.util.Log.e("GameViewModel", "DB checkAndInitialize failed", e)
                _storageError.value = "Não foi possível abrir o progresso. Seus dados foram preservados; contate o suporte para recuperar o save."
                return@launch
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

        }
    }

    fun dismissOfflineEarnings() {
        _offlineEarnings.value = null
    }

    private suspend fun startResourceTickLoop() {
        while (true) {
            try {
                delay(1000)
                repository.runProductionTransaction {
                val activeState = repository.getGameStateDirect() ?: return@runProductionTransaction
                val tickTime = System.currentTimeMillis()
                if (tickTime <= activeState.lastSavedTime || tickTime - activeState.lastSavedTime < 1000) return@runProductionTransaction
                val bList = repository.getBuildingsDirect()
                if (bList.isEmpty()) return@runProductionTransaction

                val tick = ProductionEngine.step(activeState, bList, repository.getResourcesDirect(),
                    repository.getResearchersDirect(), marketPrices.value)
                val resMap = tick.resources.associate { it.name to it.quantity }
                val creditsGainedThisTick = tick.cash
                val comradeRate = tick.comrades
                for (building in bList) {
                    val produced = tick.produced[building.id.toString()] ?: 0.0
                    incrementSeasonalProgress(building.resourceProduced, produced)
                    incrementGuildProgress(building.resourceProduced, produced)
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
                    lastSavedTime = tickTime
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
                }
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
                task.copy(progress = min(task.target, nextProg))
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

    private suspend fun prepareBattle(stage: CombatStage): ActiveBattleState? {
        val ticket = repository.beginCombat(stage.id) ?: return null
        val state = repository.getGameStateDirect() ?: return null
        val units = repository.getCombatUnitsDirect()
        val multi = 1.0 + state.scienceCombatLevel * 0.15
        val hp = (units.sumOf { it.health } * multi).toInt()
        if (hp <= 0) { repository.completeCombat(ticket.id, false); return null }
        val previous = ticket.enemyLevel - 1
        val enemyHp = (stage.enemyHp * (1.0 + 0.15 * previous)).toInt()
        return ActiveBattleState(ticket, stage.id, hp, hp, (units.sumOf { it.attack } * multi).toInt(),
            enemyHp, enemyHp, (stage.enemyDmg * (1.0 + 0.12 * previous)).toInt(), 1, false)
    }

    fun startCombatBattle(stageId: Int) { combatStages.find { it.id == stageId }?.let { combat.start(it) } }
    fun triggerShieldSkill() { combat.shield() }
    fun triggerLaserStrikeSkill() { combat.laser() }
    fun triggerOverchargeHealSkill() { combat.heal() }
    fun closeBattle() { combat.close() }
    override fun onCleared() { combat.close(); super.onCleared() }

    private fun recordCombatVictory() {
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
                task.copy(progress = maxLevel.toDouble())
            } else task
        }.toMutableList()
        _seasonalChampionship.value = currentEvent.copy(tasks = list)
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
        viewModelScope.launch { repository.buyAutoSellLicense() }
    }

    fun buyAutoSellLicenseWithGems(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch { repository.buyAutoSellLicenseWithGems(onSuccess, onError) }
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
        viewModelScope.launch {
            val success = repository.performPrestige()
            if (success) {
                onSuccess()
            } else {
                onError("Erro ao realizar a Ascensão Estelar!")
            }
        }
    }

    fun produceManual(buildingId: Int) {
        viewModelScope.launch {
            repository.runProductionTransaction {
            val activeState = repository.getGameStateDirect() ?: return@runProductionTransaction
            val building = repository.getBuildingsDirect().find { it.id == buildingId }
                ?: return@runProductionTransaction
            if (building.level == 0 || building.isAutomated) return@runProductionTransaction
            val resList = repository.getResourcesDirect()
            val resMap = resList.associate { it.name to it.quantity }.toMutableMap()

            val tick = ProductionEngine.step(activeState, listOf(building.copy(isAutomated = true)),
                resList, repository.getResearchersDirect(), marketPrices.value)
            resMap.clear()
            resMap.putAll(tick.resources.associate { it.name to it.quantity })
            val actualProduced = tick.produced[building.id.toString()] ?: 0.0
            val cashGained = tick.cash
            incrementSeasonalProgress(building.resourceProduced, actualProduced)
            incrementGuildProgress(building.resourceProduced, actualProduced)

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
    }

    fun upgradeCombatUnit(id: Int) {
        viewModelScope.launch {
            repository.upgradeCombatUnit(id)
        }
    }

    fun sellResourceToMarket(name: String, amount: Double) {
        val price = marketPrices.value[name] ?: 1.0
        val current = gameState.value

        viewModelScope.launch {
            repository.sellResource(name, amount, price)
        }
    }

    // Profile skin changing & Avatars customization
    fun customizeProfile(avatarId: Int, skinId: Int, newName: String) {
        viewModelScope.launch {
            repository.updateProfile(avatarId, skinId, newName)
        }
    }

    // Subscription & monetization interactions
    fun buySubscription() {
        viewModelScope.launch { repository.buySubscription() }
    }

    fun buyPremiumPack(costCoins: Double, rewardGems: Long, packName: String) {
        viewModelScope.launch {
            repository.exchangeCashForGems(costCoins, rewardGems)
        }
    }

    // Force claim seasonal item
    fun claimEventReward(taskId: String) {
        viewModelScope.launch {
            val task = _seasonalChampionship.value.tasks.find { it.taskId == taskId } ?: return@launch
            repository.claimCoreEvent(taskId, task.progress >= task.target)
        }
    }

    fun buyStellarUpgrade(upgrade: StellarUpgrade, expectedLevel: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.buyStellarUpgrade(upgrade, expectedLevel)) }
    }

    // Join guild simulator
    fun joinGuild(guildName: String) {
        viewModelScope.launch { repository.joinGuild(guildName) }
    }

    fun completeTutorial() {
        viewModelScope.launch { repository.completeTutorial() }
    }

    fun resetTutorial() {
        viewModelScope.launch { repository.resetTutorial() }
    }

    fun logoutFromCloud() {
        viewModelScope.launch { cloudSaveManager.logout() }
    }

    fun uploadToCloud(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            cloudSaveManager.uploadBackup(repository, onSuccess, onError)
        }
    }

    fun downloadFromCloud(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            closeBattle()
            cloudSaveManager.downloadBackup(repository, {
                _offlineEarnings.value = null
                onSuccess()
            }, onError)
        }
    }

    // --- ADVENTURE COMMUNIST MECHANICS ---

    fun buyAndOpenCapsuleWithGems(type: String, onSuccess: (Long, List<String>) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch { repository.buyAndOpenCapsuleWithGems(type, onSuccess, onError) }
    }

    fun upgradeResearcherCard(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch { repository.upgradeResearcherCard(id, onSuccess, onError) }
    }

    fun claimMissionReward(missionId: Int, onSuccess: (Long, Long, String, List<String>) -> Unit) {
        viewModelScope.launch { repository.claimMissionReward(missionId, onSuccess) }
    }

    fun rankUpPlayer(onSuccess: (Int, Long, List<String>) -> Unit) {
        viewModelScope.launch { repository.rankUpPlayer(onSuccess) }
    }

    fun purchaseScienceUpgrade(upgradeType: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch { repository.purchaseScienceUpgrade(upgradeType, onSuccess, onError) }
    }


}

// Sealed status class for active fights
sealed class CombatStatus {
    object Idle : CombatStatus()
    object Running : CombatStatus()
    object Resolving : CombatStatus()
    object Failed : CombatStatus()
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
    val battle: CombatBattle,
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
    val timestamp: Long,
    val cancelled: Boolean = false
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
