package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen

/** Рисует внешние рёбра одной зоны на клетке; геометрия согласована с изображением гекса. */
class StoryZoneOverlay(
    tile: TileGroup,
    val zoneId: String,
    val boundaryEdges: Set<Int>,
    title: String?,
    tint: Color,
    overlapIndex: Int = 0
) : Group() {
    init {
        name = "story-zone-overlay:$zoneId"
        touchable = Touchable.disabled
        isTransform = false
        setSize(tile.width, tile.height)
        val bounds = Image(ImageGetter.getDrawable(tile.tileSetStrings.hexagon))
        tile.sizeAndPlaceOverHex(bounds)
        // Небольшое смещение внутрь позволяет различить совпадающие границы разных зон.
        val inset = (overlapIndex * 2f).coerceAtMost(bounds.width * 0.15f)
        val left = bounds.x + inset
        val bottom = bounds.y + inset
        val hexWidth = bounds.width - inset * 2f
        val hexHeight = bounds.height - inset * 2f
        val points = floatArrayOf(
            0.5f, 1f, 1f, 0.75f, 1f, 0.25f,
            0.5f, 0f, 0f, 0.25f, 0f, 0.75f
        )
        for (edge in boundaryEdges.sorted()) {
            require(edge in 0..5) { "Unknown hexagon edge $edge" }
            val next = (edge + 1) % 6
            addActor(ImageGetter.getLine(
                left + points[edge * 2] * hexWidth,
                bottom + points[edge * 2 + 1] * hexHeight,
                left + points[next * 2] * hexWidth,
                bottom + points[next * 2 + 1] * hexHeight,
                2.5f
            ).apply {
                name = "story-zone-edge:$edge"
                color = tint.cpy()
                touchable = Touchable.disabled
            })
        }
        if (title != null) {
            addActor(Label(title, BaseScreen.skin).apply {
                name = "story-zone-label:$zoneId"
                color = tint.cpy()
                touchable = Touchable.disabled
                setFontScale(0.65f)
                pack()
                setPosition((tile.width - width) / 2f, tile.height * 0.72f + overlapIndex * 14f)
            })
        }
    }
}
