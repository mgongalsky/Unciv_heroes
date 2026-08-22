package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.Point

sealed class BattleCommand {
    abstract val troopId: Int

    data class Move(
        override val troopId: Int,
        val target: Point
    ) : BattleCommand()

    data class Attack(
        override val troopId: Int,
        val target: Point,
        val attackFrom: Point?
    ) : BattleCommand()

    data class Shoot(
        override val troopId: Int,
        val target: Point
    ) : BattleCommand()

    data class Skip(
        override val troopId: Int
    ) : BattleCommand()
}
