package com.unciv.pure.application.battle

/** Restores part of a surviving formation when its troop enters a new turn. */
object RestoreFormationUseCase {
    data class Input(
        val currentFormation: Int,
        val maximumFormation: Int,
        val recoveryNumerator: Int = 1,
        val recoveryDenominator: Int = 4
    )

    fun execute(input: Input): Int {
        require(input.currentFormation >= 0) { "currentFormation must not be negative" }
        require(input.maximumFormation >= 0) { "maximumFormation must not be negative" }
        require(input.currentFormation <= input.maximumFormation) {
            "currentFormation must not exceed maximumFormation"
        }
        require(input.recoveryNumerator >= 0) { "recoveryNumerator must not be negative" }
        require(input.recoveryDenominator > 0) { "recoveryDenominator must be positive" }

        if (input.currentFormation == 0 || input.currentFormation == input.maximumFormation) {
            return input.currentFormation
        }

        val restored = input.maximumFormation.toLong() * input.recoveryNumerator /
                input.recoveryDenominator
        return (input.currentFormation.toLong() + restored)
            .coerceAtMost(input.maximumFormation.toLong())
            .toInt()
    }
}
