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
        maximumMovement: Int,
        hasFormation: Boolean = true,
        ignoresMovementFormationPenalty: Boolean = false
    ): TileStyle {
        if (!isReachable) return TileStyle.HIDDEN
        val exceedsHalfMovement = ApplyMovementFormationPenaltyUseCase.wouldApplyPenalty(
            movementDistance, maximumMovement
        )
        return if (hasFormation && !ignoresMovementFormationPenalty && exceedsHalfMovement)
            TileStyle.FORMATION_PENALTY
        else TileStyle.SAFE
    }
}
