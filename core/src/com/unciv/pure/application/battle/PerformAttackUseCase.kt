package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.Point

object PerformAttackUseCase {
    data class Input(
        val attackerId: Int,
        val defenderId: Int?,
        val attackFrom: Point?,
        val current: Point?,
        val isTargetOccupiedByEnemy: Boolean,
        val isAttackFromAchievable: Boolean,
        val isAttackFromFree: Boolean,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderRemainingAmount: Int,
        val defenderDied: Boolean
    )

    data class Output(
        val success: Boolean,
        val rejection: BattleRejection? = null,
        val movedFrom: Point? = null,
        val movedTo: Point? = null,
        val isLuck: Boolean = false,
        val isMorale: Boolean = false,
        val defenderRemainingAmount: Int = 0,
        val defenderDied: Boolean = false
    )

    fun execute(input: Input): Output {
        if (input.defenderId == null || !input.isTargetOccupiedByEnemy || input.attackFrom == null) {
            return Output(success = false, rejection = BattleRejection.INVALID_TARGET)
        }
        if (!input.isAttackFromAchievable || (!input.isAttackFromFree && input.current != input.attackFrom)) {
            return Output(success = false, rejection = BattleRejection.INVALID_TARGET)
        }
        return Output(
            success = true,
            movedFrom = input.current,
            movedTo = input.attackFrom,
            isLuck = input.isLuck,
            isMorale = input.isMorale,
            defenderRemainingAmount = input.defenderRemainingAmount,
            defenderDied = input.defenderDied
        )
    }
}
