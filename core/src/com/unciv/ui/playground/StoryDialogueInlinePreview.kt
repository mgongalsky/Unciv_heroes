package com.unciv.ui.playground

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.mapeditor.StoryDialogueGraphScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ZoomableScrollPane
import com.unciv.models.translations.tr

/** Проверяет реальный ввод в карточке и форме, историю, добавление ответа и применение независимой копии. */
internal fun checkStoryDialogueInline(host: BaseScreen, clickButton: (Actor, String) -> Unit) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
            if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()

    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val source = StoryDialogue(
        id = "inline", startNodeId = "root", nodes = arrayListOf(
            StoryDialogueNode(id = "root", text = "Original", nextNodeId = "end"),
            StoryDialogueNode(id = "end", text = "End")
        )
    )
    val saved = source.deepCopy()
    var result: StoryDialogue? = null
    val screen =
            StoryDialogueGraphScreen(source, TileMap(3, ruleset, false), ruleset) { result = it }
    host.game.pushScreen(screen)
    fun find(name: String): Actor = actors(screen.stage.root).single { it.name == name }
    fun field(name: String) = find(name) as TextField
    fun click(actor: Actor) {
        screen.render(0f)
        for (pane in generateSequence(actor.parent) { it.parent }.filterIsInstance<ScrollPane>()) {
            val content = pane.actor ?: continue
            val point = actor.localToActorCoordinates(content, Vector2())
            pane.scrollTo(point.x, point.y, actor.width, actor.height)
            pane.updateVisualScroll()
        }
        screen.stage.draw()
        val point = actor.localToStageCoordinates(Vector2(actor.width / 2, actor.height / 2))
        val hit = screen.stage.hit(point.x, point.y, true)
        check(hit != null && generateSequence(hit) { it.parent }.any { it === actor }) {
            "Inline control cannot receive input: ${actor.name}, hit=$hit"
        }
        val pixel = screen.stage.stageToScreenCoordinates(point)
        screen.stage.touchDown(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
        screen.stage.touchUp(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
    }

    fun type(input: TextField, value: String) {
        check(screen.stage.keyboardFocus === input)
        input.selectAll()
        value.forEach { screen.stage.keyTyped(it) }
        check(input.text == value) { "Typed text differs: ${input.text}" }
        check(screen.stage.keyboardFocus === input) { "Typing lost keyboard focus" }
        check(input.cursorPosition == value.length)
    }

    fun graphText() = (find("story-graph-text:root") as Label).text.toString()
    try {
        click(find("story-graph-text:root"))
        val inline = field("story-inline:root:text")
        val pane = actors(screen.stage.root).filterIsInstance<ZoomableScrollPane>().single()
        val scroll = Vector2(pane.scrollX, pane.scrollY)
        type(inline, "Inline text")
        check(field("story-field:Text").text == "Inline text")
        check(graphText() == "Inline text")
        check(pane.scrollX == scroll.x && pane.scrollY == scroll.y)
        screen.stage.keyDown(Input.Keys.ESCAPE)
        check(host.game.screen === screen)
        check(actors(screen.stage.root).none { it.name == "story-inline:root:text" })
        clickButton(screen.stage.root, "Undo")
        check(graphText() == "Original")
        clickButton(screen.stage.root, "Redo")
        check(graphText() == "Inline text")
        val side = field("story-field:Text")
        click(side)
        type(side, "Sidebar text")
        check(graphText() == "Sidebar text")
        check(field("story-field:Text") === side)
        check(side.height <= 70f)
        // Неверный ID остаётся в форме; текст при этом продолжает отображаться.
        val id = field("story-field:Node ID")
        id.text = ""
        type(side, "Valid text")
        check(graphText() == "Valid text")
        id.text = "root"
        val add = find("story-graph-add-answer")
        check(generateSequence(add.parent) { it.parent }.none { it is ScrollPane })
        click(add)
        type(field("story-inline:root:answer1"), "First answer")
        check(field("story-field:Answer text").text == "First answer")
        screen.stage.keyDown(Input.Keys.ESCAPE)
        clickButton(screen.stage.root, "Add answer")
        type(field("story-inline:root:answer2"), "Second answer")
        screen.stage.keyDown(Input.Keys.ESCAPE)
        val answer = actors(screen.stage.root).filterIsInstance<Label>()
            .single { it.name?.startsWith("story-graph-edge:root:") == true && it.text.toString() == "First answer" }
        click(answer)
        type(field("story-inline:root:answer1"), "Edited answer")
        screen.stage.keyDown(Input.Keys.ESCAPE)
        check(source == saved && result == null)
        val apply =
                actors(screen.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextButton>()
                    .single { it.text.toString() == "Apply dialogue".tr() }
        click(apply)
        check(host.game.screen === host)
        val applied = checkNotNull(result)
        val root = applied.nodes.first { it.id == "root" }
        check(root.text == "Valid text")
        check(root.choices.map { it.text } == listOf("Edited answer", "Second answer"))
        check(root.choices.first().nextNodeId == "end" && root.choices.last().nextNodeId.isEmpty())
        check(applied.aiChoices["root"] == "answer1")
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}

/** Проверяет ответы невыбранной карточки, переключение ввода при zoom и раскладку компактной формы. */
internal fun checkExistingStoryAnswerInput(host: BaseScreen) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
            if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()

    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val source = StoryDialogue(
        id = "answer-input", startNodeId = "root", nodes = arrayListOf(
            StoryDialogueNode(
                id = "root", text = "Choose an answer", choices = arrayListOf(
                    com.unciv.pure.domain.story.StoryChoice(
                        "first",
                        "First existing answer",
                        "end"
                    ),
                    com.unciv.pure.domain.story.StoryChoice(
                        "second",
                        "Second existing answer",
                        "end"
                    ),
                    com.unciv.pure.domain.story.StoryChoice("third", "Third existing answer", "end")
                )
            ),
            StoryDialogueNode(id = "end", text = "End")
        ), aiChoices = linkedMapOf("root" to "second")
    )
    val saved = source.deepCopy()
    val screen = StoryDialogueGraphScreen(source, TileMap(3, ruleset, false), ruleset) {}
    host.game.pushScreen(screen)
    fun find(name: String) = actors(screen.stage.root).single { it.name == name }
    fun pane() = actors(screen.stage.root).filterIsInstance<ZoomableScrollPane>().single()

    // События идут через hit-test Stage; между нажатием и отпусканием проходит обновление экрана.
    fun click(actor: Actor, cardId: String, fraction: Float) {
        screen.render(0f)
        val scroll = pane()
        val card = find("story-graph-node:$cardId")
        val position = card.localToActorCoordinates(scroll.actor!!.parent, Vector2())
        scroll.scrollTo(position.x, position.y, card.width, card.height)
        scroll.updateVisualScroll()
        screen.stage.draw()
        val point =
                actor.localToStageCoordinates(Vector2(actor.width * fraction, actor.height / 2f))
        val hit = screen.stage.hit(point.x, point.y, true)
        check(hit != null && generateSequence(hit) { it.parent }.any { it === actor }) {
            "Control is covered: ${actor.name}, hit=$hit"
        }
        val pixel = screen.stage.stageToScreenCoordinates(point)
        screen.stage.touchDown(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
        screen.render(1f / 60f)
        screen.stage.touchDragged(pixel.x.toInt() + 2, pixel.y.toInt() + 1, 0)
        screen.render(1f / 60f)
        screen.stage.touchUp(pixel.x.toInt() + 2, pixel.y.toInt() + 1, 0, Input.Buttons.LEFT)
        screen.render(1f / 60f)
    }

    fun clickAnswer(text: String, fraction: Float) {
        val index = listOf("First", "Second", "Third").indexOf(text.substringBefore(' '))
        check(index >= 0)
        val label = find("story-graph-edge:root:$index") as Label
        val displayed = label.text.toString().removeSuffix("…")
        check(displayed.isNotBlank() && text.startsWith(displayed)) {
            "Unexpected answer caption: expected=$text, displayed=${label.text}"
        }
        click(label, "root", fraction)
    }

    fun enter(id: String, value: String) {
        val input = actors(screen.stage.root).filterIsInstance<TextField>()
            .singleOrNull { it.name == "story-inline:root:$id" }
        checkNotNull(input) {
            "Answer did not open inline: $id; selected=${(find("story-field:Node ID") as TextField).text}; error=${
                (find(
                    "story-graph-error"
                ) as Label).text
            }"
        }
        check(input.parent === find("story-graph-node:root")) { "Answer editor is outside its card" }
        check(screen.stage.keyboardFocus === input) { "Answer did not receive focus: $id" }
        input.selectAll()
        value.forEach { screen.stage.keyTyped(it) }
        screen.render(1f / 60f)
        check(input.text == value && screen.stage.keyboardFocus === input)
        val index = listOf("first", "second", "third").indexOf(id)
        val fields = actors(screen.stage.root).filterIsInstance<TextField>()
            .filter { it.name == "story-field:Answer text" }
        check(fields[index].text == value) { "Wrong answer synchronized: $id" }
    }
    try {
        screen.render(0f)
        val forms =
                actors(screen.stage.root).filterIsInstance<com.unciv.ui.mapeditor.StoryEditorForm>()
        for (form in forms) for (label in actors(form).filterIsInstance<Label>()) {
            check(label.style.font.capHeight * label.fontScaleY <= 20f) {
                "Oversized form label: ${label.text}"
            }
        }
        val pause =
                actors(screen.stage.root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.CheckBox>()
                    .single { it.text.toString() == "Pause world".tr() }
        pause.validate()
        pause.label.validate()
        // Высота одной строки и достаточная ширина защищают от переноса по буквам.
        check(pause.label.width >= pause.label.prefWidth - 1f) { "Pause world caption is squeezed" }
        check(pause.label.prefHeight <= 30f && pause.height <= 40f) { "Pause world is not a compact row" }
        val add = find("story-graph-add-answer") as com.badlogic.gdx.scenes.scene2d.ui.TextButton
        check(add.label.prefWidth <= add.width) { "Add answer caption exceeds button width" }
        check(add.label.style.font.capHeight * add.label.fontScaleY <= 20f)
        for ((pass, zoom) in listOf(1f, 0.65f, 1.3f).withIndex()) {
            pane().zoom(zoom)
            // Ответ открывается первым кликом, даже когда выбрана другая карточка.
            click(find("story-graph-drag:end"), "end", 0.5f)
            check((find("story-field:Node ID") as TextField).text == "end")
            val suffix = if (pass == 0) "existing answer" else "edited ${pass - 1}"
            clickAnswer("First $suffix", 0.15f)
            enter("first", "First edited $pass")
            // Переключение ответа не требует предварительного Escape.
            clickAnswer("Second $suffix", 0.5f)
            enter("second", "Second edited $pass")
            clickAnswer("Third $suffix", 0.85f)
            enter("third", "Third edited $pass")
            screen.stage.keyDown(Input.Keys.ESCAPE)
            check(host.game.screen === screen)
        }
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}

/** Преобразование Continue в ответ сохраняет переход, историю и позволяет применить весь сценарий без немедленного выхода. */
internal fun checkContinueStoryAnswerInput(host: BaseScreen, clickButton: (Actor, String) -> Unit) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
            if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()

    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val source = StoryDialogue(
        id = "continue-input", startNodeId = "start", nodes = arrayListOf(
            StoryDialogueNode(id = "start", text = "Start", nextNodeId = "branch"),
            StoryDialogueNode(id = "branch", text = "Question", nextNodeId = "end"),
            StoryDialogueNode(id = "end", text = "End")
        )
    )
    val saved = source.deepCopy()
    val map = TileMap(3, ruleset, false)
    var result: StoryDialogue? = null
    val screen = StoryDialogueGraphScreen(source, map, ruleset) { edited ->
        // Проверяем публикацию через общий валидатор, а не только возврат черновика из формы.
        val candidate = com.unciv.pure.domain.story.StoryScenario(dialogues = arrayListOf(edited))
        check(
            com.unciv.logic.map.ScenarioStoryEditorCommands.apply(
                map,
                ruleset,
                emptyList(),
                candidate
            )
        )
        result = edited
    }
    host.game.pushScreen(screen)
    fun find(name: String) = actors(screen.stage.root).single { it.name == name }
    fun clickContinue() {
        screen.render(0f)
        val card = find("story-graph-node:branch") as Group
        val label = card.children.filterIsInstance<Label>()
            .single { it.name?.startsWith("story-graph-edge:") == true }
        val pane = actors(screen.stage.root).filterIsInstance<ZoomableScrollPane>().single()
        val position = card.localToActorCoordinates(pane.actor!!.parent, Vector2())
        pane.scrollTo(position.x, position.y, card.width, card.height)
        pane.updateVisualScroll()
        screen.stage.draw()
        val point = label.localToStageCoordinates(Vector2(label.width / 2f, label.height / 2f))
        check(
            screen.stage.hit(
                point.x,
                point.y,
                true
            ) === label
        ) { "Continue does not receive clicks" }
        val pixel = screen.stage.stageToScreenCoordinates(point)
        val processor = checkNotNull(com.badlogic.gdx.Gdx.input.inputProcessor)
        check(processor === screen.stage)
        processor.touchDown(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
        screen.render(1f / 60f)
        processor.touchUp(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
        screen.render(1f / 60f)
    }
    try {
        screen.render(0f)
        check((find("story-field:Node ID") as TextField).text == "start")
        clickContinue()
        val input = find("story-inline:branch:answer1") as TextField
        check(input.parent === find("story-graph-node:branch"))
        check(screen.stage.keyboardFocus === input)
        input.selectAll()
        "Go through the gate".forEach { screen.stage.keyTyped(it) }
        screen.render(1f / 60f)
        check(input.text == "Go through the gate" && screen.stage.keyboardFocus === input)
        check((find("story-field:Answer text") as TextField).text == input.text)
        screen.stage.keyDown(Input.Keys.ESCAPE)
        // Первый Undo отменяет ввод, второй — создание ответа; переход должен пережить оба действия.
        clickButton(screen.stage.root, "Undo")
        clickButton(screen.stage.root, "Undo")
        check(actors(screen.stage.root).none { it.name == "story-field:Answer ID" })
        clickButton(screen.stage.root, "Redo")
        clickButton(screen.stage.root, "Redo")
        check((find("story-field:Answer text") as TextField).text == "Go through the gate")
        check(source == saved && result == null)
        clickButton(screen.stage.root, "Apply dialogue")
        val applied = checkNotNull(result)
        val branch = applied.nodes.single { it.id == "branch" }
        check(branch.nextNodeId.isEmpty())
        check(branch.choices.single().id == "answer1")
        check(branch.choices.single().text == "Go through the gate")
        check(branch.choices.single().nextNodeId == "end")
        check(applied.aiChoices["branch"] == "answer1")
        check(applied.nodes.single { it.id == "start" } == saved.nodes.single { it.id == "start" })
        val published = checkNotNull(map.story).dialogues.single()
        check(published == applied && published !== applied)
        check(published.nodes.single { it.id == "branch" } !== branch)
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}

/** Кнопка карточки добавляет ответы в нажатый узел, сохраняет первый переход и открывает встроенный ввод. */
internal fun checkCardAnswerButton(host: BaseScreen, clickButton: (Actor, String) -> Unit) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
            if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()

    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val source = StoryDialogue(
        id = "card-add", startNodeId = "start", nodes = arrayListOf(
            StoryDialogueNode(id = "start", text = "Start", nextNodeId = "branch"),
            StoryDialogueNode(id = "branch", text = "Question", nextNodeId = "end"),
            StoryDialogueNode(id = "end", text = "End")
        )
    )
    val saved = source.deepCopy()
    var result: StoryDialogue? = null
    val screen =
            StoryDialogueGraphScreen(source, TileMap(3, ruleset, false), ruleset) { result = it }
    host.game.pushScreen(screen)
    fun find(name: String) = actors(screen.stage.root).single { it.name == name }
    try {
        screen.render(0f)
        check((find("story-field:Node ID") as TextField).text == "start")
        for (number in 1..2) {
            val pane = actors(screen.stage.root).filterIsInstance<ZoomableScrollPane>().single()
            pane.zoom(if (number == 1) 0.65f else 1.3f)
            val card = find("story-graph-node:branch") as Group
            val add = find("story-graph-add-answer:branch")
            check(add.parent === card)
            check(add.y >= 0f && add.y + add.height <= card.height)
            val labels = card.children.filterIsInstance<Label>()
                .filter { it.name?.startsWith("story-graph-edge:") == true }
            check(labels.all { it.y >= add.y + add.height }) { "Add answer overlaps an answer" }
            val position = card.localToActorCoordinates(pane.actor!!.parent, Vector2())
            pane.scrollTo(position.x, position.y, card.width, card.height)
            pane.updateVisualScroll()
            screen.stage.draw()
            val point = add.localToStageCoordinates(Vector2(add.width / 2f, add.height / 2f))
            val hit = screen.stage.hit(point.x, point.y, true)
            check(hit != null && generateSequence(hit) { it.parent }.any { it === add })
            val pixel = screen.stage.stageToScreenCoordinates(point)
            screen.stage.touchDown(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
            screen.render(1f / 60f)
            screen.stage.touchUp(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
            screen.render(1f / 60f)
            val input = find("story-inline:branch:answer$number") as TextField
            check(input.parent === find("story-graph-node:branch"))
            check(screen.stage.keyboardFocus === input)
            input.selectAll()
            "Answer $number".forEach { screen.stage.keyTyped(it) }
            check(input.text == "Answer $number")
            // Следующее нажатие выполняется прямо из активного поля предыдущего ответа.
        }
        screen.stage.keyDown(Input.Keys.ESCAPE)
        check(source == saved && result == null)
        clickButton(screen.stage.root, "Apply dialogue")
        val applied = checkNotNull(result)
        val branch = applied.nodes.single { it.id == "branch" }
        check(branch.choices.map { it.text } == listOf("Answer 1", "Answer 2"))
        check(branch.choices.map { it.nextNodeId } == listOf("end", ""))
        check(applied.aiChoices["branch"] == "answer1")
        check(applied.nodes.single { it.id == "start" }.choices.isEmpty())
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}
