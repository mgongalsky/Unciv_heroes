package com.unciv.logic.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.domain.battle.Point

fun BattleManager.execute(
    command: BattleCommand,
    onApplicationEvent: ((BattleEvent) -> Unit)? = null
): BattleCommandResult {
    val troop = getTroopById(command.troopId)
        ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)

    fun resolve(point: Point): IBattleTile? {
        val fieldTile =
                battleField.getTileAt(Vector2(point.x.toFloat(), point.y.toFloat())) as? IBattleTile
        if (fieldTile != null) return fieldTile

        val occupiedTiles = (getAttackerArmy().getAllTroops() + getDefenderArmy().getAllTroops())
            .asSequence()
            .filterNotNull()
            .mapNotNull(::getTroopTile)
            .toList()

        return occupiedTiles.asSequence()
            .flatMap { tile ->
                sequence {
                    yield(tile)
                    yieldAll(tile.neighbors.filterIsInstance<IBattleTile>())
                }
            }
            .firstOrNull { it.toPoint() == point }
    }

    return when (command) {
        is BattleCommand.Move -> {
            val target = resolve(command.target)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
            performMoveCommand(troop, target, onApplicationEvent)
        }

        is BattleCommand.Skip -> {
            getTroopTile(troop)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
            performSkipCommand(troop, onApplicationEvent)
        }

        is BattleCommand.Attack -> {
            val target = resolve(command.target)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
            val attackTile = command.attackFrom?.let(::resolve)
            performAttackCommand(troop, target, attackTile, onApplicationEvent)
        }

        is BattleCommand.Shoot -> {
            val target = resolve(command.target)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
            performShootCommand(troop, target, onApplicationEvent)
        }
    }
}
