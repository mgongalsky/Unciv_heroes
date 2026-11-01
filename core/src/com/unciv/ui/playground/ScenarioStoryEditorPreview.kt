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

/** Пустое значение используется проверкой неверного ID узла; остальные поля ищутся по уникальному тексту. */
private fun storyEditorInput(root: Actor, value: String): TextField =
        storyEditorActors(root).filterIsInstance<TextField>().single {
            it.text == value && (value.isNotEmpty() || it.name == "story-field:Node ID")
        }

/** Проверяет ширину формы и откладывает проверки графа до появления владельца в стеке. */
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
    if (panel is ScenarioStoryEditorPanel) screen.stage.addAction(
        com.badlogic.gdx.scenes.scene2d.actions.Actions.run {
            checkStoryGraphInteraction(screen, ::clickStoryEditorButton)
            checkStoryGraphGestures(screen, ::clickStoryEditorButton)
            checkStoryDialogueInline(screen, ::clickStoryEditorButton)
        }
    )
}

/** Проверяет контуры зон, спрайт героя, навигацию и команды формы через настоящий ввод. */
fun openScenarioHeroEditorPreview(screen: BaseScreen) {
    checkStoryZoneContours(::clickStoryEditorButton)
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
    check(changes == 1)
    storyEditorInput(panel, "hero").text = ""
    clickStoryEditorButton(panel, "Apply hero")
    check(changes == 1 && map.scenarioHeroes.single().id == "hero")
    panel.select(position)
    check(storyEditorInput(panel, "Renamed hero").text == map.scenarioHeroes.single().name)
}

