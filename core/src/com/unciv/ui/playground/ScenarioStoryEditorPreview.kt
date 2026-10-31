package com.unciv.ui.playground

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.translations.tr
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.pure.domain.story.*
import com.unciv.ui.mapeditor.ScenarioHeroEditorPanel
import com.unciv.ui.mapeditor.ScenarioStoryEditorPanel
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.OnClickListener

/** Обходит реальные виджеты формы, включая вложенные редакторы ответов и последствий. */
private fun storyEditorActors(root: Actor): List<Actor> =
    listOf(root) + if (root is Group) root.children.toList().flatMap { storyEditorActors(it) } else emptyList()

private fun clickStoryEditorButton(root: Actor, caption: String) {
    val button = storyEditorActors(root).filterIsInstance<TextButton>()
        .single { it.text.toString() == caption.tr() }
    button.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
}

private fun storyEditorInput(root: Actor, value: String): TextField =
    storyEditorActors(root).filterIsInstance<TextField>().single { it.text == value }

/** Общий контейнер использует вертикальную прокрутку и проверяет ширину производственной формы. */
private fun showStoryEditorPanel(screen: BaseScreen, panel: Table) {
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
    check(panel.prefWidth <= 510f) { "Story editor exceeds available width: ${panel.prefWidth}" }
}

/** Проверяет навигацию, Apply, отказ и повторное открытие формы героя. */
fun openScenarioHeroEditorPreview(screen: BaseScreen) {
    checkStoryEditorNavigation()
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    val position = CityPosition(0f, 0f)
    map.scenarioHeroes.add(ScenarioHero("hero", "Original hero", "Warrior", "one", position))
    var changes = 0
    val panel = ScenarioHeroEditorPanel(map, ruleset, 500f) { changes++ }
    panel.select(position)
    storyEditorInput(panel, "Original hero").text = "Renamed hero"
    check(map.scenarioHeroes.single().name == "Original hero")
    clickStoryEditorButton(panel, "Apply hero")
    check(changes == 1 && map.scenarioHeroes.single().name == "Renamed hero")
    clickStoryEditorButton(panel, "Apply hero")
    check(changes == 1) { "Repeated hero Apply emitted a change" }
    storyEditorInput(panel, "hero").text = ""
    clickStoryEditorButton(panel, "Apply hero")
    check(changes == 1 && map.scenarioHeroes.single().id == "hero") {
        "Invalid hero draft changed the map"
    }
    panel.select(position)
    check(storyEditorInput(panel, "Renamed hero").text == map.scenarioHeroes.single().name)
    showStoryEditorPanel(screen, panel)
}

/** Проверяет ветвящийся диалог через форму, включая сохранение черновика между разделами. */
fun openScenarioStoryEditorPreview(screen: BaseScreen) {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.story = StoryScenario(dialogues = arrayListOf(StoryDialogue(
        id = "meeting", startNodeId = "question",
        nodes = arrayListOf(StoryDialogueNode(id = "question", speaker = "Gatekeeper",
            text = "Will you help the village?", choices = arrayListOf(
                StoryChoice(id = "accept", text = "I will help", effects = arrayListOf(
                    StoryEffect(StoryEffectKind.SetFlag, "accepted"),
                    StoryEffect(StoryEffectKind.GrantGold, amount = 17)
                )),
                StoryChoice(id = "leave", text = "I must leave")
            ))), aiChoices = linkedMapOf("question" to "leave")
    )))
    var changes = 0
    val panel = ScenarioStoryEditorPanel(map, ruleset, 500f, { changes++ }, {})
    clickStoryEditorButton(panel, "Dialogues")
    storyEditorInput(panel, "I will help").text = "I will help the village"
    check(map.story!!.dialogues.single().nodes.single().choices.first().text == "I will help")
    clickStoryEditorButton(panel, "Cinematics")
    clickStoryEditorButton(panel, "Dialogues")
    check(storyEditorInput(panel, "I will help the village").text.isNotBlank())
    clickStoryEditorButton(panel, "Apply story")
    check(changes == 1)
    val saved = map.story!!.deepCopy()
    check(saved.dialogues.single().nodes.single().choices.first().text == "I will help the village")
    check(saved.dialogues.single().nodes.single().choices.first().effects.size == 2)
    clickStoryEditorButton(panel, "Apply story")
    check(changes == 1) { "Repeated story Apply emitted a change" }
    storyEditorInput(panel, "question").text = ""
    clickStoryEditorButton(panel, "Apply story")
    check(changes == 1 && map.story == saved) { "Invalid story draft changed the map" }
    clickStoryEditorButton(panel, "Discard changes")
    check(storyEditorInput(panel, "question").text == "question")
    check(storyEditorInput(panel, "I will help the village").text.isNotBlank())
    showStoryEditorPanel(screen, panel)
}

