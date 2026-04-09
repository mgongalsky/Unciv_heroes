// com/unciv/pure/application/battle/PerformShootUseCase.kt
package com.unciv.pure.application.battle

import ErrorId
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.troop.Troop

object PerformShootUseCase {

    data class Input(
        val attacker: Troop,
        val defender: Troop?,
        val canShoot: Boolean,
        val isTargetOccupiedByEnemy: Boolean,
        val isLuck: Boolean,
        val isMorale: Boolean,
        val defenderRemainingAmount: Int,
        val defenderDied: Boolean
    )

    data class Output(
        val success: Boolean,
        val errorId: ErrorId? = null,
        val isLuck: Boolean = false,
        val isMorale: Boolean = false,
        val defenderRemainingAmount: Int = 0,
        val defenderDied: Boolean = false
    )

    fun execute(input: Input): Output {
        if (input.defender == null) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
        }
        if (!input.canShoot) {
            return Output(success = false, errorId = ErrorId.NOT_IMPLEMENTED)
        }
        if (!input.isTargetOccupiedByEnemy) {
            return Output(success = false, errorId = ErrorId.INVALID_TARGET)
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
