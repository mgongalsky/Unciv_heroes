package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.Point

object BattleScreenCommandMapper {
    data class Input(
        val troopId: Int,
        val target: Point,
        val canShoot: Boolean,
        val targetIsEnemy: Boolean,
        val targetIsReachable: Boolean,
        val attackFrom: Point?
    )

    fun map(input: Input): BattleCommand? {
        if (input.canShoot && input.targetIsEnemy) {
            return BattleCommand.Shoot(input.troopId, input.target)
        }
        if (input.targetIsEnemy) {
            return BattleCommand.Attack(input.troopId, input.target, input.attackFrom)
        }
        if (!input.targetIsReachable) return null
        return BattleCommand.Move(input.troopId, input.target)
    }
}
