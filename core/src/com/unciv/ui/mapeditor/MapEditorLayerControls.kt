package com.unciv.ui.mapeditor

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.toLabel

/** Закреплённые инструменты Generate с естественной высотой легенды. */
class MapEditorLayerControls(private val editor: MapEditorScreen, generation: MapEditorGenerateTab) : Table(BaseScreen.skin) {
    private val modes = MapEditorTerrainLayer.values()
    private val selector = SelectBox<String>(skin).apply {
        name = "map-editor-view-layer"
        setItems(*modes.map { it.caption }.toTypedArray())
        selected = editor.terrainLayer.caption
    }
    private val legend = Label(legendText(editor.terrainLayer), skin).apply {
        name = "map-editor-layer-legend"
        wrap = true
        setFontScale(0.65f)
    }

    init {
        name = "map-editor-layer-controls"
        top()
        defaults().pad(6f)
        add("View layer".toLabel()).left()
        add(selector).growX().row()
        add(generation.regenerateButton).colspan(2).growX().row()
        add(generation.resetDefaultsButton).colspan(2).growX().row()
        add(legend).colspan(2).width((editor.getToolsWidth() - 36f).coerceAtLeast(220f))
            .fillX().left().row()
        selector.onChange {
            editor.terrainLayer = modes[selector.selectedIndex]
            legend.setText(legendText(editor.terrainLayer))
            invalidateHierarchy()
        }
    }
    private fun legendText(layer: MapEditorTerrainLayer): String = when (layer) {
        MapEditorTerrainLayer.GAME -> "Game textures"
        MapEditorTerrainLayer.BASE -> "Base terrain colours from ruleset"
        MapEditorTerrainLayer.TEMPERATURE -> "Blue: cold (-1); red: warm (+1). Grey: no data."
        MapEditorTerrainLayer.HUMIDITY -> "Blue: dry (0); red: wet (1). Grey: no data."
        MapEditorTerrainLayer.ELEVATION -> "Beige: flat; brown: hills; grey: mountains; blue: water."
        MapEditorTerrainLayer.VEGETATION -> "Green: forest; dark green: jungle; beige: none; blue: water."
    }
    override fun act(delta: Float) {
        super.act(delta)
        if (selector.selected != editor.terrainLayer.caption) {
            selector.selected = editor.terrainLayer.caption
            legend.setText(legendText(editor.terrainLayer))
            invalidateHierarchy()
        }
    }
}
