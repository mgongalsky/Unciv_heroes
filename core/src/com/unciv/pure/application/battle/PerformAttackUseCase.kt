// com/unciv/pure/application/battle/PerformAttackUseCase.kt
package com.unciv.pure.application.battle

import ErrorId
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.ActionType

object PerformAttackUseCase {

    data class Input(
        val attacker: Troop,
        val defender: Troop?,
        val attackTile: IBattleTile?,
        val currentTile: IBattleTile?,
        val isTargetOccupiedByEnemy: Boolean,
        val isAttackTileAchievable: Boolean,
        val isAttackTileFree: Boolean,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderRemainingAmount: Int,
        val defenderDied: Boolean
    )

    data class Output(
        val success: Boolean,
        val errorId: ErrorId? = null,
        val movedFrom: IBattleTile? = null,
        val movedTo: IBattleTile? = null,
        val isLuck: Boolean = false,
        val isMorale: Boolean = false,
        val defenderRemainingAmount: Int = 0,
        val defenderDied: Boolean = false
    )

    fun execute(input: Input): Output {
        if (input.defender == null) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
        }
        if (!input.isTargetOccupiedByEnemy) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
        }
        if (input.attackTile == null) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
        }
        if (!input.isAttackTileAchievable ||
                (!input.isAttackTileFree && input.currentTile != input.attackTile)
        ) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
        }
        return Output(
            success = true,
            movedFrom = input.currentTile,
            movedTo = input.attackTile,
            isLuck = input.isLuck,
            isMorale = input.isMorale,
            defenderRemainingAmount = input.defenderRemainingAmount,
            defenderDied = input.defenderDied
        )
    }
}
