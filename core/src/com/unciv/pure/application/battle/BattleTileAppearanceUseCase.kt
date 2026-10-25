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
        canDisplayControl: Boolean,
        hasFormation: Boolean = true,
        ignoresMovementFormationPenalty: Boolean = false
    ): Appearance {
        val style = BattleMovementPreviewUseCase.execute(
            isReachable, movementDistance, maximumMovement,
            hasFormation, ignoresMovementFormationPenalty
        )
        val safe = movementDistance > 0 && style == BattleMovementPreviewUseCase.TileStyle.SAFE
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
