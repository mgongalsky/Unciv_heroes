package com.unciv.pure.application.pathfinding

import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile

class BattleMovementContext : IMovementContext {
    override fun canPassThrough(tile: INavigableTile): Boolean =
            (tile as? IBattleTile)?.getTroop() == null

    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f

    override fun hasExplored(tile: INavigableTile) = true

    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false
}
