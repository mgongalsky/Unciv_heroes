package com.unciv.pure.application.pathfinding

import com.unciv.pure.domain.battle.ZoneOfControlTransition
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile

/** Tactical movement facts, separate from world-map movement semantics. */
interface BattlePathfindingContext : IMovementContext {
    val normalControlMultiplier: Float get() = 2f
    fun controlStrength(tile: INavigableTile): ZoneOfControlTransition.Strength
}
