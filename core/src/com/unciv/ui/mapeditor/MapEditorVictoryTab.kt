package com.unciv.ui.mapeditor

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager

/** Edit → Victory: редактирование условий сценария без рисования кистью по карте. */
class MapEditorVictoryTab(
    private val editor: MapEditorScreen,
    private val editTab: MapEditorEditTab
) : Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    init { name = "scenario-victory-tab" }
    override fun isDisabled() = false

    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        editTab.setCityEditing(true)
        editor.hideSelection()
        add(ScenarioVictoryEditorPanel(editor.tileMap, editor.ruleset, editor.getToolsWidth()) {
            editor.isDirty = true
        }).growX().top()
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(false)
    }
}
