package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.pure.application.battle.BattleMovementPreviewUseCase.TileStyle
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup

private const val MOVEMENT_PREVIEW_SCALE = 0.72f
private const val SAFE_ALPHA = 0.58f
private const val PENALTY_ALPHA = 0.28f
private val MOVEMENT_PREVIEW_COLOR = Color.valueOf("#4A90E2")

internal fun createBattleMovementRangeOverlay(tileGroup: TileGroup): Image =
    ImageGetter.getImage(tileGroup.tileSetStrings.highlight).apply {
        name = "battleMovementRangeOverlay"
        touchable = Touchable.disabled
        isVisible = false
        tileGroup.sizeAndPlaceOverHex(this, MOVEMENT_PREVIEW_SCALE)
        tileGroup.highlightFogCrosshairLayerGroup.addActor(this)
    }

internal fun Image.showBattleMovementStyle(style: TileStyle) {
    isVisible = style != TileStyle.HIDDEN
    if (!isVisible) return
    color = MOVEMENT_PREVIEW_COLOR.cpy().apply {
        a = if (style == TileStyle.SAFE) SAFE_ALPHA else PENALTY_ALPHA
    }
}
