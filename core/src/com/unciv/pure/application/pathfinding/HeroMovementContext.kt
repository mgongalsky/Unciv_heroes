package com.unciv.pure.application.pathfinding

import com.unciv.logic.MovableUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext

class HeroMovementContext(private val unit: MovableUnit) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) =
            unit.movement.canPassThrough(tile as TileInfo)
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) =
            unit.movement.getMovementCostBetweenAdjacentTiles(from as TileInfo, to as TileInfo, unit.civInfo)
    override fun hasExplored(tile: INavigableTile) =
            unit.civInfo.hasExplored(tile as TileInfo)
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false
}
