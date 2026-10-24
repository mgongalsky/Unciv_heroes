package com.unciv.battle.protocol

import org.junit.Assert.*
import org.junit.Test

class BattleProtocolTest {
    private fun command(action: WireAction, target: WirePoint? = null, from: WirePoint? = null) =
        WireBattleCommand(BattleProtocol.VERSION, 1, 0, action, 1, target, from)

    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("Expected invalid wire input to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected boundary rejection.
        }
    }

    @Test
    fun acceptsAllFourCommandShapes() {
        command(WireAction.SKIP).validate()
        command(WireAction.MOVE, WirePoint(1, -1)).validate()
        command(WireAction.SHOOT, WirePoint(2, 0)).validate()
        command(WireAction.ATTACK, WirePoint(2, 0), WirePoint(1, 0)).validate()
    }

    @Test
    fun rejectsMissingAndExtraneousCoordinates() {
        rejects { command(WireAction.MOVE).validate() }
        rejects { command(WireAction.SHOOT).validate() }
        rejects { command(WireAction.ATTACK, WirePoint(1, 0)).validate() }
        rejects { command(WireAction.SKIP, WirePoint(0, 0)).validate() }
        rejects { command(WireAction.MOVE, WirePoint(1, 0), WirePoint(0, 0)).validate() }
    }

    @Test
    fun rejectsVersionSequenceAndIdentityErrors() {
        val skip = command(WireAction.SKIP)
        rejects { skip.copy(protocolVersion = 0).validate() }
        rejects { skip.copy(sequence = 0).validate() }
        rejects { skip.copy(sequence = Long.MAX_VALUE).validate() }
        rejects { skip.copy(expectedRevision = -1).validate() }
        rejects { skip.copy(troopId = 0).validate() }
    }

    @Test
    fun checksCoordinateBoundsBeforeFloatConversion() {
        command(WireAction.MOVE, WirePoint(-1024, 1024)).validate()
        rejects { command(WireAction.MOVE, WirePoint(1025, 0)).validate() }
        rejects { command(WireAction.MOVE, WirePoint(Int.MIN_VALUE, 0)).validate() }
    }

    @Test
    fun validatesRoomInputsAndDoesNotPrintPlayerToken() {
        CreateBattleRoomRequest(1, "spearmen-vs-archers").validate()
        rejects { CreateBattleRoomRequest(1, "../scenario").validate() }
        rejects { CreateBattleRoomRequest(2, "scenario").validate() }
        JoinBattleRoomRequest(1, "a".repeat(32)).validate()
        rejects { JoinBattleRoomRequest(1, "short").validate() }
        val ticket = BattleRoomTicket(1, "ABCDEF", "private-player-token", WireSide.ATTACKER)
        assertFalse(ticket.toString().contains(ticket.playerToken))
    }
}
