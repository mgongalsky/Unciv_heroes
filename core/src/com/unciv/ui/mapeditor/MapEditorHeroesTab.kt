package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager

/** Edit → Heroes: одиночный выбор размещения вместо рисования кистью. */
class MapEditorHeroesTab(private val editor: MapEditorScreen, private val editTab: MapEditorEditTab) :
    Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    private var selectedPosition: CityPosition? = null
    init { name = "scenario-heroes-tab" }
    override fun isDisabled() = false

    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        top()
        editTab.setCityEditing(true)
        editor.hideSelection()
        editor.mapHolder.refreshScenarioHeroPreviews()
        val panel =
                ScenarioHeroEditorPanel(editor.tileMap, editor.ruleset, editor.getToolsWidth()) {
                    editor.isDirty = true
                    editor.mapHolder.refreshScenarioHeroPreviews()
                }
        add(panel).growX().top()
        selectedPosition?.let { panel.select(it) }
        editor.tileClickHandler = { tile ->
            editor.hideSelection()
            editor.highlightTile(tile, Color.SKY)
            val position = CityPosition(tile.position.x, tile.position.y)
            selectedPosition = position
            panel.select(position)
        }
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(false)
        editor.hideSelection()
    }
}
