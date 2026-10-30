package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager

class MapEditorCitiesTab(
    private val editor: MapEditorScreen,
    private val editTab: MapEditorEditTab
) : Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    init { name = "scenario-cities-tab" }
    private var panel: ScenarioCityEditorPanel? = null

    override fun isDisabled() = false

    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        editTab.setCityEditing(true)
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
        editTab.setCityEditing(false)
        editor.hideSelection()
    }
}
