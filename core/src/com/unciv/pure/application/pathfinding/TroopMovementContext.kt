package com.unciv.pure.application.pathfinding

import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.battle.ZoneOfControl

class TroopMovementContext(
    private val unit: Troop,
    private val isEnemy: (Troop) -> Boolean = { false }
) : IMovementContext {
    override fun canPassThrough(tile: INavigableTile) = !(tile as TileInfo).isImpassible()
    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) =
            (tile as TileInfo).troopUnit != null &&
                    tile.troopUnit?.id != unit.id &&
                    tile != targetTile
    override fun canLeaveTile(tile: INavigableTile) =
            ZoneOfControl.canLeaveTile(
                (tile as TileInfo).neighbors.map { (it as TileInfo).troopUnit },
                isEnemy
            )
}