/** Проверяет граф через Stage, изоляцию двух уровней черновика и производственный предпросмотр. */
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
                        id = "question",
                        speaker = "Gatekeeper",
                        text = "Will you help the village?",
                        choices = arrayListOf(
                            StoryChoice(
                                "accept", "I will help", effects = arrayListOf(
                                    StoryEffect(StoryEffectKind.SetFlag, "accepted"),
                                    StoryEffect(StoryEffectKind.GrantGold, amount = 17)
                                )
                            ),
                            StoryChoice("leave", "I must leave")
                        )
                    )
                ), aiChoices = linkedMapOf("question" to "leave")
            )
        )
    )
    val original = map.story!!.deepCopy()
    var changes = 0
    val panel = ScenarioStoryEditorPanel(map, ruleset, 500f, { changes++ }, {})
    panel.previewScreen = screen
    showStoryEditorPanel(screen, panel)
    clickStoryEditorButton(panel, "Dialogues")
    check(storyEditorActors(panel).none { it is TextField }) { "Dialogue list still contains inline node forms" }
    screen.stage.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.run {
        check(screen.game.screen === screen)
        fun graph() = screen.game.screen as? com.unciv.ui.mapeditor.StoryDialogueGraphScreen
            ?: error("Full-screen dialogue graph did not open")

        // Кнопки возврата уничтожают свой Stage; после touchUp к нему больше не обращаемся.
        fun closeGraph(caption: String) {
            val current = graph()
            current.render(0f)
            val button = storyEditorActors(current.stage.root).filterIsInstance<TextButton>()
                .single { it.text.toString() == caption.tr() }
            val center = button.localToStageCoordinates(
                com.badlogic.gdx.math.Vector2(
                    button.width / 2f,
                    button.height / 2f
                )
            )
            val hit = current.stage.hit(center.x, center.y, true)
            check(hit != null && generateSequence(hit) { it.parent }.any { it === button })
            val point = current.stage.stageToScreenCoordinates(center)
            current.stage.touchDown(
                point.x.toInt(),
                point.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            current.stage.touchUp(
                point.x.toInt(),
                point.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            check(screen.game.screen === screen) { "Graph did not return to its owner after $caption" }
        }
        clickStoryEditorButton(panel, "Edit dialogue: meeting")
        val first = graph()
        first.render(0f)
        check(storyEditorActors(first.stage.root).any { it.name == "story-graph-node:question" })
        check(storyEditorActors(first.stage.root).any { it.name == "story-graph-end" })
        check(storyEditorActors(first.stage.root).count { it.name?.startsWith("story-graph-edge:") == true } == 2)
        storyEditorInput(first.stage.root, "I will help").text = "I will help the village"
        storyEditorInput(first.stage.root, "question").text = "renamed"
        clickStoryEditorButton(first.stage.root, "Update node")
        check(storyEditorActors(first.stage.root).any { it.name == "story-graph-node:renamed" })
        check(map.story == original && changes == 0)
        storyEditorInput(first.stage.root, "renamed").text = ""
        clickStoryEditorButton(first.stage.root, "Update node")
        check(
            storyEditorActors(first.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                .single { it.name == "story-graph-error" }.text.isNotEmpty()
        )
        check(storyEditorActors(first.stage.root).any { it.name == "story-graph-node:renamed" })
        storyEditorInput(first.stage.root, "").text = "renamed"
        clickStoryEditorButton(first.stage.root, "Update node")
        clickStoryEditorButton(first.stage.root, "Add node")
        check(storyEditorActors(first.stage.root).any { it.name == "story-graph-node:node1" })
        clickStoryEditorButton(first.stage.root, "Delete node")
        check(storyEditorActors(first.stage.root).none { it.name == "story-graph-node:node1" })
        clickStoryEditorButton(first.stage.root, "−")
        clickStoryEditorButton(first.stage.root, "+")
        closeGraph("Apply dialogue")
        check(map.story == original && changes == 0) { "Graph Apply bypassed Apply story" }
        clickStoryEditorButton(panel, "Cinematics")
        check(storyEditorActors(panel).any { it.name == "scenario-story-empty-section" })
        clickStoryEditorButton(panel, "Dialogues")
        clickStoryEditorButton(panel, "Apply story")
        check(changes == 1)
        val saved = map.story!!.deepCopy()
        val dialogue = saved.dialogues.single()
        check(dialogue.startNodeId == "renamed" && dialogue.aiChoices["renamed"] == "leave")
        check(dialogue.nodes.single().choices.first().text == "I will help the village")
        check(dialogue.nodes.single().choices.first().effects.size == 2)
        clickStoryEditorButton(panel, "Apply story")
        check(changes == 1)
        clickStoryEditorButton(panel, "Edit dialogue: meeting")
        storyEditorInput(graph().stage.root, "I will help the village").text = "Cancelled edit"
        closeGraph("Cancel")
        clickStoryEditorButton(panel, "Apply story")
        check(map.story == saved && changes == 1) { "Cancel changed the shared draft" }
        clickStoryEditorButton(panel, "Preview dialogue: meeting")
        check(storyEditorActors(screen.stage.root).any { it.name == "story-text" })
        clickStoryEditorButton(screen.stage.root, "I will help the village")
        clickStoryEditorButton(screen.stage.root, "Continue")
        clickStoryEditorButton(screen.stage.root, "Continue")
        check(
            storyEditorActors(screen.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                .any { it.text.toString() == "Preview complete".tr() })
        clickStoryEditorButton(screen.stage.root, "Close")
        check(map.story == saved && changes == 1)
        clickStoryEditorButton(panel, "Preview dialogue: meeting")
        clickStoryEditorButton(screen.stage.root, "Close preview")
        check(storyEditorActors(screen.stage.root).none { it is com.unciv.ui.story.StoryDialoguePopup })
        checkCinematicEditorPreview(screen)
        checkStorySaveAndPlay(screen)
    })
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

/** Создание примера заменяет карту и компактный список диалогов открытой формы. */
private fun checkStoryExampleCreation() {
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val original = TileMap(3, ruleset, false)
    val editor = com.unciv.ui.mapeditor.MapEditorScreen(original)
    try {
        editor.tabs.selectPage("Edit")
        editor.tabs.edit.selectPage("Story")
        check(!editor.isDirty)
        clickStoryEditorButton(editor.stage.root, "Create story example map")
        check(editor.tileMap !== original)
        check(editor.isDirty)
        check(editor.tileMap.scenarioHeroes.single().id == "guide")
        check(editor.tileMap.scenarioCities.single().id == "gate")
        check(editor.tabs.edit.currentSubTab() == "Story" && !editor.allowDragPainting)
        clickStoryEditorButton(editor.stage.root, "Dialogues")
        check(storyEditorActors(editor.stage.root).any { it.name == "scenario-edit-dialogue:parley" }) {
            "Story list still displays the previous map"
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

/** Проверяет кадр подготовки, причины заглушки и возврат после подтверждения через стек экранов. */
private fun checkCinematicEditorPreview(host: BaseScreen) {
    check(host.game.screen === host)
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    for ((path, reason) in listOf(
        "" to "No video file has been assigned to this scene.",
        "Videos/__story_ui_missing_video__.webm" to "The video file was not found."
    )) {
        val map = TileMap(3, ruleset, false).apply {
            story = StoryScenario(
                cinematics = arrayListOf(
                    StoryCinematic("arrival", path, "Preview arrival text", false)
                )
            )
        }
        val saved = map.story!!.deepCopy()
        val editor = com.unciv.ui.mapeditor.MapEditorScreen(map)
        host.game.pushScreen(editor)
        try {
            editor.tabs.selectPage("Edit")
            editor.tabs.edit.selectPage("Story")
            clickStoryEditorButton(editor.stage.root, "Cinematics")
            clickStoryEditorButton(editor.stage.root, "Preview cinematic: arrival")
            val cinematic = host.game.screen as? com.unciv.ui.story.StoryCinematicScreen
                ?: error("Cinematic preview did not open its production screen")
            cinematic.render(0f)
            check(storyEditorActors(cinematic.stage.root).any { it.name == "story-video-preparing" })
            check(storyEditorActors(cinematic.stage.root).none { it.name == "story-choice:continue" })
            cinematic.render(0f)
            check(storyEditorActors(cinematic.stage.root).any { it.name == "story-video-placeholder" })
            val labels =
                    storyEditorActors(cinematic.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
            check(labels.any { it.name == "story-text" && it.text.toString() == "Preview arrival text" })
            check(labels.any { it.name == "story-video-reason" && it.text.toString() == reason.tr() })
            if (path.isNotEmpty()) check(labels.any { it.name == "story-video-path" && it.text.toString() == path })
            cinematic.render(0f)
            check(host.game.screen === cinematic) { "Fallback completed without confirmation" }
            val button = storyEditorActors(cinematic.stage.root).filterIsInstance<TextButton>()
                .single { it.name == "story-choice:continue" }
            for (scroll in generateSequence(button.parent) { it.parent }
                .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()) {
                val content = scroll.actor ?: continue
                val position =
                        button.localToActorCoordinates(content, com.badlogic.gdx.math.Vector2())
                scroll.scrollTo(position.x, position.y, button.width, button.height)
                scroll.updateVisualScroll()
            }
            cinematic.stage.draw()
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
            cinematic.stage.touchUp(
                point.x.toInt(),
                point.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            check(host.game.screen === editor)
            editor.stage.act(0f)
            check(
                storyEditorActors(editor.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                    .any { it.text.toString() == "Preview complete".tr() })
            clickStoryEditorButton(editor.stage.root, "Close")
            check(map.story == saved && !editor.isDirty)
        } finally {
            while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
        }
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
