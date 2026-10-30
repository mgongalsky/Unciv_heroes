package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.*
import com.unciv.ui.mapeditor.ScenarioCityEditorPanel
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.OnClickListener

/** Shared deterministic editor form, without a game world or runtime city objects. */
fun openScenarioCityEditorPreview(screen: BaseScreen) {
    val ruleset = RulesetCache.getVanillaRuleset()
    val map = TileMap(3, ruleset)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("first", "First player"))
    val original = ScenarioCity(id = "riverford", name = "Riverford",
        owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "first", population = 3, foodStored = 17,
        isCapital = true, isOriginalCapital = true, buildings = arrayListOf("Granary"),
        garrison = arrayListOf(ScenarioCityTroop("Spearman", 5)))
    map.scenarioCities.add(original)
    var changes = 0
    val panel = ScenarioCityEditorPanel(map, ruleset, 500f) { changes++ }
    panel.select(CityPosition())

    // Exercise the actual form handler before restoring the deterministic preview.
    fun applyName(value: String) {
        panel.findActor<TextField>("scenario-city-name").text = value
        val button = panel.findActor<com.badlogic.gdx.scenes.scene2d.Actor>("scenario-city-apply")
        button.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
    }
    applyName("Updated Riverford")
    check(map.scenarioCities.single().name == "Updated Riverford") { "Apply did not update the city" }
    check(changes == 1) { "Apply must emit exactly one change" }
    check(original.name == "Riverford") { "Editing mutated the previous description" }
    applyName("Updated Riverford")
    check(map.scenarioCities.size == 1 && changes == 1) { "Repeated Apply duplicated a city or change" }
    applyName("Riverford")
    check(map.scenarioCities.single() == original) { "Editing lost city properties" }

    val scroll = AutoScrollPane(panel).apply {
        setScrollingDisabled(true, false)
        setOverscroll(false, false)
        setFadeScrollBars(false)
    }
    val container = Table().apply {
        setFillParent(true)
        add(scroll).width(500f).height(screen.stage.height - 40f)
    }
    screen.stage.addActor(container)
    container.validate()
    scroll.validate()
    check(panel.prefWidth <= 510f) { "City editor form exceeds its available width: ${panel.prefWidth}" }
}
