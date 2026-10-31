package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.ScenarioVictoryMode
import com.unciv.ui.mapeditor.ScenarioVictoryEditorPanel
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.OnClickListener

/** Воспроизводимая форма Victory с реальным Apply и двумя целями, без запуска партии. */
fun openScenarioVictoryEditorPreview(screen: BaseScreen) {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("two", "Second player"))
    map.scenarioCities.add(ScenarioCity(id = "fort", name = "North fort",
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
    map.scenarioCities.add(ScenarioCity(id = "port", name = "River port", position = CityPosition(2f, 0f),
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
    var changes = 0
    val panel = ScenarioVictoryEditorPanel(map, ruleset, 500f) { changes++ }
    panel.findActor<SelectBox<ScenarioVictoryMode>>("scenario-victory-mode-CaptureCity").selected = ScenarioVictoryMode.Required
    for (city in listOf("fort", "port")) {
        panel.findActor<CheckBox>("scenario-victory-city-$city").isChecked = true
        panel.findActor<CheckBox>("scenario-victory-slot-$city-one").isChecked = true
    }
    check(map.scenarioVictorySettings == null) { "Editing changed the map before Apply" }
    val apply = panel.findActor<Actor>("scenario-victory-apply")
    fun applyDraft() = apply.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
    applyDraft()
    check(changes == 1)
    check(map.scenarioVictorySettings!!.captureObjectives.map { it.cityId } == listOf("fort", "port"))
    check(map.scenarioVictorySettings!!.captureObjectives.all { it.eligibleSlotIds == listOf("one") })
    applyDraft()
    check(changes == 1) { "Repeated Apply emitted a duplicate change" }
    panel.findActor<CheckBox>("scenario-victory-slot-fort-one").isChecked = false
    applyDraft()
    check(changes == 1 && map.scenarioVictorySettings!!.captureObjectives.first().eligibleSlotIds == listOf("one")) {
        "Invalid draft partially changed the map"
    }
    panel.refresh()
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
    check(panel.prefWidth <= 510f) { "Victory form exceeds available width: ${panel.prefWidth}" }
}
