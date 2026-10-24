package com.unciv.battle.server

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import org.junit.Assert.*
import org.junit.Test

class BattleRoomRematchTest {
    private class Session : BattleSession {
        var finished = false
        var calls = 0
        private fun troop(id: Int, side: BattleSide, amount: Int) = BattleTroopSnapshot(
            id, side, 0, "Spearman", 3, amount, 10, 10, 5, 2, 0,
            0, 0, null, 0, 0, false, false, 0, 0)
        override fun read(seat: BattleSide) = BattleSessionSnapshot(
            calls.toLong(), calls.toLong(), null,
            BattleStateSnapshot(
                if (finished) listOf(troop(1, BattleSide.ATTACKER, 1)) else
                    listOf(troop(1, BattleSide.ATTACKER, 3), troop(2, BattleSide.DEFENDER, 3)),
                if (finished) emptyList() else listOf(1, 2), if (finished) 0 else 2,
                if (finished) null else 1, if (finished) null else BattleSide.ATTACKER,
                finished, if (finished) BattleSide.ATTACKER else null))
        override fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply {
            calls++
            finished = true
            return BattleSessionReply(request.sequence, calls.toLong())
        }
    }

    @Test
    fun bothVotesStartExactlyOneNewBattleAndOldCommandsCannotReachIt() {
        val sessions = mutableListOf<Session>()
        val rooms = BattleRooms({ Session().also { sessions.add(it) } })
        val a = rooms.create("scenario")
        val b = rooms.join(a.roomCode, "b".repeat(32))
        rooms.ready(a.roomCode, a.token)
        rooms.ready(a.roomCode, b.token)
        try {
            rooms.rematch(a.roomCode, a.token, 1)
            fail("An active battle cannot be restarted")
        } catch (ex: RoomException) { assertEquals(RoomFailure.NOT_READY, ex.reason) }
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        rooms.submit(a.roomCode, a.token, request)
        val result = rooms.read(a.roomCode, a.token).report!!
        assertEquals(2, result.attackerLosses.single().amount)
        assertEquals(3, result.defenderLosses.single().amount)
        assertEquals(result, rooms.read(a.roomCode, b.token).report)
        repeat(2) {
            val waiting = rooms.rematch(a.roomCode, a.token, 1)
            assertEquals(1, waiting.battleNumber)
            assertTrue(waiting.attackerRematch)
            assertFalse(waiting.defenderRematch)
        }
        val next = rooms.rematch(a.roomCode, b.token, 1)
        assertEquals(2, next.battleNumber)
        assertFalse(next.battle.state.finished)
        assertNull(next.report)
        repeat(2) {
            assertEquals(2, rooms.rematch(a.roomCode, a.token, 1).battleNumber)
            assertEquals(2, rooms.rematch(a.roomCode, b.token, 1).battleNumber)
        }
        assertEquals(2, sessions.size)
        try {
            rooms.submit(a.roomCode, a.token, request, 1)
            fail("A delayed command must not reach the new session")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, sessions[1].calls)
        rooms.submit(a.roomCode, a.token, request, 2)
        assertEquals(1, sessions[1].calls)
        assertFalse(rooms.read(a.roomCode, a.token).attackerRematch)
        assertFalse(rooms.read(a.roomCode, b.token).defenderRematch)
    }
}
