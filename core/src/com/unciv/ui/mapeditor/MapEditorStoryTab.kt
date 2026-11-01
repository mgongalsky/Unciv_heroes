package com.unciv.ui.mapeditor

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.domain.city.CityPosition
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toTextButton

/** Edit → Story: черновик сценария и набор клеток выбранной зоны. */
class MapEditorStoryTab(private val editor: MapEditorScreen, private val editTab: MapEditorEditTab) :
    Table(BaseScreen.skin), TabbedPager.IPageExtensions, IMapEditorEditSubTabs {
    init { name = "scenario-story-tab" }
    override fun isDisabled() = false

    /** При новом входе читается карта; неприменённый черновик предыдущего входа отбрасывается. */
    override fun activated(index: Int, caption: String, pager: TabbedPager) {
        clear()
        top()
        editTab.setCityEditing(true)
        editor.hideSelection()
        add("Create story example map".toTextButton().apply {
            name = "scenario-story-create-example"
        }.onClick {
            editor.askIfDirty(
                "Do you want to leave without saving the recent changes?",
                "Create story example map"
            ) {
                val parameters = com.unciv.models.metadata.GameParameters()
                val ruleset = com.unciv.models.ruleset.RulesetCache.getComplexRuleset(parameters)
                val example = com.unciv.logic.story.StoryDemoScenario.create(ruleset)
                example.mapParameters.baseRuleset = parameters.baseRuleset
                example.mapParameters.mods = LinkedHashSet(parameters.mods)
                editor.tabs.selectPage(-1)
                editor.selectedEditSubTab = "Story"
                editor.loadMap(example, ruleset, selectPage = 2)
                editor.isDirty = true
            }
        }).growX().pad(4f).row()
        val panel = ScenarioStoryEditorPanel(
            editor.tileMap, editor.ruleset, editor.getToolsWidth(),
            changed = { editor.isDirty = true },
            highlight = { positions ->
                editor.hideSelection()
                val selected = positions.toSet()
                for (tile in editor.tileMap.values) {
                    if (CityPosition(tile.position.x, tile.position.y) in selected)
                        editor.highlightTile(tile, Color.SKY)
                }
            })
        panel.previewScreen = editor
        add(panel).growX().top()
        editor.tileClickHandler =
                { tile -> panel.selectTile(CityPosition(tile.position.x, tile.position.y)) }
    }

    override fun deactivated(index: Int, caption: String, pager: TabbedPager) {
        editTab.setCityEditing(false)
        editor.hideSelection()
    }
}
