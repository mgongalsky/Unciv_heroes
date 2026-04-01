package com.unciv.pure.application.pathfinding

import com.unciv.logic.MovableUnit
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext

// pure/application/pathfinding/HeroMovementContext.kt
class HeroMovementContext(
    private val unit: MovableUnit
) : IMovementContext {
    override fun canPassThrough(tile: TileInfo) =
            unit.movement.canPassThrough(tile)
    override fun getMovementCost(from: TileInfo, to: TileInfo) =
            unit.movement.getMovementCostBetweenAdjacentTiles(from, to, unit.civInfo)
    override fun hasExplored(tile: TileInfo) =
            unit.civInfo.hasExplored(tile)
    override fun shouldSkipTile(tile: TileInfo, targetTile: TileInfo?) = false
}
