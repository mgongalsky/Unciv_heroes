package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.Point

object PerformMoveUseCase {
    data class Input(
        val target: Point,
        val current: Point?,
        val isTargetAchievable: Boolean,
        val isTargetOccupiedByAlly: Boolean,
        val isTargetFree: Boolean
    )

    data class Output(
        val success: Boolean,
        val rejection: BattleRejection? = null,
        val movedFrom: Point? = null,
        val movedTo: Point? = null
    )

    fun execute(input: Input): Output {
        if (!input.isTargetAchievable) {
            return Output(success = false, rejection = BattleRejection.TOO_FAR)
        }
        if (input.isTargetOccupiedByAlly) {
            return Output(success = false, rejection = BattleRejection.OCCUPIED_BY_ALLY)
        }
        if (!input.isTargetFree) {
            return Output(success = false, rejection = BattleRejection.HEX_OCCUPIED)
        }
        return Output(success = true, movedFrom = input.current, movedTo = input.target)
    }
}
