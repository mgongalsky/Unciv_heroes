// com/unciv/pure/application/battle/PerformMoveUseCase.kt
package com.unciv.pure.application.battle

import ErrorId
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.battlescreen.ActionType

object PerformMoveUseCase {

    data class Input(
        val troop: Troop,
        val targetTile: IBattleTile,
        val currentTile: IBattleTile?,
        val isTileAchievable: Boolean,
        val isTileOccupiedByAlly: Boolean,
        val isTileFree: Boolean
    )

    data class Output(
        val success: Boolean,
        val errorId: ErrorId? = null,
        val movedFrom: IBattleTile? = null,
        val movedTo: IBattleTile? = null,
        val isMorale: Boolean = false
    )

    fun execute(input: Input): Output {
        if (!input.isTileAchievable) {
            return Output(success = false, errorId = ErrorId.TOO_FAR)
        }
        if (input.isTileOccupiedByAlly) {
            return Output(success = false, errorId = ErrorId.OCCUPIED_BY_ALLY)
        }
        if (!input.isTileFree) {
            return Output(success = false, errorId = ErrorId.HEX_OCCUPIED)
        }
        return Output(
            success = true,
            movedFrom = input.currentTile,
            movedTo = input.targetTile
        )
    }
}
