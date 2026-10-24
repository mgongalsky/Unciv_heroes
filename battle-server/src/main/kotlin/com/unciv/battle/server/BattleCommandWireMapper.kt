package com.unciv.battle.server

import com.unciv.battle.protocol.WireAction
import com.unciv.battle.protocol.WireBattleCommand
import com.unciv.battle.protocol.WirePoint
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.domain.battle.Point

/** Validates wire shape before crossing the application boundary. Authentication is separate. */
object BattleCommandWireMapper {
    fun decode(wire: WireBattleCommand): BattleSessionRequest {
        wire.validate()
        fun WirePoint.point() = Point(x, y)
        val command = when (wire.action) {
            WireAction.MOVE -> BattleCommand.Move(wire.troopId, requireNotNull(wire.target).point())
            WireAction.ATTACK -> BattleCommand.Attack(
                wire.troopId, requireNotNull(wire.target).point(), requireNotNull(wire.attackFrom).point()
            )
            WireAction.SHOOT -> BattleCommand.Shoot(wire.troopId, requireNotNull(wire.target).point())
            WireAction.SKIP -> BattleCommand.Skip(wire.troopId)
        }
        return BattleSessionRequest(wire.sequence, wire.expectedRevision, command)
    }
}
