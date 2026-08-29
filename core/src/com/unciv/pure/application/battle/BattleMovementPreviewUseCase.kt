package com.unciv.pure.application.battle

/** Classifies a reachable battle tile for movement-range presentation. */
object BattleMovementPreviewUseCase {
    enum class TileStyle {
        HIDDEN,
        SAFE,
        FORMATION_PENALTY
    }

    fun execute(
        isReachable: Boolean,
        movementDistance: Int,
        maximumMovement: Int
    ): TileStyle {
        if (!isReachable) return TileStyle.HIDDEN
        return if (ApplyMovementFormationPenaltyUseCase.wouldApplyPenalty(
                movementDistance = movementDistance,
                maximumMovement = maximumMovement
            )
        ) TileStyle.FORMATION_PENALTY
        else TileStyle.SAFE
    }
}
