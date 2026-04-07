package com.unciv.pure.application.pathfinding

import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class TroopMovementContext(private val troop: Troop) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) = !(tile as TileInfo).isImpassible()
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) =
            (tile as TileInfo).troopUnit != null &&
                    tile.troopUnit?.troop?.id != troop.id &&
                    tile != targetTile
}
