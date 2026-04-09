package com.unciv.ai

import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

class AIBattle(private val battleManager: BattleManager) {

    companion object {
        var AI_verbose = true
    }

    fun performTurn(troop: Troop) {
        if (AI_verbose) println("AI Turn: ${troop.unitName} at ${battleManager.getTroopTile(troop)?.position}")
        if (troop.isRanged) performRangedAction(troop)
        else performMeleeAction(troop)
    }

    private fun performMeleeAction(troop: Troop) {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return
        }

        val closestEnemy = enemies.minByOrNull {
            battleManager.getTroopTile(it)?.let { tile ->
                HexMath.getDistance(currentTile.position, tile.position)
            } ?: Int.MAX_VALUE
        } ?: return

        val closestEnemyTile = battleManager.getTroopTile(closestEnemy) ?: return
        if (AI_verbose) println("Closest enemy for ${troop.unitName}: ${closestEnemy.unitName} at ${closestEnemyTile.position}")

        val attackTile = findAttackTile(troop, closestEnemyTile)
        if (attackTile != null) {
            battleManager.performTurn(BattleActionRequest(
                troop = troop,
                targetPosition = closestEnemyTile,
                actionType = ActionType.ATTACK,
                attackTile = attackTile
            ))
        } else {
            val moveTarget = findBestMoveTarget(troop, closestEnemyTile)
            if (moveTarget != null) {
                if (AI_verbose) println("${troop.unitName} moving to ${moveTarget.position}")
                battleManager.performTurn(BattleActionRequest(
                    troop = troop,
                    targetPosition = moveTarget,
                    actionType = ActionType.MOVE
                ))
            } else {
                if (AI_verbose) println("${troop.unitName} cannot find a valid move target.")
            }
        }
    }

    private fun performRangedAction(troop: Troop) {
        val enemies = battleManager.getEnemies(troop)
        battleManager.getTroopTile(troop) ?: return

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return
        }

        val target = enemies
            .sortedWith(compareByDescending<Troop> { it.isRanged }.thenByDescending { it.speed })
            .firstOrNull() ?: return

        val targetTile = battleManager.getTroopTile(target) ?: return
        if (AI_verbose) println("Selected ranged target for ${troop.unitName}: ${target.unitName} at ${targetTile.position}")

        battleManager.performTurn(BattleActionRequest(
            troop = troop,
            targetPosition = targetTile,
            actionType = ActionType.SHOOT
        ))
    }

    private fun findAttackTile(troop: Troop, targetTile: IBattleTile): IBattleTile? {
        if (AI_verbose) println("Finding attack tile for ${troop.unitName} attacking ${targetTile.position}")

        val currentTile = battleManager.getTroopTile(troop) ?: return null

        if (targetTile.neighbors.contains(currentTile)) {
            if (AI_verbose) println("Troop is already in a valid attack position: ${currentTile.position}")
            return currentTile
        }

        val attackTile = targetTile.neighbors
            .filterIsInstance<IBattleTile>()
            .firstOrNull { battleManager.isTileAchievable(troop, it) && battleManager.isTileFree(it) }

        if (attackTile != null)
            if (AI_verbose) println("Found attack tile: ${attackTile.position}")
            else
                if (AI_verbose) println("No valid attack tile found for ${troop.unitName}")

        return attackTile
    }

    private fun isDirectionValid(troop: Troop, targetTile: IBattleTile, direction: Direction): Boolean {
        val attackTile = battleManager.battleField.getNeighborTile(targetTile, direction) ?: return false
        return battleManager.isTileAchievable(troop, attackTile) && battleManager.isTileFree(attackTile)
    }

    private fun findBestMoveTarget(troop: Troop, targetTile: IBattleTile): IBattleTile? {
        val reachableTiles = battleManager.getReachableTiles(troop)
        if (reachableTiles.isEmpty()) {
            if (AI_verbose) println("No reachable tiles for ${troop.unitName}")
            return null
        }

        val bestTile = reachableTiles.minByOrNull {
            HexMath.getDistance(it.position, targetTile.position)
        }
        if (AI_verbose) println("Best move target for ${troop.unitName}: ${bestTile?.position}")
        return bestTile as? IBattleTile
    }
}
