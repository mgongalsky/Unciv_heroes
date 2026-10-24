package com.unciv.battle.server

import com.unciv.battle.protocol.*
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.*
import org.junit.Test

class BattleSnapshotWireMapperTest {
    private fun snapshot() = BattleSessionSnapshot(
        12, 7, BattleSessionReply(7, 12, BattleSessionRejection.STALE_REVISION),
        BattleStateSnapshot(
            listOf(BattleTroopSnapshot(
                4, BattleSide.DEFENDER, 2, "Archer", 40, 31, 60, 100, 5, 8, 10,
                12, 30, Point(-2, 3), 70, 90, true, true, 15, 2
            )), listOf(4), 1, 4, BattleSide.DEFENDER, false, null
        )
    )

    @Test
    fun preservesEveryTroopFieldAndRecoveryReceipt() {
        val encoded = BattleSnapshotWireMapper.encode(snapshot())
        assertEquals(12L, encoded.revision)
        assertEquals(7L, encoded.lastSequence)
        assertEquals(WireSessionRejection.STALE_REVISION, encoded.lastReply!!.rejection)
        assertEquals(
            WireTroopSnapshot(4, WireSide.DEFENDER, 2, "Archer", 40, 31, 60, 100, 5, 8, 10,
                12, 30, WirePoint(-2, 3), 70, 90, true, true, 15, 2),
            encoded.state.troops.single()
        )
        assertEquals(listOf(4), encoded.state.turnQueue)
        assertEquals(1, encoded.state.remainingTurnsInRound)
        assertEquals(4, encoded.state.currentTroopId)
        assertEquals(WireSide.DEFENDER, encoded.state.currentSide)
        assertFalse(encoded.state.finished)
        assertNull(encoded.state.winner)
    }

    @Test
    fun distinguishesWaitingActiveAndFinishedRooms() {
        val waiting = RoomView("scenario", BattleSide.ATTACKER, false, true, false, snapshot())
        val encoded = BattleSnapshotWireMapper.encode(waiting)
        assertEquals(BattleProtocol.VERSION, encoded.protocolVersion)
        assertEquals(WireRoomPhase.WAITING, encoded.phase)
        assertEquals(WireSide.ATTACKER, encoded.side)
        assertFalse(encoded.opponentJoined)
        val active = waiting.copy(opponentJoined = true, defenderReady = true)
        assertEquals(WireRoomPhase.ACTIVE, BattleSnapshotWireMapper.encode(active).phase)
        val finished = active.copy(battle = active.battle.copy(state = active.battle.state.copy(
            finished = true, winner = BattleSide.DEFENDER, currentSide = null,
            currentTroopId = null, turnQueue = emptyList(), remainingTurnsInRound = 0
        )))
        val final = BattleSnapshotWireMapper.encode(finished)
        assertEquals(WireRoomPhase.FINISHED, final.phase)
        assertEquals(WireSide.DEFENDER, final.battle.state.winner)
    }

    @Test
    fun preservesTerrainAppearanceAndAuthoritativePassability() {
        val field = BattleFieldDescription(
            2, 1, listOf(
                BattleFieldCell(Point(-1, 0), "Grassland", listOf("Hill"), 0.25, 0.6, true),
                BattleFieldCell(Point(0, 0), "Plains", emptyList(), -0.125, 0.4, false)
            )
        )
        val original = snapshot()
        val encoded = BattleSnapshotWireMapper.encode(
            original.copy(
                state = original.state.copy(field = field)
            )
        )
        assertEquals(
            WireFieldDescription(
                2, 1, listOf(
                    WireFieldCell(WirePoint(-1, 0), "Grassland", listOf("Hill"), 0.25, 0.6, true),
                    WireFieldCell(WirePoint(0, 0), "Plains", emptyList(), -0.125, 0.4, false)
                )
            ), encoded.state.field
        )
        assertNull(BattleSnapshotWireMapper.encode(original).state.field)
    }
}
