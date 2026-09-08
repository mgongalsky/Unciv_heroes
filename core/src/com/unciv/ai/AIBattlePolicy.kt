package com.unciv.ai

import com.unciv.logic.HexMath
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.domain.troop.Troop

class AIBattlePolicy(
    private val battleManager: BattleManager,
    private val targetHasRetaliationRemaining: (Troop) -> Boolean = battleManager::hasRetaliationRemaining
) : BattlePolicy {
    override fun chooseCommand(troopId: Int): BattleCommand? {
        val troop = battleManager.getTroopById(troopId) ?: return null
        if (battleManager.hasPendingFollowUpShot(troop)) {
            return if (battleManager.canShoot(troop)) {
                chooseRangedCommand(troop) ?: BattleCommand.Skip(troop.id)
            } else BattleCommand.Skip(troop.id)
        }
        if (battleManager.canFullyRestoreFormation(troop)) return BattleCommand.Skip(troop.id)
        return if (troop.isRanged) chooseRangedCommand(troop) else chooseMeleeCommand(troop)
    }

    private fun chooseMeleeCommand(troop: Troop): BattleCommand? {
        val enemies = battleManager.getEnemies(troop)
        val currentTile = battleManager.getTroopTile(troop) ?: return null
        if (enemies.isEmpty()) return null
        val preferredEnemy = enemies.minWithOrNull(
            compareBy<Troop> { if (it.formation.isBroken) 0 else 1 }
                .thenBy {
                    battleManager.getTroopTile(it)?.let { tile ->
                        HexMath.getDistance(currentTile.position, tile.position)
                    } ?: Int.MAX_VALUE
                }
                .thenBy { if (targetHasRetaliationRemaining(it)) 1 else 0 }
        ) ?: return null
        val preferredEnemyTile = battleManager.getTroopTile(preferredEnemy) ?: return null
        val attackTile = findAttackTile(troop, preferredEnemyTile)
        if (attackTile != null) return BattleCommand.Attack(
            troopId = troop.id,
            target = preferredEnemyTile.toPoint(),
            attackFrom = attackTile.toPoint()
        )
        val moveTarget = findBestMoveTarget(troop, preferredEnemyTile) ?: return null
        return BattleCommand.Move(troop.id, moveTarget.toPoint())
    }

    private fun chooseRangedCommand(troop: Troop): BattleCommand? {
        val currentTile = battleManager.getTroopTile(troop) ?: return null
        val enemies = battleManager.getEnemies(troop).filter { it.currentAmount > 0 }
        if (!battleManager.canShoot(troop)) {
            val adjacentEnemy = enemies.filter { enemy ->
                battleManager.getTroopTile(enemy)?.let { it in currentTile.neighbors } == true
            }.minWithOrNull(
                compareBy<Troop> { if (it.formation.isBroken) 0 else 1 }
                    .thenBy { if (targetHasRetaliationRemaining(it)) 1 else 0 }
            ) ?: return chooseMeleeCommand(troop)
            val targetTile = battleManager.getTroopTile(adjacentEnemy) ?: return null
            return BattleCommand.Attack(troop.id, targetTile.toPoint(), currentTile.toPoint())
        }
        val target = enemies.sortedWith(
            compareByDescending<Troop> { it.formation.isBroken }
                .thenByDescending { it.isRanged }
                .thenByDescending { it.speed }
        ).firstOrNull() ?: return null
        val targetTile = battleManager.getTroopTile(target) ?: return null
        return BattleCommand.Shoot(troop.id, targetTile.toPoint())
    }

    private fun findAttackTile(troop: Troop, targetTile: IBattleTile): IBattleTile? {
        val currentTile = battleManager.getTroopTile(troop) ?: return null
        val candidates = targetTile.neighbors.filterIsInstance<IBattleTile>().filter {
            it == currentTile ||
                    (battleManager.isTileAchievable(troop, it) && battleManager.isTileFree(it))
        }
        return candidates.sortedWith(
            compareByDescending<IBattleTile> { battleManager.getSupportBonusPercent(troop, it) }
                .thenBy { if (it == currentTile) 0 else 1 }
        ).firstOrNull()
    }

    private fun findBestMoveTarget(troop: Troop, targetTile: IBattleTile): IBattleTile? =
            battleManager.getReachableTiles(troop).minWithOrNull(
                compareBy<IBattleTile> { HexMath.getDistance(it.position, targetTile.position) }
                    .thenByDescending { battleManager.getSupportBonusPercent(troop, it) }
            )
}
