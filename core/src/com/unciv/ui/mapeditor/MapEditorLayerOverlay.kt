package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.SolidHexagonDrawable

/** Диагностическая заливка под выделением, с исходными размерами drawable до расчёта геометрии. */
class MapEditorLayerOverlay(
    private val tile: TileGroup,
    private val selectedLayer: () -> MapEditorTerrainLayer
) : Image(ImageGetter.getDrawable(tile.tileSetStrings.hexagon).let {
    SolidHexagonDrawable(it.minWidth, it.minHeight)
}) {
    init {
        // Image(drawable) задаёт ширину и высоту. Пустой Image давал 0/0 и NaN в помощнике размера.
        name = "map-editor-layer-overlay"
        touchable = Touchable.disabled
        tile.sizeAndPlaceOverHex(this)
        require(width.isFinite() && height.isFinite() && width > 0f && height > 0f) {
            "Invalid terrain layer overlay geometry"
        }
    }
    fun currentColor(): Color? = selectedLayer().colorFor(tile.tileInfo)
    override fun draw(batch: Batch, parentAlpha: Float) {
        val selectedColor = currentColor() ?: return
        color.set(selectedColor)
        super.draw(batch, parentAlpha)
    }
}
