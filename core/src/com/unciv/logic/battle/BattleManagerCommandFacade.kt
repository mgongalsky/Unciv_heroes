package com.unciv.logic.battle

import BattleActionResult
import ErrorId
import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.domain.battle.Point
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

fun BattleManager.execute(
    command: BattleCommand,
    onApplicationEvent: ((com.unciv.pure.application.battle.BattleEvent) -> Unit)? = null
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

    fun executeWithApplicationEvents(action: () -> BattleActionResult): BattleCommandResult {
        if (onApplicationEvent == null) return action().toCommandResult()

        val legacyHandler = onEvent
        onEvent = { event ->
            legacyHandler?.invoke(event)
            onApplicationEvent(event.toApplicationEvent())
        }
        return try {
            action().toCommandResult()
        } finally {
            onEvent = legacyHandler
        }
    }

    if (command is BattleCommand.Move) {
        val target = resolve(command.target)
            ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
        return executeWithApplicationEvents {
            performMoveCommand(troop, target)
        }
    }

    if (command is BattleCommand.Skip) {
        getTroopTile(troop)
            ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET)
        return executeWithApplicationEvents {
            performSkipCommand(troop)
        }
    }

    val request = when (command) {
        is BattleCommand.Move -> error("MOVE command is handled before legacy request mapping")
        is BattleCommand.Skip -> error("SKIP command is handled before legacy request mapping")

        is BattleCommand.Attack -> BattleActionRequest(
            troop = troop,
            targetPosition = resolve(command.target)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET),
            actionType = ActionType.ATTACK,
            attackTile = command.attackFrom?.let(::resolve)
        )

        is BattleCommand.Shoot -> BattleActionRequest(
            troop = troop,
            targetPosition = resolve(command.target)
                ?: return BattleCommandResult(false, rejection = BattleRejection.INVALID_TARGET),
            actionType = ActionType.SHOOT
        )
    }

    return executeWithApplicationEvents { performTurn(request) }
}

private fun BattleActionResult.toCommandResult() = BattleCommandResult(
    success = success,
    movedFrom = movedFrom?.toPoint(),
    movedTo = movedTo?.toPoint(),
    rejection = errorId?.toRejection(),
    isLuck = isLuck,
    isMorale = isMorale,
    battleEnded = battleEnded
)

private fun ErrorId.toRejection() = BattleRejection.valueOf(name)

private fun com.unciv.pure.domain.battle.BattleEvent.toApplicationEvent(): com.unciv.pure.application.battle.BattleEvent =
        when (this) {
            is com.unciv.pure.domain.battle.BattleEvent.TroopMoved ->
                com.unciv.pure.application.battle.BattleEvent.TroopMoved(
                    troopId,
                    from,
                    to,
                    isMorale
                )

            is com.unciv.pure.domain.battle.BattleEvent.TroopAttacked ->
                com.unciv.pure.application.battle.BattleEvent.TroopAttacked(
                    attackerId, defenderId, defenderRemainingAmount, isLuck, isMorale, defenderDied
                )

            is com.unciv.pure.domain.battle.BattleEvent.TroopShot ->
                com.unciv.pure.application.battle.BattleEvent.TroopShot(
                    attackerId, defenderId, defenderRemainingAmount, isLuck, isMorale, defenderDied
                )

            is com.unciv.pure.domain.battle.BattleEvent.TurnAdvanced ->
                com.unciv.pure.application.battle.BattleEvent.TurnAdvanced(nextTroopId)

            is com.unciv.pure.domain.battle.BattleEvent.BattleEnded ->
                com.unciv.pure.application.battle.BattleEvent.BattleEnded(winnerIsAttacker)

            com.unciv.pure.domain.battle.BattleEvent.TurnSkipped ->
                com.unciv.pure.application.battle.BattleEvent.TurnSkipped
        }
