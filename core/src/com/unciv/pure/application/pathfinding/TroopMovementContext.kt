package com.unciv.pure.application.pathfinding

import com.unciv.logic.map.TileInfo
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.battle.ZoneOfControl

class TroopMovementContext(
    private val unit: Troop,
    private val isEnemy: (Troop) -> Boolean = { false }
) : BattlePathfindingContext {
    override fun canPassThrough(tile: INavigableTile) =
        !(tile as TileInfo).isImpassible() && (tile.troopUnit == null || tile.troopUnit?.id == unit.id)

    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) =
        (tile as TileInfo).isImpassible() ||
                (tile.troopUnit != null && tile.troopUnit?.id != unit.id && tile != targetTile)

    override fun controlStrength(tile: INavigableTile) =
        com.unciv.pure.domain.battle.ZoneOfControlTransition.strength(
            (tile as TileInfo).neighbors.mapNotNull { (it as TileInfo).troopUnit }
                .filter { it.currentAmount > 0 && isEnemy(it) }
                .distinctBy { it.id }.count()
        )

    override fun canLeaveTile(tile: INavigableTile) =
        controlStrength(tile) != com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength.REINFORCED
}
