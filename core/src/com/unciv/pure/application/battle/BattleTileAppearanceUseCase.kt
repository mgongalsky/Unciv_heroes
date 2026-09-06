package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength

/** Presentation values for a tile, independent of cursor state and Scene2D. */
object BattleTileAppearanceUseCase {
    data class Appearance(val terrainAlpha: Float = 1f, val control: Strength = Strength.NONE)

    fun execute(
        isReachable: Boolean,
        movementDistance: Int,
        maximumMovement: Int,
        control: Strength,
        canDisplayControl: Boolean
    ): Appearance {
        val safe = isReachable && movementDistance > 0 &&
                !ApplyMovementFormationPenaltyUseCase.wouldApplyPenalty(
                    movementDistance,
                    maximumMovement
                )
        return Appearance(
            terrainAlpha = when {
                safe -> 0.5f
                isReachable -> 0.7f
                else -> 1f
            },
            control = if (isReachable && canDisplayControl) control else Strength.NONE
        )
    }
}
