package com.example

import com.example.data.*
import com.example.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CombatEngineTest {
    private val stage = CombatStage(1,"Test",1000,100,"Loot")
    private fun battle(hp: Int=500, maxHp: Int=500, enemyHp: Int=1000, damage: Int=10, enemyDamage: Int=100) =
        ActiveBattleState(CombatBattle("battle",1,1,500.0,1,5),1,hp,maxHp,damage,enemyHp,enemyHp,enemyDamage,1,false)
    private fun TestScope.createEngine(initial: ActiveBattleState = battle(),
        prepare: suspend (CombatStage)->ActiveBattleState? = { initial },
        settle: suspend (String,Boolean)->CombatReward? = { _,win -> if(win)initial.battle.reward() else null },
        onVictory: ()->Unit = {}) = CombatEngine(backgroundScope,prepare,settle,onVictory,{ _,_->1.0 },{testScheduler.currentTime})

    @Test fun shieldChangesRealDamageAndExpiresAfterOneRetaliation() = runTest {
        val engine=createEngine();engine.start(stage);runCurrent()
        assertTrue(engine.shield());assertFalse(engine.shield())
        advanceTimeBy(1500);runCurrent()
        assertEquals(480,engine.live.value!!.allyHp)
        assertFalse(engine.live.value!!.isShieldActive)
        advanceTimeBy(1500);runCurrent()
        assertEquals(380,engine.live.value!!.allyHp)
    }

    @Test fun laserDamageSurvivesTheNextRound() = runTest {
        val engine=createEngine();engine.start(stage);runCurrent()
        assertTrue(engine.laser());assertFalse(engine.laser())
        assertEquals(980,engine.live.value!!.enemyHp)
        advanceTimeBy(1500);runCurrent()
        assertEquals(970,engine.live.value!!.enemyHp)
    }

    @Test fun healPersistsAndCannotExceedMaximumHealth() = runTest {
        val engine=createEngine(battle(hp=300));engine.start(stage);runCurrent()
        advanceTimeBy(1500);runCurrent();assertEquals(200,engine.live.value!!.allyHp)
        assertTrue(engine.heal());assertFalse(engine.heal())
        assertEquals(400,engine.live.value!!.allyHp)
        advanceTimeBy(1500);runCurrent();assertEquals(300,engine.live.value!!.allyHp)
        val full=createEngine(battle(hp=450));full.start(stage);runCurrent()
        assertTrue(full.heal());assertEquals(500,full.live.value!!.allyHp)
    }

    @Test fun laserAndRoundCannotResolveOrPayTwice() = runTest {
        val release=CompletableDeferred<Unit>();var payments=0;var victories=0
        val engine=createEngine(battle(enemyHp=20), settle={_,win->
            if(win){release.await();payments++;CombatReward(500.0,1,5)} else null
        },onVictory={victories++})
        engine.start(stage);runCurrent();advanceTimeBy(1499)
        assertTrue(engine.laser());runCurrent()
        assertEquals(CombatStatus.Resolving,engine.status.value)
        assertFalse(engine.shield());assertFalse(engine.heal());assertFalse(engine.laser())
        advanceTimeBy(2000);runCurrent();assertEquals(0,payments)
        release.complete(Unit);runCurrent()
        assertEquals(1,payments);assertEquals(1,victories)
        assertTrue(engine.replay.value!!.victory)
        assertEquals(engine.logs.value,engine.replay.value!!.logsSnapshot)
        advanceTimeBy(5000);runCurrent();assertEquals(1,payments)
        val loopFirst=createEngine(battle(enemyHp=10));loopFirst.start(stage);runCurrent()
        advanceTimeBy(1500);runCurrent()
        assertTrue(loopFirst.status.value is CombatStatus.Victory)
        assertFalse(loopFirst.laser())
    }

    @Test fun closingBeforeRoundCancelsDamageAndClosingDuringSettlementCancelsPayment() = runTest {
        var paid=0
        val engine=createEngine(settle={_,win->if(win){paid++;CombatReward(500.0,1,5)} else null})
        engine.start(stage);runCurrent();advanceTimeBy(1499);engine.close();runCurrent()
        val logs=engine.logs.value
        advanceTimeBy(10000);runCurrent()
        assertNull(engine.live.value);assertEquals(logs,engine.logs.value);assertEquals(0,paid)
        assertTrue(engine.replay.value!!.cancelled)
        assertFalse(engine.laser());assertFalse(engine.heal());assertFalse(engine.shield())
        val gate=CompletableDeferred<Unit>()
        val pending=createEngine(battle(enemyHp=10),settle={_,win->if(win){gate.await();paid++;CombatReward(500.0,1,5)}else null})
        pending.start(stage);runCurrent();pending.laser();runCurrent()
        pending.close();runCurrent();gate.complete(Unit);runCurrent()
        assertEquals(0,paid);assertEquals(CombatStatus.Idle,pending.status.value)
    }

    @Test fun doubleStartAndConcurrentSkillCallsAreSerialized() = runTest {
        val gate=CompletableDeferred<Unit>();var preparations=0
        val engine=createEngine(prepare={preparations++;gate.await();battle()})
        assertTrue(engine.start(stage));assertFalse(engine.start(stage));runCurrent()
        assertEquals(1,preparations);assertFalse(engine.laser())
        gate.complete(Unit);runCurrent()
        // Keep virtual rounds/cooldowns paused while real threads contend for the lock.
        // Suspending the test on awaitAll lets runTest advance to the battle's defeat.
        val accepted = runBlocking(Dispatchers.Default) {
            List(20) { async { engine.laser() } }.awaitAll()
        }
        assertEquals(1,accepted.count{it});assertEquals(980,engine.live.value!!.enemyHp)
        engine.close();runCurrent();assertTrue(engine.start(stage));runCurrent()
        assertEquals(2,preparations)
    }

    @Test fun cooldownsPersistBetweenBattlesAndCountDownWithoutOfflineCombat() = runTest {
        val engine=createEngine();engine.start(stage);runCurrent()
        engine.shield();engine.laser();engine.heal()
        assertEquals(10,engine.shieldCooldown.value);assertEquals(14,engine.beamCooldown.value);assertEquals(18,engine.healCooldown.value)
        engine.close();runCurrent();engine.start(stage);runCurrent()
        assertFalse(engine.shield());assertFalse(engine.laser());assertFalse(engine.heal())
        engine.close();runCurrent();advanceTimeBy(10000);runCurrent()
        assertEquals(0,engine.shieldCooldown.value);assertEquals(4,engine.beamCooldown.value);assertEquals(8,engine.healCooldown.value)
        advanceTimeBy(8000);runCurrent();assertEquals(0,engine.healCooldown.value)
        assertNull(engine.live.value);assertEquals(CombatStatus.Idle,engine.status.value)
    }

    @Test fun cancelDuringPreparationAndScopeDestructionCannotStartLateBattle() = runTest {
        val gate=CompletableDeferred<Unit>()
        val engine=createEngine(prepare={gate.await();battle()})
        engine.start(stage);runCurrent();engine.close();gate.complete(Unit);runCurrent()
        assertNull(engine.live.value);assertFalse(engine.active.value)
        val parent=CoroutineScope(SupervisorJob()+StandardTestDispatcher(testScheduler))
        var payments=0
        val destroyed=CombatEngine(parent,{battle(enemyHp=10)},{_,win->if(win){payments++;CombatReward(500.0,1,5)}else null})
        destroyed.start(stage);runCurrent();parent.cancel();advanceTimeBy(5000);runCurrent()
        assertEquals(0,payments)
    }

    @Test fun defeatPreservesFinalDamageAndReplayWithoutVictoryCallback() = runTest {
        var victories=0;var defeats=0
        val engine=createEngine(battle(hp=50),settle={_,win->if(!win)defeats++;null},onVictory={victories++})
        engine.start(stage);runCurrent();advanceTimeBy(1500);runCurrent()
        assertEquals(CombatStatus.Defeated,engine.status.value)
        assertEquals(0,engine.live.value!!.allyHp);assertEquals(990,engine.live.value!!.enemyHp)
        assertEquals(1,engine.replay.value!!.roundsCount)
        assertFalse(engine.replay.value!!.victory);assertEquals(0,victories);assertEquals(1,defeats)
        engine.close();runCurrent();assertFalse(engine.replay.value!!.cancelled)
    }
}
