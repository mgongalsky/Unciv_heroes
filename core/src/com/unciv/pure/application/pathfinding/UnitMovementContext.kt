package com.unciv.pure.application.pathfinding

import com.unciv.logic.MovableUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext

class UnitMovementContext(
    private val unit: MovableUnit
) : IMovementContext {
    override fun canPassThrough(tile: TileInfo) =
            unit.movement.canPassThrough(tile)
    override fun getMovementCost(from: TileInfo, to: TileInfo) =
            unit.movement.getMovementCostBetweenAdjacentTiles(from, to, unit.civInfo)
}
