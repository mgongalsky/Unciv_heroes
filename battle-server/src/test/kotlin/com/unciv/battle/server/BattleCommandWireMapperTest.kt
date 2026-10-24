package com.unciv.battle.server

import com.unciv.battle.protocol.WireAction
import com.unciv.battle.protocol.WireBattleCommand
import com.unciv.battle.protocol.WirePoint
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.*
import org.junit.Test

class BattleCommandWireMapperTest {
    @Test
    fun preservesCoordinatesIdentityAndOrderingForEveryAction() {
        val target = WirePoint(-2, 3)
        val approach = WirePoint(-1, 3)
        val examples = listOf(
            WireBattleCommand(1, 7, 12, WireAction.MOVE, 4, target) to BattleCommand.Move(4, Point(-2, 3)),
            WireBattleCommand(1, 7, 12, WireAction.SHOOT, 4, target) to BattleCommand.Shoot(4, Point(-2, 3)),
            WireBattleCommand(1, 7, 12, WireAction.ATTACK, 4, target, approach) to
                BattleCommand.Attack(4, Point(-2, 3), Point(-1, 3)),
            WireBattleCommand(1, 7, 12, WireAction.SKIP, 4) to BattleCommand.Skip(4)
        )
        for ((wire, expected) in examples) {
            val request = BattleCommandWireMapper.decode(wire)
            assertEquals(expected, request.command)
            assertEquals(7L, request.sequence)
            assertEquals(12L, request.expectedRevision)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedVersionBeforeCreatingApplicationRequest() {
        BattleCommandWireMapper.decode(WireBattleCommand(2, 1, 0, WireAction.SKIP, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsIncompleteMeleeCommandBeforeCreatingApplicationRequest() {
        BattleCommandWireMapper.decode(WireBattleCommand(1, 1, 0, WireAction.ATTACK, 1, WirePoint(1, 0)))
    }
}
