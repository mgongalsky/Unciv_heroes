package com.unciv.logic.battle

import BattleActionResult
import ErrorId
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest

fun BattleManager.performTurn(
    actionRequest: BattleActionRequest,
    onApplicationEvent: ((BattleEvent) -> Unit)? = null
): BattleActionResult =
        BattleManagerLegacyAdapter.execute(this, actionRequest, onApplicationEvent)

private object BattleManagerLegacyAdapter {
    fun execute(
        manager: BattleManager,
        actionRequest: BattleActionRequest,
        onApplicationEvent: ((BattleEvent) -> Unit)?
    ): BattleActionResult {
        val troop = actionRequest.troop
        val currentTile = manager.getTroopTile(troop)

        val result = when (actionRequest.actionType) {
            ActionType.SKIP -> manager.performSkipCommand(troop, onApplicationEvent)
            ActionType.MOVE -> manager.performMoveCommand(
                troop,
                actionRequest.targetPosition,
                onApplicationEvent
            )

            ActionType.ATTACK -> manager.performAttackCommand(
                troop,
                actionRequest.targetPosition,
                actionRequest.attackTile,
                onApplicationEvent
            )

            ActionType.SHOOT -> manager.performShootCommand(
                troop,
                actionRequest.targetPosition,
                onApplicationEvent
            )
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
