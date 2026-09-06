package com.unciv.pure.domain.battle

/** Evaluates one movement step without reading or mutating battlefield state. */
object ZoneOfControlTransition {
    enum class Strength { NONE, NORMAL, REINFORCED }

    data class Input(
        val from: Strength,
        val to: Strength,
        val remainingMovement: Float,
        val isFirstStep: Boolean,
        val baseCost: Float = 1f,
        val normalMultiplier: Float = 2f
    )

    data class Output(
        val allowed: Boolean,
        val movementCost: Float,
        val endsMovement: Boolean
    )

    fun strength(distinctEnemyCount: Int): Strength {
        require(distinctEnemyCount >= 0)
        return when (distinctEnemyCount) {
            0 -> Strength.NONE
            1 -> Strength.NORMAL
            else -> Strength.REINFORCED
        }
    }

    fun execute(input: Input): Output {
        require(input.baseCost.isFinite() && input.baseCost > 0f)
        require(input.remainingMovement.isFinite() && input.remainingMovement >= 0f)
        require(input.normalMultiplier.isFinite() && input.normalMultiplier >= 1f)

        val cost = input.baseCost * if (
            input.from == Strength.NORMAL && input.to != Strength.NONE
        ) input.normalMultiplier else 1f
        val blocked = input.from == Strength.REINFORCED && !input.isFirstStep
        val endsMovement = input.to == Strength.REINFORCED ||
                (input.from == Strength.REINFORCED && input.to != Strength.NONE)
        return Output(
            allowed = !blocked && cost <= input.remainingMovement,
            movementCost = cost,
            endsMovement = endsMovement
        )
    }
}
