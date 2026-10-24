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
    BattleDiagnostics.shared.log {
        "command=$command currentTroop=${getCurrentTroop()?.id}"
    }
    val recordEvent: (BattleEvent) -> Unit = { event ->
        BattleDiagnostics.shared.log { "event=$event" }
        onApplicationEvent?.invoke(event)
    }

    fun resolve(point: Point): IBattleTile? {
        val fieldTile = battleField.getTileAt(Vector2(point.x.toFloat(), point.y.toFloat())) as? IBattleTile
        if (fieldTile != null) return fieldTile
        val occupiedTiles = (getAttackerArmy().getAllTroops() + getDefenderArmy().getAllTroops())
            .asSequence().filterNotNull().mapNotNull(::getTroopTile).toList()
        return occupiedTiles.asSequence()
            .flatMap { tile ->
                sequence {
                    yield(tile)
                    yieldAll(tile.neighbors.filterIsInstance<IBattleTile>())
                }
            }
            .firstOrNull { it.toPoint() == point }
    }

    // Keep early rejections inside dispatch so every command gets a result record.
    fun dispatch(): BattleCommandResult {
        val troop = getTroopById(command.troopId)
            ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
        return when (command) {
            is BattleCommand.Move -> {
                val target = resolve(command.target)
                    ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
                performMoveCommand(troop, target, recordEvent)
            }
            is BattleCommand.Skip -> {
                getTroopTile(troop)
                    ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
                performSkipCommand(troop, recordEvent)
            }
            is BattleCommand.Attack -> {
                val target = resolve(command.target)
                    ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
                val attackTile = command.attackFrom?.let(::resolve)
                performAttackCommand(troop, target, attackTile, recordEvent)
            }
            is BattleCommand.Shoot -> {
                val target = resolve(command.target)
                    ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
                performShootCommand(troop, target, recordEvent)
            }
        }
    }

    val result = dispatch()
    BattleDiagnostics.shared.log { "result troop=${command.troopId} $result" }
    return result
}
