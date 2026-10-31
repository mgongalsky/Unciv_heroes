package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager

/** Edit → Story: черновик сценария и набор клеток выбранной зоны. */
class MapEditorStoryTab(private val editor: MapEditorScreen, private val editTab: MapEditorEditTab) :
    Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    init { name = "scenario-story-tab" }
    override fun isDisabled() = false

    /** При новом входе читается карта; неприменённый черновик предыдущего входа отбрасывается. */
    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        editTab.setCityEditing(true)
        editor.hideSelection()
        val panel = ScenarioStoryEditorPanel(editor.tileMap, editor.ruleset, editor.getToolsWidth(),
            changed = { editor.isDirty = true },
            highlight = { positions ->
                editor.hideSelection()
                val selected = positions.toSet()
                for (tile in editor.tileMap.values) {
                    if (CityPosition(tile.position.x, tile.position.y) in selected)
                        editor.highlightTile(tile, Color.SKY)
                }
            })
        add(panel).growX().top()
        editor.tileClickHandler = { tile -> panel.selectTile(CityPosition(tile.position.x, tile.position.y)) }
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(false)
        editor.hideSelection()
    }
}
