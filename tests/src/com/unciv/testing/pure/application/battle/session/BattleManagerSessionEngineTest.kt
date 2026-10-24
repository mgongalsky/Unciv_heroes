package com.unciv.testing.pure.application.battle.session

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.session.BattleManagerSessionEngine
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.application.battle.session.OrderedBattleSession
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class BattleManagerSessionEngineTest {
    private class Fixture {
        var randomCalls = 0
        val attacker = Troop("Attacker", 10, 5, 100, 100, id = 1)
        val defender = Troop("Defender", 1, 4, 1, 1, id = 2)
        val field = TacticalBattleField(listOf(
            TacticalBattleField.Cell(Point(0, 0), false),
            TacticalBattleField.Cell(Point(1, 0), false),
            TacticalBattleField.Cell(Point(2, 0), false)
        ))
        val manager = BattleManager(
            Army(1).apply { addTroop(attacker) },
            Army(1).apply { addTroop(defender) },
            field,
            object : IBattleRandom {
                override fun nextDouble(): Double {
                    randomCalls++
                    return 0.5
                }
            },
            moraleProbability = 0.0, luckProbability = 0.0
        ).apply {
            val start = requireNotNull(field.getTileAt(Vector2(0f, 0f)))
            val target = requireNotNull(field.getTileAt(Vector2(1f, 0f)))
            start.receiveTroop(attacker)
            target.receiveTroop(defender)
            setTroopPosition(attacker, start)
            setTroopPosition(defender, target)
            initializeTurnQueue()
        }
        val engine = BattleManagerSessionEngine(manager)
        val session = OrderedBattleSession(engine)
    }

    @Test
    fun skipPassesControlToTheOtherHumanWithoutRunningAnAi() {
        val f = Fixture()
        assertEquals(BattleSide.ATTACKER, f.engine.currentSide)
        val reply = f.session.submit(
            BattleSide.ATTACKER, BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        )
        assertTrue(reply.result!!.success)
        assertEquals(listOf(BattleEvent.TurnSkipped), reply.events)
        assertEquals(BattleSide.DEFENDER, f.engine.currentSide)
        assertEquals(2, f.engine.currentTroopId)
        assertEquals(1, f.randomCalls)
    }

    @Test
    fun rejectedMoveDoesNotAdvanceTheTurnAndCanBeFollowedByAValidAction() {
        val f = Fixture()
        val rejected = f.session.submit(
            BattleSide.ATTACKER,
            BattleSessionRequest(1, 0, BattleCommand.Move(1, Point(99, 99)))
        )
        assertFalse(rejected.result!!.success)
        assertTrue(rejected.events.isEmpty())
        assertEquals(1, f.engine.currentTroopId)
        assertEquals(0, f.randomCalls)
        val accepted = f.session.submit(
            BattleSide.ATTACKER, BattleSessionRequest(2, rejected.revision, BattleCommand.Skip(1))
        )
        assertTrue(accepted.result!!.success)
        assertEquals(BattleSide.DEFENDER, f.engine.currentSide)
    }

    @Test
    fun retryDoesNotConsumeMoreRandomnessOrAdvanceAnotherTurn() {
        val f = Fixture()
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        val first = f.session.submit(BattleSide.ATTACKER, request)
        assertEquals(first, f.session.submit(BattleSide.ATTACKER, request))
        assertEquals(1, f.randomCalls)
        assertEquals(2, f.engine.currentTroopId)
    }

    @Test
    fun lethalAttackReturnsOrderedEventsAndPreservesCombatStateBeforeRecovery() {
        val f = Fixture()
        val reply = f.session.submit(
            BattleSide.ATTACKER,
            BattleSessionRequest(1, 0, BattleCommand.Attack(1, Point(1, 0), Point(0, 0)))
        )
        assertTrue(reply.result!!.success)
        assertTrue(reply.result!!.battleEnded)
        assertEquals(listOf("TroopAttacked", "BattleEnded"), reply.events.map { it.javaClass.simpleName })
        assertEquals(BattleEvent.BattleEnded(true), reply.events.last())
        assertTrue(f.engine.isFinished)
        assertNull(f.engine.currentTroopId)
        assertNull(f.engine.currentSide)
        assertEquals(0, f.defender.currentAmount)
        assertTrue("Retaliation damage must remain until explicit recovery", f.attacker.currentHealth < f.attacker.maxHealth)
    }

    @Test
    fun reconnectRecoversLostReceiptAndCurrentStateWithoutExecutingAgain() {
        val f = Fixture()
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        val reply = f.session.submit(BattleSide.ATTACKER, request)
        val recovered = f.session.read(BattleSide.ATTACKER)
        assertEquals(1L, recovered.revision)
        assertEquals(1L, recovered.lastSequence)
        assertEquals(reply, recovered.lastReply)
        assertEquals(BattleSide.DEFENDER, recovered.state.currentSide)
        assertEquals(2, recovered.state.currentTroopId)
        assertEquals(1, f.randomCalls)
        val opponent = f.session.read(BattleSide.DEFENDER)
        assertEquals(recovered.state, opponent.state)
        assertEquals(0L, opponent.lastSequence)
        assertNull(opponent.lastReply)
        assertEquals(reply, f.session.submit(BattleSide.ATTACKER, request))
        assertEquals(1, f.randomCalls)
    }

    @Test
    fun reconnectAfterRuleRejectionRecoversSequenceAndAllowsTheNextAction() {
        val f = Fixture()
        val rejected = f.session.submit(
            BattleSide.ATTACKER,
            BattleSessionRequest(1, 0, BattleCommand.Move(1, Point(99, 99)))
        )
        val recovered = f.session.read(BattleSide.ATTACKER)
        assertEquals(rejected, recovered.lastReply)
        assertFalse(recovered.lastReply!!.result!!.success)
        assertEquals(1, recovered.state.currentTroopId)
        val next = f.session.submit(
            BattleSide.ATTACKER,
            BattleSessionRequest(
                recovered.lastSequence + 1,
                recovered.revision,
                BattleCommand.Skip(1)
            )
        )
        assertTrue(next.result!!.success)
        assertEquals(2L, next.revision)
        assertEquals(1, recovered.state.currentTroopId)
        assertEquals(2, f.session.read(BattleSide.ATTACKER).state.currentTroopId)
    }

    @Test
    fun boundClientsShareTheBattleButCannotChooseEachOthersSeat() = kotlinx.coroutines.runBlocking {
        val f = Fixture()
        val host = com.unciv.logic.battle.session.LocalBattleHost(f.session)
        val attacker = host.client(BattleSide.ATTACKER)
        val defender = host.client(BattleSide.DEFENDER)
        val wrongSide = defender.submit(BattleSessionRequest(1, 0, BattleCommand.Skip(1)))
        assertEquals(
            com.unciv.pure.application.battle.session.BattleSessionRejection.NOT_YOUR_TURN,
            wrongSide.rejection
        )
        assertEquals(0, f.randomCalls)
        val first = attacker.submit(BattleSessionRequest(1, 0, BattleCommand.Skip(1)))
        assertTrue(first.result!!.success)
        val defenderState = defender.read()
        assertEquals(BattleSide.DEFENDER, defenderState.state.currentSide)
        val second = defender.submit(
            BattleSessionRequest(
                defenderState.lastSequence + 1, defenderState.revision, BattleCommand.Skip(2)
            )
        )
        assertTrue(second.result!!.success)
        assertEquals(attacker.read().state, defender.read().state)
        assertEquals(BattleSide.ATTACKER, attacker.read().state.currentSide)
    }

    @Test
    fun simultaneousRetriesThroughTheHostExecuteOnlyOnce() {
        val f = Fixture()
        val host = com.unciv.logic.battle.session.LocalBattleHost(f.session)
        val client = host.client(BattleSide.ATTACKER)
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        val ready = java.util.concurrent.CountDownLatch(2)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val calls = (1..2).map {
                executor.submit(java.util.concurrent.Callable {
                    ready.countDown()
                    check(start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    kotlinx.coroutines.runBlocking { client.submit(request) }
                })
            }
            assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS))
            start.countDown()
            val replies = calls.map { it.get(5, java.util.concurrent.TimeUnit.SECONDS) }
            assertEquals(replies[0], replies[1])
            assertTrue(replies[0].result!!.success)
            assertEquals(1, f.randomCalls)
            val state = kotlinx.coroutines.runBlocking { client.read() }
            assertEquals(1L, state.revision)
            assertEquals(2, state.state.currentTroopId)
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }
}
