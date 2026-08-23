package com.unciv.pure.application.battle

/** Splits incoming damage between formation cohesion and the soldiers behind it. */
object ApplyFormationDamageUseCase {
    data class Input(
        val incomingDamage: Int,
        val currentFormation: Int,
        val formationDamageNumerator: Int = 1,
        val formationDamageDenominator: Int = 2
    )

    data class Output(
        val absorbedByFormation: Int,
        val remainingFormation: Int,
        val damageToSoldiers: Int
    )

    fun execute(input: Input): Output {
        require(input.incomingDamage >= 0) { "incomingDamage must not be negative" }
        require(input.currentFormation >= 0) { "currentFormation must not be negative" }
        require(input.formationDamageNumerator >= 0) { "formationDamageNumerator must not be negative" }
        require(input.formationDamageDenominator > 0) { "formationDamageDenominator must be positive" }
        require(input.formationDamageNumerator <= input.formationDamageDenominator) {
            "formation damage share must not exceed incoming damage"
        }

        val allocatedToFormation =
                input.incomingDamage * input.formationDamageNumerator / input.formationDamageDenominator
        val absorbed = minOf(allocatedToFormation, input.currentFormation)
        return Output(
            absorbedByFormation = absorbed,
            remainingFormation = input.currentFormation - absorbed,
            damageToSoldiers = input.incomingDamage - absorbed
        )
    }
}
