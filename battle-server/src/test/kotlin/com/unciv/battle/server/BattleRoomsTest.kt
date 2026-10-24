package com.unciv.battle.server

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import org.junit.Assert.*
import org.junit.Test

class BattleRoomsTest {
    private class Session : BattleSession {
        var calls = 0
        var submittedSide: BattleSide? = null
        override fun read(seat: BattleSide) = BattleSessionSnapshot(
            0, 0, null, BattleStateSnapshot(emptyList(), listOf(1), 1, 1, BattleSide.ATTACKER, false, null)
        )
        override fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply {
            calls++
            submittedSide = seat
            return BattleSessionReply(request.sequence, 1, result = BattleCommandResult(true))
        }
    }

    private fun fails(reason: RoomFailure, block: () -> Unit) {
        try {
            block()
            fail("Expected $reason")
        } catch (ex: RoomException) {
            assertEquals(reason, ex.reason)
        }
    }

    @Test
    fun onlyAuthenticatedReadyPlayersCanSubmit() {
        val session = Session()
        val rooms = BattleRooms({ session })
        val a = rooms.create("scenario")
        val request = BattleSessionRequest(1, 0, BattleCommand.Skip(1))
        fails(RoomFailure.UNAUTHORIZED) { rooms.submit(a.roomCode, a.roomCode, request) }
        fails(RoomFailure.NOT_READY) { rooms.submit(a.roomCode, a.token, request) }
        rooms.ready(a.roomCode, a.token)
        val b = rooms.join(a.roomCode, "b".repeat(32))
        fails(RoomFailure.NOT_READY) { rooms.submit(a.roomCode, a.token, request) }
        rooms.ready(b.roomCode, b.token)
        rooms.submit(b.roomCode, b.token, request)
        assertEquals(BattleSide.DEFENDER, session.submittedSide)
        assertEquals(1, session.calls)
    }

    @Test
    fun joinRetryRecoversTheSameSeatAndRejectsAThirdPlayer() {
        val rooms = BattleRooms({ Session() })
        val a = rooms.create("scenario")
        val nonce = "b".repeat(32)
        val b = rooms.join(a.roomCode, nonce)
        assertEquals(b.token, rooms.join(a.roomCode, nonce).token)
        assertNotEquals(a.token, b.token)
        fails(RoomFailure.FULL) { rooms.join(a.roomCode, "c".repeat(32)) }
        assertEquals(BattleSide.ATTACKER, rooms.read(a.roomCode, a.token).side)
        assertEquals(BattleSide.DEFENDER, rooms.read(b.roomCode, b.token).side)
        assertFalse(b.toString().contains(b.token))
    }

    @Test
    fun expiryBoundsMemoryAndUnauthorizedReadsDoNotKeepRoomsAlive() {
        var time = 0L
        val rooms = BattleRooms({ Session() }, maxRooms = 1, idleMillis = 100, now = { time })
        val a = rooms.create("scenario")
        fails(RoomFailure.CAPACITY) { rooms.create("scenario") }
        time = 99
        fails(RoomFailure.UNAUTHORIZED) { rooms.read(a.roomCode, "invalid") }
        time = 100
        fails(RoomFailure.NOT_FOUND) { rooms.read(a.roomCode, a.token) }
        val replacement = rooms.create("scenario")
        assertFalse(rooms.read(replacement.roomCode, replacement.token).opponentJoined)
    }
}
