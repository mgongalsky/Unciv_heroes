package com.unciv.pure.application.pathfinding

import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.battle.ZoneOfControl
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class BattleMovementContext(
    private val isEnemy: (Troop) -> Boolean = { false }
) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile): Boolean =
        (tile as? IBattleTile)?.getTroop() == null

    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f

    override fun hasExplored(tile: INavigableTile) = true

    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false

    override fun canLeaveTile(tile: INavigableTile) =
        ZoneOfControl.canLeaveTile(tile.neighbors.map { (it as? IBattleTile)?.getTroop() }, isEnemy)
}
