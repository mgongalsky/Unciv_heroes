package com.unciv.ai

import com.unciv.logic.Direction
import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.application.battle.BattleCommandResult

class AIBattle(
    private val battleManager: BattleManager,
    private val onApplicationEvent: ((com.unciv.pure.application.battle.BattleEvent) -> Unit)? = null
) {

    companion object {
        var AI_verbose = true
    }

    fun performTurn(troop: Troop): BattleCommandResult? {
        if (AI_verbose) println("AI Turn: ${troop.unitName} at ${battleManager.getTroopTile(troop)?.position}")
        return if (troop.isRanged) performRangedAction(troop)
        else performMeleeAction(troop)
    }

    private fun performMeleeAction(troop: Troop): BattleCommandResult? {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return null

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return null
        }

        val closestEnemy = enemies.minByOrNull {
            battleManager.getTroopTile(it)?.let { tile ->
                HexMath.getDistance(currentTile.position, tile.position)
            } ?: Int.MAX_VALUE
        } ?: return null

        val closestEnemyTile = battleManager.getTroopTile(closestEnemy) ?: return null
        if (AI_verbose) println("Closest enemy for ${troop.unitName}: ${closestEnemy.unitName} at ${closestEnemyTile.position}")

        val attackTile = findAttackTile(troop, closestEnemyTile)
        return if (attackTile != null) {
            battleManager.execute(
                BattleCommand.Attack(
                    troopId = troop.id,
                    target = closestEnemyTile.toPoint(),
                    attackFrom = attackTile.toPoint()
                ),
                onApplicationEvent
            )
        } else {
            val moveTarget = findBestMoveTarget(troop, closestEnemyTile)
            if (moveTarget != null) {
                if (AI_verbose) println("${troop.unitName} moving to ${moveTarget.position}")
                battleManager.execute(
                    BattleCommand.Move(troop.id, moveTarget.toPoint()),
                    onApplicationEvent
                )
            } else {
                if (AI_verbose) println("${troop.unitName} cannot find a valid move target.")
                null
            }
        }
    }

    private fun performRangedAction(troop: Troop): BattleCommandResult? {
        val enemies = battleManager.getEnemies(troop)
        battleManager.getTroopTile(troop) ?: return null

        if (enemies.isEmpty()) {
            if (AI_verbose) println("No enemies left for ${troop.unitName}")
            return null
        }

        val target = enemies
            .sortedWith(compareByDescending<Troop> { it.isRanged }.thenByDescending { it.speed })
            .firstOrNull() ?: return null

        val targetTile = battleManager.getTroopTile(target) ?: return null
        if (AI_verbose) println("Selected ranged target for ${troop.unitName}: ${target.unitName} at ${targetTile.position}")

        return battleManager.execute(
            BattleCommand.Shoot(troop.id, targetTile.toPoint()),
            onApplicationEvent
        )
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
            .firstOrNull {
                battleManager.isTileAchievable(troop, it) && battleManager.isTileFree(it)
            }

        if (attackTile != null)
            if (AI_verbose) println("Found attack tile: ${attackTile.position}")
            else if (AI_verbose) println("No valid attack tile found for ${troop.unitName}")

        return attackTile
    }

    private fun isDirectionValid(
        troop: Troop,
        targetTile: IBattleTile,
        direction: Direction
    ): Boolean {
        val attackTile =
                battleManager.battleField.getNeighborTile(targetTile, direction) ?: return false
        return battleManager.isTileAchievable(troop, attackTile) && battleManager.isTileFree(
            attackTile
        )
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
