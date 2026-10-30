package com.unciv.ui.playground

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.ui.mapeditor.MapEditorScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.OnClickListener

/** Exercises nested tool activation and recreation in the real editor. */
fun createScenarioCityEditorTabsPreview(): BaseScreen {
    val editor = MapEditorScreen(TileMap(3, RulesetCache.getVanillaRuleset()))
    check(editor.tabs.getPageIndex("Cities") == -1) { "Cities must be inside Edit" }
    editor.tabs.selectPage("Edit")
    check(editor.tabs.edit.selectPage("Cities")) { "Edit must contain Cities" }
    check(!editor.allowDragPainting)
    editor.tileClickHandler!!.invoke(editor.tileMap[Vector2.Zero])
    val form = editor.stage.root
    check(form.findActor<CheckBox>("scenario-city-capital") != null)
    form.findActor<TextField>("scenario-city-name").text = "Saved town"
    val apply = form.findActor<Actor>("scenario-city-apply")
    apply.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
    check(editor.tileMap.scenarioCities.single().name == "Saved town")
    editor.tabs.edit.selectPage("Terrain")
    check(editor.allowDragPainting && editor.tileClickHandler != null)
    editor.tabs.edit.selectPage("Cities")
    check(!editor.allowDragPainting)
    editor.tabs.selectPage("View")
    check(editor.allowDragPainting) { "Leaving Cities must restore normal map interaction" }
    editor.tabs.selectPage("Edit")
    check(editor.tabs.edit.currentSubTab() == "Cities" && !editor.allowDragPainting)
    val recreated = editor.recreate() as MapEditorScreen
    editor.dispose()
    check(recreated.tabs.edit.currentSubTab() == "Cities" && !recreated.allowDragPainting)
    check(recreated.isDirty && recreated.tileMap.scenarioCities.single().name == "Saved town")
    check(recreated.stage.root.findActor<TextField>("scenario-city-name").text == "Saved town")
    return recreated
}
