package com.unciv.pure.application.pathfinding

import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext

class TroopMovementContext(
    private val unit: TroopInfo
) : IMovementContext {

    override fun canPassThrough(tile: TileInfo): Boolean =
            !tile.isImpassible()

    override fun getMovementCost(from: TileInfo, to: TileInfo): Float = 1f

    override fun hasExplored(tile: TileInfo): Boolean = true

    override fun shouldSkipTile(tile: TileInfo, targetTile: TileInfo?): Boolean =
            tile.troopUnit != null && tile.troopUnit != unit && tile != targetTile
}
