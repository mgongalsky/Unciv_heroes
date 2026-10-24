package com.unciv.pure.application.battle

/** Follow-up shooting uses the same half-movement boundary as the battle highlight. */
object MoveAndShootRules {
    const val DEFAULT_PENALTY_PERCENT = 50
    fun grantsShot(
        isRanged: Boolean,
        movementDistance: Int,
        maximumMovement: Int,
        alreadyGranted: Boolean
    ): Boolean {
        require(movementDistance >= 0)
        require(maximumMovement >= 0)
        return isRanged && !alreadyGranted && movementDistance > 0 &&
                !ApplyMovementFormationPenaltyUseCase.wouldApplyPenalty(
                    movementDistance, maximumMovement
                )
    }

    fun shotDamage(
        rawDamage: Int,
        afterMovement: Boolean,
        penaltyPercent: Int = DEFAULT_PENALTY_PERCENT
    ): Int {
        require(rawDamage >= 0)
        require(penaltyPercent in 0..100)
        return if (afterMovement) (rawDamage.toLong() * (100 - penaltyPercent) / 100).toInt()
        else rawDamage
    }
}
