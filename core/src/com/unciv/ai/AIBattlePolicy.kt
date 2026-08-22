package com.unciv.ai

import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.domain.troop.Troop

class AIBattlePolicy(
    private val battleManager: BattleManager
) : BattlePolicy {
    override fun chooseCommand(troopId: Int): BattleCommand? {
        val troop = battleManager.getTroopById(troopId) ?: return null
        return if (troop.isRanged) chooseRangedCommand(troop)
        else chooseMeleeCommand(troop)
    }

    private fun chooseMeleeCommand(troop: Troop): BattleCommand? {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return null
        if (enemies.isEmpty()) return null

        val closestEnemy = enemies.minByOrNull {
            battleManager.getTroopTile(it)?.let { tile ->
                HexMath.getDistance(currentTile.position, tile.position)
            } ?: Int.MAX_VALUE
        } ?: return null
        val closestEnemyTile = battleManager.getTroopTile(closestEnemy) ?: return null

        val attackTile = findAttackTile(troop, closestEnemyTile)
        if (attackTile != null) {
            return BattleCommand.Attack(
                troopId = troop.id,
                target = closestEnemyTile.toPoint(),
                attackFrom = attackTile.toPoint()
            )
        }

        val moveTarget = findBestMoveTarget(troop, closestEnemyTile) ?: return null
        return BattleCommand.Move(troop.id, moveTarget.toPoint())
    }

    private fun chooseRangedCommand(troop: Troop): BattleCommand? {
        if (battleManager.getTroopTile(troop) == null) return null
        val target = battleManager.getEnemies(troop)
            .sortedWith(compareByDescending<Troop> { it.isRanged }.thenByDescending { it.speed })
            .firstOrNull() ?: return null
        val targetTile = battleManager.getTroopTile(target) ?: return null
        return BattleCommand.Shoot(troop.id, targetTile.toPoint())
    }

    private fun findAttackTile(troop: Troop, targetTile: IBattleTile): IBattleTile? {
        val currentTile = battleManager.getTroopTile(troop) ?: return null
        if (targetTile.neighbors.contains(currentTile)) return currentTile

        return targetTile.neighbors
            .filterIsInstance<IBattleTile>()
            .firstOrNull {
                battleManager.isTileAchievable(troop, it) && battleManager.isTileFree(it)
            }
    }

    private fun findBestMoveTarget(troop: Troop, targetTile: IBattleTile): IBattleTile? =
        battleManager.getReachableTiles(troop)
            .minByOrNull { HexMath.getDistance(it.position, targetTile.position) }
                as? IBattleTile
}