/** Проверяет маршрутизацию кликов и восстановление инструментов на настоящем экране редактора. */
private fun checkStoryEditorNavigation() {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.scenarioHeroes.add(
        ScenarioHero(
            "hero",
            "Saved hero",
            "Warrior",
            "one",
            CityPosition(0f, 0f)
        )
    )
    map.story = StoryScenario(
        zones = arrayListOf(
            StoryZone("gate", "Gate", arrayListOf(CityPosition(1f, 0f)))
        )
    )
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(map)
    var recreated: com.unciv.ui.mapeditor.MapEditorScreen? = null
    try {
        editor.tabs.selectPage("Edit")
        check(editor.tabs.edit.selectPage("Heroes"))
        check(!editor.allowDragPainting && editor.tileClickHandler != null)
        editor.tileClickHandler!!.invoke(map[com.badlogic.gdx.math.Vector2.Zero])
        check(storyEditorInput(editor.stage.root, "Saved hero").text == "Saved hero")
        check(!editor.isDirty) { "Selecting a hero changed the map" }
        check(editor.tabs.edit.selectPage("Story"))
        check(!editor.allowDragPainting && editor.tileClickHandler != null)
        clickStoryEditorButton(editor.stage.root, "Gate (gate)")
        editor.tileClickHandler!!.invoke(map[com.badlogic.gdx.math.Vector2(2f, 0f)])
        check(map.story!!.zones.single().positions == listOf(CityPosition(1f, 0f)))
        check(!editor.isDirty) { "Selecting zone cells published the draft" }
        clickStoryEditorButton(editor.stage.root, "Apply story")
        check(editor.isDirty)
        check(
            map.story!!.zones.single().positions == listOf(
                CityPosition(1f, 0f),
                CityPosition(2f, 0f)
            )
        )
        editor.tabs.edit.selectPage("Terrain")
        check(editor.allowDragPainting && editor.tileClickHandler != null)
        editor.tabs.edit.selectPage("Story")
        check(!editor.allowDragPainting)
        val storyHandler = editor.tileClickHandler
        val savedStory = map.story!!.deepCopy()
        editor.tabs.selectPage("View")
        check(editor.allowDragPainting && editor.tileClickHandler != null)
        check(editor.tileClickHandler !== storyHandler) { "View retained the Story tile handler" }
        check(editor.tileClickHandler == editor.tabs.view::tileClickHandler) {
            "View did not install its tile inspection handler"
        }
        check(map.story == savedStory)
        editor.tabs.selectPage("Edit")
        check(editor.tabs.edit.currentSubTab() == "Story" && !editor.allowDragPainting)
        val restored = editor.recreate() as com.unciv.ui.mapeditor.MapEditorScreen
        recreated = restored
        check(restored.tabs.edit.currentSubTab() == "Story")
        check(!restored.allowDragPainting && restored.tileClickHandler != null)
        check(restored.isDirty && restored.tileMap.story == savedStory)
        check(restored.tabs.edit.selectPage("Heroes"))
        restored.tileClickHandler!!.invoke(map[com.badlogic.gdx.math.Vector2.Zero])
        check(storyEditorInput(restored.stage.root, "Saved hero").text == "Saved hero")
    } finally {
        recreated?.dispose()
        editor.dispose()
    }
}
