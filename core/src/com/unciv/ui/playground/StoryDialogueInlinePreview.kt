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
