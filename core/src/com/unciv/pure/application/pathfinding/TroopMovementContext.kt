package com.unciv.pure.application.pathfinding

import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.battle.ZoneOfControlTransition
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class TroopMovementContext(
    private val unit: Troop,
    private val isEnemy: (Troop) -> Boolean = { false }
) : BattlePathfindingContext {
    private fun battleTile(tile: INavigableTile): IBattleTile =
            requireNotNull(tile as? IBattleTile) { "Tactical movement requires an IBattleTile" }

    override fun canPassThrough(tile: INavigableTile): Boolean {
        val battleTile = battleTile(tile)
        return !battleTile.isImpassible() &&
                (battleTile.getTroop() == null || battleTile.getTroop()?.id == unit.id)
    }

    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true

    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?): Boolean {
        val battleTile = battleTile(tile)
        return battleTile.isImpassible() ||
                (battleTile.getTroop() != null && battleTile.getTroop()?.id != unit.id && tile != targetTile)
    }

    override fun controlStrength(tile: INavigableTile) = ZoneOfControlTransition.strength(
        tile.neighbors.mapNotNull { battleTile(it).getTroop() }
            .filter { it.currentAmount > 0 && isEnemy(it) }
            .distinctBy { it.id }.count()
    )

    override fun canLeaveTile(tile: INavigableTile) =
            controlStrength(tile) != ZoneOfControlTransition.Strength.REINFORCED
}
