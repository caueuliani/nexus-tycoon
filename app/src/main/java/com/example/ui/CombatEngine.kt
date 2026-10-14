package com.example.ui

import com.example.data.CombatReward
import com.example.audio.SoundEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** All transitions are serialized; only this engine writes the live battle state. */
internal class CombatEngine(
    private val scope: CoroutineScope,
    private val prepare: suspend (CombatStage) -> ActiveBattleState?,
    private val settle: suspend (String, Boolean) -> CombatReward?,
    private val onVictory: () -> Unit = {},
    private val random: (Double, Double) -> Double = { from, to -> Random.nextDouble(from, to) },
    private val now: () -> Long = System::currentTimeMillis,
    private val onSound: (SoundEvent) -> Unit = {}
) {
    private val lock = Any()
    private var generation = 0L
    private var session: CoroutineScope? = null
    private var stage: CombatStage? = null
    private val mutableLive = MutableStateFlow<ActiveBattleState?>(null)
    private val mutableActive = MutableStateFlow(false)
    private val mutableStatus = MutableStateFlow<CombatStatus>(CombatStatus.Idle)
    private val mutableLogs = MutableStateFlow<List<String>>(emptyList())
    private val mutableReplay = MutableStateFlow<CombatReplay?>(null)
    private val mutableSelected = MutableStateFlow(1)
    private val shield = MutableStateFlow(0)
    private val beam = MutableStateFlow(0)
    private val heal = MutableStateFlow(0)
    val live = mutableLive.asStateFlow()
    val active = mutableActive.asStateFlow()
    val status = mutableStatus.asStateFlow()
    val logs = mutableLogs.asStateFlow()
    val replay = mutableReplay.asStateFlow()
    val selected = mutableSelected.asStateFlow()
    val shieldCooldown = shield.asStateFlow()
    val beamCooldown = beam.asStateFlow()
    val healCooldown = heal.asStateFlow()

    init {
        // Existing one-second countdown spans battles and arena exits within this ViewModel.
        scope.launch {
            while (isActive) {
                delay(1000)
                synchronized(lock) {
                    shield.value = max(0, shield.value - 1)
                    beam.value = max(0, beam.value - 1)
                    heal.value = max(0, heal.value - 1)
                }
            }
        }
    }

    fun start(next: CombatStage): Boolean = synchronized(lock) {
        if (mutableActive.value) return false
        val token = ++generation
        stage = next
        mutableSelected.value = next.id
        mutableActive.value = true
        mutableStatus.value = CombatStatus.Running
        mutableLogs.value = emptyList()
        mutableLive.value = null
        val child = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
        session = child
        child.launch {
            try {
                val initial = prepare(next)
                synchronized(lock) {
                    if (token != generation) return@launch
                    if (initial == null) { close(); return@launch }
                    mutableLive.value = initial
                    log("INITIATING MISSION: ${next.name} (Amea?a ${initial.battle.enemyLevel})")
                    log("Squad HP: ${initial.allyHp}, Attack: ${initial.allyDmg}; Enemy HP: ${initial.enemyHp}, Attack: ${initial.enemyDmg}")
                }
                while (isActive) {
                    delay(1500)
                    synchronized(lock) {
                        if (token != generation || mutableStatus.value != CombatStatus.Running) return@launch
                        round()
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                synchronized(lock) {
                    if (token == generation) {
                        log("Falha no combate: ${e.message}")
                        mutableStatus.value = CombatStatus.Failed
                    }
                }
            }
        }
        true
    }

    private fun log(message: String) { mutableLogs.value = mutableLogs.value + message }
    private fun running(): ActiveBattleState? =
        if (mutableActive.value && mutableStatus.value == CombatStatus.Running) mutableLive.value else null

    private fun round() {
        val current = running() ?: return
        val damage = (current.allyDmg * random(0.85, 1.15)).toInt()
        val enemyHp = max(0, current.enemyHp - damage)
        log("? Ally squad attacks doing $damage DMG!")
        onSound(SoundEvent.ATTACK)
        if (enemyHp == 0) {
            mutableLive.value = current.copy(enemyHp = 0, round = current.round + 1, isShieldActive = false)
            finish(true)
            return
        }
        val baseDamage = (current.enemyDmg * random(0.9, 1.1)).toInt()
        val received = if (current.isShieldActive) (baseDamage * 0.2).toInt() else baseDamage
        if (current.isShieldActive) log("Shield absorbed 80% damage.")
        val allyHp = max(0, current.allyHp - received)
        log("Raiders retaliate! Inflicting $received DMG to squad!")
        mutableLive.value = current.copy(allyHp = allyHp, enemyHp = enemyHp,
            round = current.round + 1, isShieldActive = false)
        if (allyHp == 0) finish(false)
    }

    fun shield(): Boolean = synchronized(lock) {
        val current = running() ?: return false
        if (shield.value > 0) return false
        shield.value = 10
        mutableLive.value = current.copy(isShieldActive = true)
        log("Tactical Action: DEFENSIVE ENERGY SHIELD DEPLOYED!")
        onSound(SoundEvent.SKILL)
        true
    }
    fun laser(): Boolean = synchronized(lock) {
        val current = running() ?: return false
        if (beam.value > 0) return false
        beam.value = 14
        val damage = current.allyDmg * 2
        mutableLive.value = current.copy(enemyHp = max(0, current.enemyHp - damage))
        log("Tactical Action: ORBITAL ION LASER STRIKE! Deals $damage DMG!")
        onSound(SoundEvent.SKILL)
        if (mutableLive.value!!.enemyHp == 0) finish(true)
        true
    }
    fun heal(): Boolean = synchronized(lock) {
        val current = running() ?: return false
        if (heal.value > 0) return false
        heal.value = 18
        val amount = (current.allyMaxHp * 0.4).toInt()
        val hp = min(current.allyMaxHp, current.allyHp + amount)
        mutableLive.value = current.copy(allyHp = hp)
        log("Tactical Action: NANITE AUTO-REPAIR SYSTEM. Restored ${hp - current.allyHp} HP!")
        onSound(SoundEvent.SKILL)
        true
    }

    private fun finish(victory: Boolean) {
        val final = running() ?: return
        val token = generation
        mutableStatus.value = CombatStatus.Resolving // blocks both skills and a second terminal transition
        log(if (victory) "Raider vanguard neutralized!" else "Tactical squad retreated! Units damaged.")
        session!!.launch {
            try {
                val reward = settle(final.battle.id, victory)
                synchronized(lock) {
                    if (token != generation) return@launch
                    if (victory && reward == null) {
                        log("Pagamento n?o confirmado; esta batalha j? foi encerrada ou invalidada.")
                        mutableStatus.value = CombatStatus.Failed
                        return@launch
                    }
                    mutableStatus.value = if (reward != null && victory)
                        CombatStatus.Victory(reward.credits, reward.diamonds, reward.guildTokens) else CombatStatus.Defeated
                    onSound(if (victory) SoundEvent.VICTORY else SoundEvent.DEFEAT)
                    saveReplay(final, victory, false)
                    if (victory) onVictory()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                synchronized(lock) {
                    if (token == generation) {
                        log("Falha ao confirmar resultado: ${e.message}")
                        mutableStatus.value = CombatStatus.Failed
                    }
                }
            }
        }
    }

    private fun saveReplay(final: ActiveBattleState, victory: Boolean, cancelled: Boolean) {
        mutableReplay.value = CombatReplay(stage?.name ?: "Unknown Stage", final.round - 1,
            victory, mutableLogs.value.toList(), now(), cancelled)
    }

    fun close() = synchronized(lock) {
        val previous = mutableLive.value
        val unfinished = mutableStatus.value == CombatStatus.Running || mutableStatus.value == CombatStatus.Resolving
        val needsCleanup = unfinished || mutableStatus.value == CombatStatus.Failed
        ++generation
        session?.cancel()
        session = null
        if (previous != null && unfinished) {
            log("Combate cancelado: arena fechada ou aplicativo oculto.")
            saveReplay(previous, false, true)
        }
        mutableLive.value = null
        mutableStatus.value = CombatStatus.Idle
        mutableActive.value = false
        // Cleanup cannot award anything; it is independent from the cancelled battle work.
        if (previous != null && needsCleanup) scope.launch {
            try { settle(previous.battle.id, false) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* A new battle also invalidates any pending receipt. */ }
        }
    }
}
