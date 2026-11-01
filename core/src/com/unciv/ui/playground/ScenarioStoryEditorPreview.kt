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

/** Проверяет доступность экранной кнопки через Stage; отдельные формы проверяют команды без экрана. */
private fun clickStoryEditorButton(root: Actor, caption: String) {
    val button = storyEditorActors(root).filterIsInstance<TextButton>()
        .single { it.text.toString() == caption.tr() }
    val stage = button.stage
    if (stage == null) {
        button.listeners.filterIsInstance<OnClickListener>().single().function(null, 0f, 0f)
        return
    }
    fun renderLayout() {
        repeat(3) {
            storyEditorActors(stage.root)
                .filterIsInstance<com.badlogic.gdx.scenes.scene2d.utils.Layout>()
                .forEach { it.validate() }
        }
        // ScrollPane размещает содержимое по visualScrollY во время draw, а не validate.
        stage.viewport.apply()
        stage.draw()
    }

    fun geometry(): String = "stage=${stage.width}x${stage.height}\n" +
            generateSequence<Actor>(button) { it.parent }.joinToString("\n") { actor ->
                val origin = actor.localToStageCoordinates(com.badlogic.gdx.math.Vector2())
                val scroll = actor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
                "${actor.javaClass.simpleName} name=${actor.name} origin=$origin size=${actor.width}x${actor.height}" +
                        if (scroll == null) "" else " scrollY=${scroll.scrollY} visualY=${scroll.visualScrollY} maxY=${scroll.maxY} disabledY=${scroll.isScrollingDisabledY}"
            }
    renderLayout()
    // Раскрываем кнопку во внутренних контейнерах до прокрутки внешних.
    for (scroll in generateSequence(button.parent) { it.parent }
        .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()) {
        val content = scroll.actor ?: continue
        val position = button.localToActorCoordinates(content, com.badlogic.gdx.math.Vector2())
        scroll.scrollTo(position.x, position.y, button.width, button.height)
        scroll.updateVisualScroll()
        renderLayout()
    }
    val center = button.localToStageCoordinates(
        com.badlogic.gdx.math.Vector2(button.width / 2f, button.height / 2f)
    )
    check(center.x in 0f..stage.width && center.y in 0f..stage.height) {
        "Button '$caption' is outside the stage: $center\n${geometry()}"
    }
    val hit = stage.hit(center.x, center.y, true)
    check(hit != null && generateSequence(hit) { it.parent }.any { it === button }) {
        "Button '$caption' cannot receive a click: hit=$hit, position=$center\n${geometry()}"
    }
    val screenPosition = stage.stageToScreenCoordinates(center.cpy())
    stage.touchDown(
        screenPosition.x.toInt(),
        screenPosition.y.toInt(),
        0,
        com.badlogic.gdx.Input.Buttons.LEFT
    )
    stage.touchUp(
        screenPosition.x.toInt(),
        screenPosition.y.toInt(),
        0,
        com.badlogic.gdx.Input.Buttons.LEFT
    )
    renderLayout()
}

private fun storyEditorInput(root: Actor, value: String): TextField =
    storyEditorActors(root).filterIsInstance<TextField>().single { it.text == value }

/** Общий контейнер проверяет ширину формы; экранные переходы проверяются после установки экрана в стек. */
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
    if (panel is ScenarioStoryEditorPanel) {
        screen.stage.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.run {
            checkCinematicEditorPreview(screen)
            checkStorySaveAndPlay(screen)
        })
    }
}

/** Проверяет жизненный цикл спрайта, создание примера, навигацию и команды формы героя. */
fun openScenarioHeroEditorPreview(screen: BaseScreen) {
    checkHeroPreviewLifecycle()
    checkStoryEditorNavigation()
    checkStoryExampleCreation()
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    val position = CityPosition(0f, 0f)
    map.scenarioHeroes.add(ScenarioHero("hero", "Original hero", "Warrior", "one", position))
    var changes = 0
    val panel = ScenarioHeroEditorPanel(map, ruleset, 500f) { changes++ }
    panel.select(position)
    showStoryEditorPanel(screen, panel)
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
}

