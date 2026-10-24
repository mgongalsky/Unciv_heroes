package com.unciv.battle.server

import com.unciv.pure.application.battle.recruitment.*
import com.unciv.pure.application.battle.session.*
import com.unciv.pure.domain.battle.BattleSide
import org.junit.Assert.*
import org.junit.Test

class BattleRecruitmentRoomTest {
    private class Session : BattleSession {
        var finished = false
        override fun read(seat: BattleSide) = BattleSessionSnapshot(
            0L, 0L, null,
            BattleStateSnapshot(emptyList(), emptyList(), 0, null, null, finished,
                if (finished) BattleSide.ATTACKER else null)
        )
        override fun submit(seat: BattleSide, request: BattleSessionRequest): BattleSessionReply =
            error("No combat commands expected in this setup test")
    }

    private fun invalid(action: () -> Unit) {
        try { action(); fail("Expected invalid army or battle number") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun createsOneBattleAfterBothPlayersConfirmAndReturnsToRecruitment() {
        val selection = BattleArmySelection(listOf(
            BattleRecruitmentUnit("Peasant", 1), BattleRecruitmentUnit("Archer", 3)
        ))
        val created = mutableListOf<Session>()
        val received = mutableListOf<Pair<List<BattleArmyStack>, List<BattleArmyStack>>>()
        val rooms = BattleRooms(
            createSession = { error("Fixed scenario factory must not run") },
            recruitment = selection,
            createPurchasedSession = { a, d ->
                received += a to d
                Session().also { created += it }
            }
        )
        val a = rooms.create(BattleRooms.RECRUITMENT_SCENARIO)
        val d = rooms.join(a.roomCode, "x".repeat(32))
        val armyA = listOf(BattleArmyStack("Peasant", 300))
        val armyD = listOf(BattleArmyStack("Archer", 100))
        assertTrue(created.isEmpty())
        invalid { rooms.ready(a.roomCode, a.token) }
        invalid { rooms.ready(a.roomCode, a.token, stacks = listOf(BattleArmyStack("Peasant", 301))) }
        assertFalse(rooms.read(a.roomCode, a.token).attackerReady)
        val readyA = rooms.ready(a.roomCode, a.token, stacks = armyA)
        assertTrue(readyA.attackerReady)
        assertFalse(readyA.defenderReady)
        assertEquals(armyA, readyA.ownArmy)
        assertTrue(rooms.read(d.roomCode, d.token).ownArmy.isEmpty())
        rooms.ready(a.roomCode, a.token, stacks = armyA)
        assertTrue(created.isEmpty())
        invalid { rooms.ready(a.roomCode, a.token, stacks = armyD) }
        rooms.ready(d.roomCode, d.token, stacks = armyD)
        rooms.ready(d.roomCode, d.token, stacks = armyD)
        assertEquals(1, created.size)
        assertEquals(armyA to armyD, received.single())
        created.single().finished = true
        rooms.rematch(a.roomCode, a.token, 1)
        val next = rooms.rematch(d.roomCode, d.token, 1)
        assertEquals(2, next.battleNumber)
        assertFalse(next.attackerReady)
        assertFalse(next.defenderReady)
        assertFalse(next.battle.state.finished)
        assertNull(next.report)
        assertEquals(armyD, next.ownArmy)
        assertEquals(1, created.size)
        assertEquals(2, rooms.rematch(d.roomCode, d.token, 1).battleNumber)
        invalid { rooms.ready(a.roomCode, a.token, 1, armyA) }
        rooms.ready(a.roomCode, a.token, 2, armyD)
        rooms.ready(d.roomCode, d.token, 2, armyA)
        assertEquals(2, created.size)
        assertEquals(armyD to armyA, received.last())
    }
}
