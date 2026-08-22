package com.unciv.pure.application.battle

object PerformShootUseCase {
    data class Input(
        val attackerId: Int,
        val defenderId: Int?,
        val canShoot: Boolean,
        val isTargetOccupiedByEnemy: Boolean,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderRemainingAmount: Int,
        val defenderDied: Boolean
    )

    data class Output(
        val success: Boolean,
        val rejection: BattleRejection? = null,
        val isLuck: Boolean = false,
        val isMorale: Boolean = false,
        val defenderRemainingAmount: Int = 0,
        val defenderDied: Boolean = false
    )

    fun execute(input: Input): Output {
        if (input.defenderId == null) {
            return Output(success = false, rejection = BattleRejection.INVALID_TARGET)
        }
        if (!input.canShoot) {
            return Output(success = false, rejection = BattleRejection.NOT_IMPLEMENTED)
        }
        if (!input.isTargetOccupiedByEnemy) {
            return Output(success = false, rejection = BattleRejection.INVALID_TARGET)
        }
        return Output(
            success = true,
            isLuck = input.isLuck,
            isMorale = input.isMorale,
            defenderRemainingAmount = input.defenderRemainingAmount,
            defenderDied = input.defenderDied
        )
    }
}
