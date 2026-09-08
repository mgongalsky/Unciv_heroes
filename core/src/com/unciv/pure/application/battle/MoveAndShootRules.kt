package com.unciv.pure.application.battle

/** Follow-up shooting uses the same half-movement boundary as the battle highlight. */
object MoveAndShootRules {
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

    /** Apply after luck, before formation absorption, rounding the total hit down. */
    fun shotDamage(rawDamage: Int, afterMovement: Boolean, penaltyPercent: Int = 50): Int {
        require(rawDamage >= 0)
        require(penaltyPercent in 0..100)
        return if (afterMovement) (rawDamage.toLong() * (100 - penaltyPercent) / 100).toInt()
        else rawDamage
    }
}
