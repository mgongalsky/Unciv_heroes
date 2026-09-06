package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.pure.application.battle.BattleTileAppearanceUseCase.Appearance
import com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup

/** Applies the same tile appearance on refresh, cursor entry and cursor exit. */
class BattleTileHighlightRenderer {
    private val appearances = mutableMapOf<TileGroup, Appearance>()
    private val controlOverlays = mutableMapOf<TileGroup, Image>()
    private val hoveredTiles = mutableSetOf<TileGroup>()

    fun bind(tile: TileGroup) {
        tile.addListener(object : InputListener() {
            override fun enter(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                fromActor: Actor?
            ) {
                if (pointer != -1 || fromActor?.isDescendantOf(tile) == true) return
                hoveredTiles.add(tile)
                apply(tile)
            }

            override fun exit(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: Actor?
            ) {
                if (pointer != -1 || toActor?.isDescendantOf(tile) == true) return
                hoveredTiles.remove(tile)
                apply(tile)
            }
        })
    }

    fun update(tile: TileGroup, appearance: Appearance) {
        appearances[tile] = appearance
        apply(tile)
    }

    fun restoreAll() {
        appearances.keys.forEach(::apply)
    }

    private fun apply(tile: TileGroup) {
        val appearance = appearances[tile] ?: Appearance()
        tile.baseLayerGroup.color = Color(
            1f, 1f, 1f,
            if (tile in hoveredTiles) 0.3f else appearance.terrainAlpha
        )
        if (appearance.control == Strength.NONE) {
            controlOverlays[tile]?.isVisible = false
            return
        }
        val overlay = controlOverlays.getOrPut(tile) {
            val reference = ImageGetter.getDrawable(tile.tileSetStrings.highlight)
            Image(BattleControlOutlineDrawable(reference.minWidth, reference.minHeight)).apply {
                name = "battleZoneOfControlOverlay"
                touchable = Touchable.disabled
                tile.sizeAndPlaceOverHex(this, 0.72f)
                tile.highlightFogCrosshairLayerGroup.addActor(this)
            }
        }
        overlay.color = Color.valueOf(
            if (appearance.control == Strength.NORMAL) "B87928" else "A6443C"
        ).apply { a = 0.85f }
        overlay.isVisible = true
        // Keep control readable when the independent Shift preview also covers this tile.
        overlay.toFront()
    }
}
