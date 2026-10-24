package com.unciv.logic.battle.session

import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.battle.Point

/** Presentation hints only. Submission still validates ownership, revision and combat rules. */
object BattleAvailableCommandsReader {
    fun capture(manager: BattleManager): List<BattleCommand> {
        if (!manager.isBattleOn()) return emptyList()
        val troop = manager.getCurrentTroop() ?: return emptyList()
        val current = manager.getTroopTile(troop) ?: return emptyList()
        if (troop.currentAmount <= 0) return emptyList()
        fun IBattleTile.point() = Point(position.x.toInt(), position.y.toInt())
        val commands = mutableListOf<BattleCommand>(BattleCommand.Skip(troop.id))
        manager.getReachableTiles(troop).forEach { tile ->
            if (tile != current && manager.isTileFree(tile)) {
                commands.add(BattleCommand.Move(troop.id, tile.point()))
            }
        }
        val canShoot = manager.canShoot(troop)
        manager.getEnemies(troop).filter { it.currentAmount > 0 }.forEach { enemy ->
            val target = manager.getTroopTile(enemy) ?: return@forEach
            if (canShoot) {
                if (manager.getAttackPreview(troop, enemy)?.isRanged == true) {
                    commands.add(BattleCommand.Shoot(troop.id, target.point()))
                }
            } else if (!manager.hasPendingFollowUpShot(troop)) {
                target.neighbors.filterIsInstance<IBattleTile>().forEach { approach ->
                    val preview = manager.getAttackPreview(troop, enemy, approach)
                    if (preview != null && !preview.isRanged) {
                        commands.add(BattleCommand.Attack(troop.id, target.point(), approach.point()))
                    }
                }
            }
        }
        return commands.distinct()
    }
}
