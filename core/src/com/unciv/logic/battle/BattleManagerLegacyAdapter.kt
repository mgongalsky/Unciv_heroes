package com.unciv.logic.battle

import BattleActionResult
import ErrorId
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

fun BattleManager.performTurn(actionRequest: BattleActionRequest): BattleActionResult =
        BattleManagerLegacyAdapter.execute(this, actionRequest)

private object BattleManagerLegacyAdapter {
    fun execute(
        manager: BattleManager,
        actionRequest: BattleActionRequest
    ): BattleActionResult {
        val troop = actionRequest.troop
        val currentTile = manager.getTroopTile(troop)

        val result = when (actionRequest.actionType) {
            ActionType.SKIP -> manager.performSkipCommand(troop)
            ActionType.MOVE -> manager.performMoveCommand(troop, actionRequest.targetPosition)
            ActionType.ATTACK -> manager.performAttackCommand(
                troop,
                actionRequest.targetPosition,
                actionRequest.attackTile
            )

            ActionType.SHOOT -> manager.performShootCommand(troop, actionRequest.targetPosition)
        }

        return BattleActionResult(
            actionType = actionRequest.actionType,
            success = result.success,
            movedFrom = when (actionRequest.actionType) {
                ActionType.MOVE, ActionType.ATTACK -> if (result.success) currentTile else null
                else -> null
            },
            movedTo = when (actionRequest.actionType) {
                ActionType.MOVE -> if (result.success) actionRequest.targetPosition else null
                ActionType.ATTACK -> if (result.success) actionRequest.attackTile else null
                else -> null
            },
            errorId = result.rejection?.let { ErrorId.valueOf(it.name) },
            isLuck = result.isLuck,
            isMorale = result.isMorale,
            battleEnded = result.battleEnded
        )
    }
}
