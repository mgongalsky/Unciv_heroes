package com.unciv.infrastructure.battle.network

import com.badlogic.gdx.utils.JsonValue
import com.unciv.battle.protocol.*
import com.unciv.infrastructure.battle.network.BattleReplyJson.int
import com.unciv.infrastructure.battle.network.BattleReplyJson.optional
import com.unciv.infrastructure.battle.network.BattleReplyJson.required
import com.unciv.infrastructure.battle.network.BattleReplyJson.text
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.Point

internal object BattleAvailableCommandJson {
    fun decode(value: JsonValue): BattleCommand {
        require(value.isObject)
        fun point(name: String) = value.optional(name)?.let { WirePoint(it.int("x"), it.int("y")) }
        val action = WireAction.valueOf(value.required("action").text())
        val troopId = value.int("troopId")
        val target = point("target")
        val approach = point("attackFrom")
        // Reuse request shape validation without assigning an actual client request number.
        WireBattleCommand(BattleProtocol.VERSION, 1, 0, action, troopId, target, approach).validate()
        fun WirePoint.domain() = Point(x, y)
        return when (action) {
            WireAction.SKIP -> BattleCommand.Skip(troopId)
            WireAction.MOVE -> BattleCommand.Move(troopId, requireNotNull(target).domain())
            WireAction.SHOOT -> BattleCommand.Shoot(troopId, requireNotNull(target).domain())
            WireAction.ATTACK -> BattleCommand.Attack(troopId, requireNotNull(target).domain(), requireNotNull(approach).domain())
        }
    }
}
