package com.unciv.pure.application.pathfinding

import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.battle.ZoneOfControl
import com.unciv.pure.domain.pathfinding.IMovementContext
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class BattleMovementContext(
    private val isEnemy: (Troop) -> Boolean = { false }
) : BattlePathfindingContext {
    override fun canPassThrough(tile: INavigableTile): Boolean =
        (tile as? IBattleTile)?.getTroop() == null

    override fun getMovementCost(from: INavigableTile, to: INavigableTile) = 1f
    override fun hasExplored(tile: INavigableTile) = true
    override fun shouldSkipTile(tile: INavigableTile, targetTile: INavigableTile?) = false

    override fun controlStrength(tile: INavigableTile) =
        com.unciv.pure.domain.battle.ZoneOfControlTransition.strength(
            tile.neighbors.mapNotNull { (it as? IBattleTile)?.getTroop() }
                .filter { it.currentAmount > 0 && isEnemy(it) }
                .distinctBy { it.id }.count()
        )

    override fun canLeaveTile(tile: INavigableTile) =
        controlStrength(tile) != com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength.REINFORCED
}
