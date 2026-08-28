package com.unciv.pure.application.battle

/** Reduces formation after a troop moves strictly more than half of its movement allowance. */
object ApplyMovementFormationPenaltyUseCase {
    data class Input(
        val currentFormation: Int,
        val maximumFormation: Int,
        val movementDistance: Int,
        val maximumMovement: Int,
        val penaltyNumerator: Int = 1,
        val penaltyDenominator: Int = 4
    )

    data class Output(
        val remainingFormation: Int,
        val penaltyApplied: Boolean
    )

    fun execute(input: Input): Output {
        require(input.currentFormation >= 0) { "currentFormation must not be negative" }
        require(input.maximumFormation >= 0) { "maximumFormation must not be negative" }
        require(input.currentFormation <= input.maximumFormation) {
            "currentFormation must not exceed maximumFormation"
        }
        require(input.movementDistance >= 0) { "movementDistance must not be negative" }
        require(input.maximumMovement >= 0) { "maximumMovement must not be negative" }
        require(input.penaltyNumerator >= 0) { "penaltyNumerator must not be negative" }
        require(input.penaltyDenominator > 0) { "penaltyDenominator must be positive" }

        if (input.movementDistance.toLong() * 2L <= input.maximumMovement.toLong()) {
            return Output(input.currentFormation, penaltyApplied = false)
        }

        val penalty = input.maximumFormation.toLong() * input.penaltyNumerator /
                input.penaltyDenominator
        val remaining = (input.currentFormation.toLong() - penalty).coerceAtLeast(0L).toInt()
        return Output(remaining, penaltyApplied = true)
    }
}
