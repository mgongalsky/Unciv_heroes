package com.unciv.testing.pure.application.battle.session

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import org.junit.Assert.*
import org.junit.Test

class OrderedBattleSessionTest {
    private class Engine : BattleSessionEngine {
        override var isFinished = false
        override var currentSide: BattleSide? = BattleSide.ATTACKER
        override var currentTroopId: Int? = 1
        var calls = 0
        var result = BattleCommandResult(success = true)
        override fun execute(command: BattleCommand): BattleSessionExecution {
            calls++
            return BattleSessionExecution(result, emptyList())
        }
        override fun snapshot(): BattleStateSnapshot = BattleStateSnapshot(
            troops = emptyList(),
            turnQueue = if (isFinished) emptyList() else listOfNotNull(currentTroopId),
            remainingTurnsInRound = if (isFinished) 0 else 1,
            currentTroopId = if (isFinished) null else currentTroopId,
            currentSide = if (isFinished) null else currentSide,
            finished = isFinished,
            winner = null
        )
    }

    private fun request(sequence: Long = 1, revision: Long = 0, troopId: Int = 1) =
        BattleSessionRequest(sequence, revision, BattleCommand.Skip(troopId))

    @Test
    fun retryReturnsOriginalReceiptWithoutExecutingAgain() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        val command = request()
        val first = session.submit(BattleSide.ATTACKER, command)
        assertTrue(first.result!!.success)
        assertEquals(1L, first.revision)
        assertEquals(first, session.submit(BattleSide.ATTACKER, command))
        assertEquals(1, engine.calls)
    }

    @Test
    fun rejectedRuleIsNotExecutedTwiceAndDoesNotLoseItsRevision() {
        val engine = Engine().apply {
            result = BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
        }
        val session = OrderedBattleSession(engine)
        val first = session.submit(BattleSide.ATTACKER, request())
        assertFalse(first.result!!.success)
        assertEquals(1L, first.revision)
        assertEquals(first, session.submit(BattleSide.ATTACKER, request()))
        assertEquals(1, engine.calls)
    }

    @Test
    fun opponentCannotActEvenWithTheCorrectTroopId() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        val reply = session.submit(BattleSide.DEFENDER, request())
        assertEquals(BattleSessionRejection.NOT_YOUR_TURN, reply.rejection)
        assertEquals(0, engine.calls)
        assertEquals(0L, session.revision)
    }

    @Test
    fun staleRevisionAndWrongTroopNeverReachTheEngine() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        assertEquals(
            BattleSessionRejection.STALE_REVISION,
            session.submit(BattleSide.ATTACKER, request(revision = 10)).rejection
        )
        assertEquals(
            BattleSessionRejection.WRONG_TROOP,
            session.submit(BattleSide.ATTACKER, request(sequence = 2, troopId = 2)).rejection
        )
        assertEquals(0, engine.calls)
    }

    @Test
    fun reusingASequenceForDifferentContentIsRejected() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        session.submit(BattleSide.ATTACKER, request())
        val reply = session.submit(BattleSide.ATTACKER, request(troopId = 2))
        assertEquals(BattleSessionRejection.REQUEST_CONFLICT, reply.rejection)
        assertEquals(1, engine.calls)
    }

    @Test
    fun evictedReceiptCannotCauseAnOldActionToExecuteAgain() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        session.submit(BattleSide.ATTACKER, request())
        session.submit(BattleSide.ATTACKER, request(sequence = 2, revision = 1))
        assertEquals(
            BattleSessionRejection.OUT_OF_ORDER,
            session.submit(BattleSide.ATTACKER, request()).rejection
        )
        assertEquals(2, engine.calls)
    }

    @Test
    fun receiptRemainsStableAfterTheOtherPlayerActs() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        val first = session.submit(BattleSide.ATTACKER, request())
        engine.currentSide = BattleSide.DEFENDER
        engine.currentTroopId = 2
        session.submit(BattleSide.DEFENDER, request(revision = 1, troopId = 2))
        assertEquals(first, session.submit(BattleSide.ATTACKER, request()))
        assertEquals(2L, session.revision)
        assertEquals(2, engine.calls)
    }

    @Test
    fun invalidAndSkippedSequencesDoNotConsumeTheNextSequence() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        for (sequence in listOf(0L, -1L, Long.MAX_VALUE)) {
            assertEquals(
                BattleSessionRejection.INVALID_SEQUENCE,
                session.submit(BattleSide.ATTACKER, request(sequence)).rejection
            )
        }
        assertEquals(
            BattleSessionRejection.OUT_OF_ORDER,
            session.submit(BattleSide.ATTACKER, request(sequence = 2)).rejection
        )
        assertEquals(0L, session.lastSequence(BattleSide.ATTACKER))
        assertTrue(session.submit(BattleSide.ATTACKER, request()).result!!.success)
        assertEquals(1, engine.calls)
    }

    @Test
    fun finishedBattleRejectsNewActionsButStillAcknowledgesARetry() {
        val engine = Engine()
        val session = OrderedBattleSession(engine)
        val first = session.submit(BattleSide.ATTACKER, request())
        engine.isFinished = true
        assertEquals(first, session.submit(BattleSide.ATTACKER, request()))
        assertEquals(
            BattleSessionRejection.BATTLE_FINISHED,
            session.submit(BattleSide.ATTACKER, request(sequence = 2, revision = 1)).rejection
        )
        assertEquals(1, engine.calls)
    }
}