/** Проверяет редактирование и предпросмотр через экранный ввод, включая изоляцию последствий. */
fun openScenarioStoryEditorPreview(screen: BaseScreen) {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.story = StoryScenario(
        dialogues = arrayListOf(
            StoryDialogue(
                id = "meeting", startNodeId = "question",
                nodes = arrayListOf(
                    StoryDialogueNode(
                        id = "question", speaker = "Gatekeeper",
                        text = "Will you help the village?", choices = arrayListOf(
                            StoryChoice(
                                id = "accept", text = "I will help", effects = arrayListOf(
                                    StoryEffect(StoryEffectKind.SetFlag, "accepted"),
                                    StoryEffect(StoryEffectKind.GrantGold, amount = 17)
                                )
                            ),
                            StoryChoice(id = "leave", text = "I must leave")
                        )
                    )
                ), aiChoices = linkedMapOf("question" to "leave")
            )
        )
    )
    var changes = 0
    val panel = ScenarioStoryEditorPanel(map, ruleset, 500f, { changes++ }, {})
    panel.previewScreen = screen
    showStoryEditorPanel(screen, panel)
    clickStoryEditorButton(panel, "Dialogues")
    storyEditorInput(panel, "I will help").text = "I will help the village"
    check(map.story!!.dialogues.single().nodes.single().choices.first().text == "I will help")
    clickStoryEditorButton(panel, "Cinematics")
    check(storyEditorActors(panel).any { it.name == "scenario-story-empty-section" })
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
    clickStoryEditorButton(panel, "Preview dialogue: meeting")
    check(storyEditorActors(screen.stage.root).any { it.name == "story-text" })
    clickStoryEditorButton(screen.stage.root, "I will help the village")
    clickStoryEditorButton(screen.stage.root, "Continue")
    clickStoryEditorButton(screen.stage.root, "Continue")
    check(
        storyEditorActors(screen.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
            .any { it.text.toString() == "Preview complete".tr() })
    clickStoryEditorButton(screen.stage.root, "Close")
    check(changes == 1 && map.story == saved) { "Preview published changes to the map" }
    clickStoryEditorButton(panel, "Preview dialogue: meeting")
    clickStoryEditorButton(screen.stage.root, "Close preview")
    check(storyEditorActors(screen.stage.root).none { it is com.unciv.ui.story.StoryDialoguePopup })
    check(changes == 1 && map.story == saved) { "Closing preview changed the map" }
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

/** Создание примера обновляет карту и открытую форму, не оставляя обработчики прежнего черновика. */
private fun checkStoryExampleCreation() {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val original = TileMap(3, ruleset, false)
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(original)
    try {
        editor.tabs.selectPage("Edit")
        editor.tabs.edit.selectPage("Story")
        check(!editor.isDirty)
        clickStoryEditorButton(editor.stage.root, "Create story example map")
        check(editor.tileMap !== original) { "Example did not replace the editor map" }
        check(editor.isDirty) { "Example map was not marked for saving" }
        check(editor.tileMap.scenarioHeroes.single().id == "guide")
        check(editor.tileMap.scenarioCities.single().id == "gate")
        check(editor.tabs.edit.currentSubTab() == "Story" && !editor.allowDragPainting)
        clickStoryEditorButton(editor.stage.root, "Dialogues")
        check(storyEditorInput(editor.stage.root, "parley").text == "parley") {
            "Story form still displays the previous map"
        }
        clickStoryEditorButton(editor.stage.root, "Apply story")
        check(editor.tileMap.story!!.dialogues.any { it.id == "parley" })
        check(original.story == null && original.scenarioHeroes.isEmpty())
    } finally {
        editor.dispose()
    }
}

/** Превью героя следует за Apply/Delete и пересозданием экрана, не создавая игровых юнитов. */
private fun checkHeroPreviewLifecycle() {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one", "First player"))
    map.scenarioHeroes.add(
        ScenarioHero(
            "hero",
            "Original hero",
            "Warrior",
            "one",
            CityPosition(0f, 0f)
        )
    )
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(map)
    var recreated: com.unciv.ui.mapeditor.MapEditorScreen? = null
    fun assertPreview(screen: com.unciv.ui.mapeditor.MapEditorScreen, expectedName: String?) {
        val tile = screen.tileMap[com.badlogic.gdx.math.Vector2.Zero]
        check(tile.militaryUnit == null) { "Editor preview created a military unit" }
        for (group in screen.mapHolder.tileGroups.getValue(tile)) {
            val previews = group.children.toList().filter { it.name == "scenario-hero-preview" }
            if (expectedName == null) {
                check(previews.isEmpty()) { "Deleted hero still has a preview" }
                continue
            }
            check(previews.size == 1) { "Expected one hero preview, found ${previews.size}" }
            val actors = storyEditorActors(previews.single())
            check(actors.any { it.name == "scenario-hero-sprite" && it.width > 0f && it.height > 0f })
            check(
                actors.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                    .any { it.name == "scenario-hero-name" && it.text.toString() == expectedName })
            check(previews.single().touchable == com.badlogic.gdx.scenes.scene2d.Touchable.disabled)
        }
    }
    try {
        assertPreview(editor, "Original hero")
        editor.tabs.selectPage("Edit")
        editor.tabs.edit.selectPage("Heroes")
        editor.tileClickHandler!!.invoke(map[com.badlogic.gdx.math.Vector2.Zero])
        storyEditorInput(editor.stage.root, "Original hero").text = "Renamed hero"
        clickStoryEditorButton(editor.stage.root, "Apply hero")
        assertPreview(editor, "Renamed hero")
        editor.mapHolder.updateTileGroups()
        assertPreview(editor, "Renamed hero")
        val restored = editor.recreate() as com.unciv.ui.mapeditor.MapEditorScreen
        recreated = restored
        assertPreview(restored, "Renamed hero")
        restored.tabs.selectPage("Edit")
        restored.tabs.edit.selectPage("Heroes")
        restored.tileClickHandler!!.invoke(restored.tileMap[com.badlogic.gdx.math.Vector2.Zero])
        clickStoryEditorButton(restored.stage.root, "Delete hero")
        check(restored.tileMap.scenarioHeroes.isEmpty())
        assertPreview(restored, null)
    } finally {
        recreated?.dispose()
        editor.dispose()
    }
}

/** Проверяет кнопку видеосцены, текстовую замену и возврат в редактор через настоящий стек экранов. */
private fun checkCinematicEditorPreview(host: BaseScreen) {
    check(host.game.screen === host)
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    map.story = StoryScenario(
        cinematics = arrayListOf(
            StoryCinematic("arrival", "", "Preview arrival text", true)
        )
    )
    val saved = map.story!!.deepCopy()
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(map)
    host.game.pushScreen(editor)
    try {
        editor.tabs.selectPage("Edit")
        editor.tabs.edit.selectPage("Story")
        clickStoryEditorButton(editor.stage.root, "Cinematics")
        // Открытие меняет экран; послекликовая отрисовка редактора безопасна, он остаётся в стеке.
        clickStoryEditorButton(editor.stage.root, "Preview cinematic: arrival")
        val cinematic = host.game.screen as? com.unciv.ui.story.StoryCinematicScreen
            ?: error("Cinematic preview did not open its production screen")
        cinematic.render(0f)
        check(
            storyEditorActors(cinematic.stage.root)
                .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                .any { it.name == "story-text" && it.text.toString() == "Preview arrival text" })
        val button = storyEditorActors(cinematic.stage.root).filterIsInstance<TextButton>()
            .single { it.name == "story-choice:continue" }
        val center = button.localToStageCoordinates(
            com.badlogic.gdx.math.Vector2(
                button.width / 2f,
                button.height / 2f
            )
        )
        val hit = cinematic.stage.hit(center.x, center.y, true)
        check(hit != null && generateSequence(hit) { it.parent }.any { it === button })
        val point = cinematic.stage.stageToScreenCoordinates(center)
        cinematic.stage.touchDown(
            point.x.toInt(),
            point.y.toInt(),
            0,
            com.badlogic.gdx.Input.Buttons.LEFT
        )
        // Подтверждение уничтожает экран ролика: после touchUp больше не обращаемся к его Stage.
        cinematic.stage.touchUp(
            point.x.toInt(),
            point.y.toInt(),
            0,
            com.badlogic.gdx.Input.Buttons.LEFT
        )
        check(host.game.screen === editor) { "Cinematic preview did not return to the editor" }
        editor.stage.act(0f)
        check(
            storyEditorActors(editor.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                .any { it.text.toString() == "Preview complete".tr() })
        clickStoryEditorButton(editor.stage.root, "Close")
        check(map.story == saved && !editor.isDirty) { "Cinematic preview changed the map" }
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}

/** Проверяет асинхронную запись и передачу именно сохранённого сценария в штатный экран запуска. */
private fun checkStorySaveAndPlay(host: BaseScreen) {
    check(host.game.screen === host)
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = com.unciv.logic.story.StoryDemoScenario.create(ruleset)
    val filename = "story-ui-check-${java.util.UUID.randomUUID()}.json"
    map.mapParameters.name = filename
    val file = com.badlogic.gdx.Gdx.files.local("${com.unciv.logic.MapSaver.mapsFolder}/$filename")
    check(!file.exists())
    val savedStory = map.story!!.deepCopy()
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(map)
    host.game.pushScreen(editor)
    editor.isDirty = true
    fun cleanup() {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
        if (file.exists()) check(file.delete()) { "Could not remove the UI test map" }
    }
    try {
        editor.tabs.selectPage("Save")
        clickStoryEditorButton(editor.stage.root, "Save and play")
    } catch (failure: Throwable) {
        cleanup()
        throw failure
    }
    val deadline = System.nanoTime() + 10_000_000_000L
    com.badlogic.gdx.Gdx.app.postRunnable(object : Runnable {
        override fun run() {
            val setupScreen = host.game.screen as? com.unciv.ui.newgamescreen.NewGameScreen
            if (setupScreen == null && System.nanoTime() < deadline) {
                com.badlogic.gdx.Gdx.app.postRunnable(this)
                return
            }
            try {
                check(setupScreen != null) { "Save and play did not open game setup" }
                val setup = setupScreen.gameSetupInfo
                check(setup.mapFile == file && setup.mapParameters.name == filename)
                val restored = com.unciv.logic.MapSaver.loadMap(file)
                check(restored.story == savedStory)
                check(restored.scenarioHeroes == map.scenarioHeroes)
                check(setup.gameParameters.players.map { it.scenarioSlotId } == map.scenarioPlayerSlots.map { it.id })
                check(setup.gameParameters.players.first().playerType == com.unciv.logic.civilization.PlayerType.Human)
                check(!editor.isDirty) { "Unchanged saved map still marked dirty" }
            } finally {
                cleanup()
            }
        }
    })
}
