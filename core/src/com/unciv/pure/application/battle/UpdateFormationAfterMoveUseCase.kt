package com.unciv.pure.application.battle

/** Applies formation recovery or fatigue after a successful move. */
object UpdateFormationAfterMoveUseCase {
    data class Input(
        val currentFormation: Int,
        val maximumFormation: Int,
        val movementDistance: Int,
        val maximumMovement: Int,
        val turnEndsWithoutAttack: Boolean,
        val recoveryNumerator: Int = 15,
        val recoveryDenominator: Int = 100
    )

    data class Output(
        val remainingFormation: Int,
        val recoveryApplied: Boolean,
        val penaltyApplied: Boolean
    )

    fun execute(input: Input): Output {
        if (input.movementDistance == 1 && input.turnEndsWithoutAttack) {
            val restored = RestoreFormationUseCase.execute(
                RestoreFormationUseCase.Input(
                    currentFormation = input.currentFormation,
                    maximumFormation = input.maximumFormation,
                    recoveryNumerator = input.recoveryNumerator,
                    recoveryDenominator = input.recoveryDenominator
                )
            )
            return Output(
                remainingFormation = restored,
                recoveryApplied = restored > input.currentFormation,
                penaltyApplied = false
            )
        }

        val penalty = ApplyMovementFormationPenaltyUseCase.execute(
            ApplyMovementFormationPenaltyUseCase.Input(
                currentFormation = input.currentFormation,
                maximumFormation = input.maximumFormation,
                movementDistance = input.movementDistance,
                maximumMovement = input.maximumMovement
            )
        )
        return Output(
            remainingFormation = penalty.remainingFormation,
            recoveryApplied = false,
            penaltyApplied = penalty.penaltyApplied
        )
    }
}
