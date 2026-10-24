package com.unciv.battle.server

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.domain.battle.BattleSide
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BattleScenarioFieldTest {
    @Test
    fun bothPlayersReceiveTheSameFieldBeforeAndAfterRepeatedCommand() {
        val catalog = BattleScenarioCatalog(File(requireNotNull(System.getProperty("battle.assets"))))
        val session = catalog.create("spearmen-vs-archers", 42L)
        val before = session.read(BattleSide.ATTACKER)
        val field = requireNotNull(before.state.field)
        assertEquals(14, field.width)
        assertEquals(8, field.height)
        assertEquals(112, field.cells.size)
        assertEquals(field, session.read(BattleSide.DEFENDER).state.field)
        before.state.troops.forEach { troop ->
            assertTrue(field.cells.any { it.position == troop.position })
        }
        val seat = requireNotNull(before.state.currentSide)
        val request = BattleSessionRequest(
            1, before.revision, BattleCommand.Skip(requireNotNull(before.state.currentTroopId))
        )
        val reply = session.submit(seat, request)
        assertTrue(requireNotNull(reply.result).success)
        assertEquals(reply, session.submit(seat, request))
        val after = session.read(seat)
        assertEquals(1L, after.revision)
        assertEquals(field, after.state.field)
        assertEquals(after.state, session.read(
            if (seat == BattleSide.ATTACKER) BattleSide.DEFENDER else BattleSide.ATTACKER
        ).state)
        assertNotSame(field.cells, requireNotNull(after.state.field).cells)
        assertEquals(0L, before.revision)
    }
}
