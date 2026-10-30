package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager

class MapEditorCitiesTab(private val editor: MapEditorScreen) : Table(BaseScreen.skin), TabbedPager.IPageExtensions {
    private var panel: ScenarioCityEditorPanel? = null

    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        editor.allowDragPainting = false
        panel = ScenarioCityEditorPanel(editor.tileMap, editor.ruleset, editor.getToolsWidth()) {
            editor.isDirty = true
            editor.mapHolder.refreshScenarioCityPreviews()
        }
        add(panel).growX().top()
        editor.selectedScenarioCity?.let { panel?.select(it) }
        editor.tileClickHandler = { tile ->
            editor.hideSelection()
            editor.highlightTile(tile, Color.SKY)
            val position = CityPosition(tile.position.x, tile.position.y)
            editor.selectedScenarioCity = position
            panel?.select(position)
        }
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editor.tileClickHandler = null
        editor.allowDragPainting = true
        editor.hideSelection()
    }
}
