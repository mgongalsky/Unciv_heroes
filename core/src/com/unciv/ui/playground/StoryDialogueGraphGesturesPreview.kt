package com.unciv.ui.playground

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.translations.tr
import com.unciv.pure.domain.story.*
import com.unciv.ui.mapeditor.StoryDialogueGraphCanvas
import com.unciv.ui.mapeditor.StoryDialogueGraphScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ZoomableScrollPane

/** Реальные жесты Stage проверяют конфликт drag/pan, порты, историю и ограничение длинного текста. */
internal fun checkStoryGraphGestures(host: BaseScreen, clickButton: (Actor, String) -> Unit) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
        if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val source = StoryDialogue(id = "gestures", startNodeId = "root", nodes = arrayListOf(
        StoryDialogueNode(id = "root", speaker = "Long speaker ".repeat(12), text = "Long dialogue text ".repeat(60),
            choices = arrayListOf(StoryChoice("route", "A long answer ".repeat(25)), StoryChoice("exit", "Leave"))),
        StoryDialogueNode(id = "target", text = "Target")
    ), aiChoices = linkedMapOf("root" to "exit"))
    val saved = source.deepCopy()
    var result: StoryDialogue? = null
    val screen = StoryDialogueGraphScreen(source, TileMap(3, ruleset, false), ruleset) { result = it }
    host.game.pushScreen(screen)
    fun find(name: String) = actors(screen.stage.root).single { it.name == name }
    fun canvas() = actors(screen.stage.root).filterIsInstance<StoryDialogueGraphCanvas>().single()
    fun pane() = actors(screen.stage.root).filterIsInstance<ZoomableScrollPane>().single()
    fun render() { screen.render(0f) }
    fun center(actor: Actor): Vector2 = actor.localToStageCoordinates(Vector2(actor.width / 2f, actor.height / 2f))
    fun screenPoint(stagePoint: Vector2) = screen.stage.stageToScreenCoordinates(stagePoint.cpy())
    fun down(actor: Actor): Vector2 {
        render()
        val point = center(actor)
        val hit = screen.stage.hit(point.x, point.y, true)
        check(hit != null && generateSequence(hit) { it.parent }.any { it === actor }) {
            "Gesture cannot reach ${actor.name}: hit=$hit"
        }
        val pixel = screenPoint(point)
        screen.stage.touchDown(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
        return point
    }
    fun drag(point: Vector2) {
        val pixel = screenPoint(point)
        screen.stage.touchDragged(pixel.x.toInt(), pixel.y.toInt(), 0)
    }
    fun up(point: Vector2) {
        val pixel = screenPoint(point)
        screen.stage.touchUp(pixel.x.toInt(), pixel.y.toInt(), 0, Input.Buttons.LEFT)
    }
    fun frameBoth() {
        val graph = pane()
        graph.zoom(0.55f)
        graph.scrollTo(390f, canvas().height - 190f, true)
        render()
    }
    fun assertTextFits() {
        for (label in actors(canvas()).filterIsInstance<Label>()) {
            check(label.prefHeight <= label.height + 1f) { "Graph label overflows vertically: ${label.name}" }
            check(label.x >= 0f && label.y >= 0f)
            val parent = label.parent
            check(label.x + label.width <= parent.width + 1f && label.y + label.height <= parent.height + 1f) {
                "Graph label is outside its card: ${label.name}"
            }
        }
    }
    try {
        frameBoth()
        assertTextFits()
        val rootBefore = find("story-graph-node:root")
        val original = Vector2(rootBefore.x, rootBefore.y)
        val graph = pane()
        val scrollBefore = Vector2(graph.scrollX, graph.scrollY)
        val start = down(find("story-graph-drag:root"))
        val destination = start.cpy().add(44f, 22f)
        drag(destination)
        up(destination)
        render()
        val moved = find("story-graph-node:root")
        check(kotlin.math.abs(moved.x - original.x - 80f) < 4f)
        check(kotlin.math.abs(moved.y - original.y - 40f) < 4f)
        check(kotlin.math.abs(graph.scrollX - scrollBefore.x) < 2f && kotlin.math.abs(graph.scrollY - scrollBefore.y) < 2f) {
            "Dragging a card also panned the graph"
        }
        clickButton(screen.stage.root, "Undo")
        check(kotlin.math.abs(find("story-graph-node:root").x - original.x) < 1f)
        clickButton(screen.stage.root, "Redo")
        check(kotlin.math.abs(find("story-graph-node:root").x - original.x - 80f) < 4f)
        clickButton(screen.stage.root, "Text +")
        clickButton(screen.stage.root, "Text +")
        clickButton(screen.stage.root, "Text +")
        clickButton(screen.stage.root, "Text +")
        assertTextFits()
        clickButton(screen.stage.root, "Text −")
        assertTextFits()
        frameBoth()

        // Пустой drop не изменяет переход; Escape отменяет соединение, не закрывая редактор.
        val emptyDrop = down(find("story-graph-output:root:route")).add(30f, -55f)
        drag(emptyDrop); up(emptyDrop)
        check(!canvas().gestureActive)
        val cancelled = down(find("story-graph-output:root:route")).add(30f, 20f)
        drag(cancelled)
        check(canvas().gestureActive)
        screen.stage.keyDown(Input.Keys.ESCAPE)
        check(host.game.screen === screen && !canvas().gestureActive)
        up(cancelled)

        // Соединение сохраняет ещё не применённый текст выбранного узла.
        val text = actors(screen.stage.root).filterIsInstance<TextField>().single { it.name == "story-field:Text" }
        text.text = "Pending text preserved by connection"
        down(find("story-graph-output:root:route"))
        val target = center(find("story-graph-input:target"))
        drag(target); up(target)
        render()
        check(actors(screen.stage.root).filterIsInstance<TextField>().single { it.name == "story-field:Text" }.text ==
            "Pending text preserved by connection")
        clickButton(screen.stage.root, "Disconnect output")
        clickButton(screen.stage.root, "Undo")
        frameBoth()
        val activeText = actors(screen.stage.root).filterIsInstance<TextField>().single { it.name == "story-field:Text" }
        screen.stage.keyboardFocus = activeText
        screen.stage.keyDown(Input.Keys.FORWARD_DEL)
        check(actors(screen.stage.root).any { it.name == "story-graph-node:root" })
        screen.stage.keyboardFocus = null
        check(source == saved && result == null)
        val apply = actors(screen.stage.root).filterIsInstance<TextButton>().single { it.text.toString() == "Apply dialogue".tr() }
        for (scroll in generateSequence(apply.parent) { it.parent }.filterIsInstance<ScrollPane>()) {
            val content = scroll.actor ?: continue
            val pos = apply.localToActorCoordinates(content, Vector2())
            scroll.scrollTo(pos.x, pos.y, apply.width, apply.height)
            scroll.updateVisualScroll()
        }
        val applyPoint = down(apply)
        up(applyPoint)
        check(host.game.screen === host)
        val applied = checkNotNull(result)
        val root = applied.nodes.first { it.id == "root" }
        check(root.choices.first { it.id == "route" }.nextNodeId == "target")
        check(root.choices.first { it.id == "exit" }.nextNodeId.isEmpty())
        check(applied.aiChoices["root"] == "exit")
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}
