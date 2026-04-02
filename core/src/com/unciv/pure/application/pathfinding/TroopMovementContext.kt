package com.unciv.pure.application.pathfinding

import com.unciv.logic.army.TroopInfo
import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext

class TroopMovementContext(private val unit: TroopInfo) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) = !(tile as TileInfo).isImpassible()
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) =
            (tile as TileInfo).troopUnit != null && tile.troopUnit != unit && tile != targetTile
}
