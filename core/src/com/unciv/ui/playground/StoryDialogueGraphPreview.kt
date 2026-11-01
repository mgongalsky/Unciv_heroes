package com.unciv.ui.playground

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
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

/** Проверяет выбор карточки и переход через экранный ввод, сохранность текста при resize и изоляцию источника. */
internal fun checkStoryGraphInteraction(host: BaseScreen, clickButton: (Actor, String) -> Unit) {
    fun actors(root: Actor): List<Actor> = listOf(root) +
        if (root is Group) root.children.toList().flatMap { actors(it) } else emptyList()
    val ruleset = RulesetCache.getComplexRuleset(GameParameters())
    val map = TileMap(3, ruleset, false)
    val source = StoryDialogue(id = "graph", startNodeId = "root", nodes = arrayListOf(
        StoryDialogueNode(id = "root", text = "Root line", nextNodeId = "end"),
        StoryDialogueNode(id = "end", text = "End line"),
        StoryDialogueNode(id = "spare", text = "Spare line")
    ))
    val saved = source.deepCopy()
    var applied: StoryDialogue? = null
    val screen = StoryDialogueGraphScreen(source, map, ruleset) { applied = it }
    host.game.pushScreen(screen)
    val originalWidth = screen.stage.viewport.screenWidth
    val originalHeight = screen.stage.viewport.screenHeight
    fun clickActor(actor: Actor) {
        screen.stage.draw()
        val center = actor.localToStageCoordinates(Vector2(actor.width / 2f, actor.height / 2f))
        check(center.x in 0f..screen.stage.width && center.y in 0f..screen.stage.height)
        val hit = screen.stage.hit(center.x, center.y, true)
        check(hit != null && generateSequence(hit) { it.parent }.any { it === actor }) {
            "Graph actor cannot receive input: ${actor.name}, hit=$hit"
        }
        val point = screen.stage.stageToScreenCoordinates(center)
        screen.stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
        screen.stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
    }
    fun select(id: String) {
        screen.render(0f)
        val all = actors(screen.stage.root)
        val canvas = all.filterIsInstance<StoryDialogueGraphCanvas>().single()
        val pane = all.filterIsInstance<ZoomableScrollPane>().single()
        val card = actors(canvas).single { it.name == "story-graph-node:$id" }
        pane.scrollTo(card.x + card.width / 2f, canvas.height - card.y - card.height / 2f, true)
        clickActor(card)
        check(actors(screen.stage.root).filterIsInstance<TextField>()
            .single { it.name == "story-field:Node ID" }.text == id)
    }
    try {
        select("spare")
        val text = actors(screen.stage.root).filterIsInstance<TextField>()
            .single { it.name == "story-field:Text" }
        text.text = "Preserved across resize"
        screen.resize(800, 800)
        screen.render(0f)
        check(text.text == "Preserved across resize")
        check(text.width <= 300f)
        screen.resize(originalWidth, originalHeight)
        clickButton(screen.stage.root, "Update node")
        select("root")
        @Suppress("UNCHECKED_CAST")
        val destination = actors(screen.stage.root).filterIsInstance<SelectBox<*>>().single() as SelectBox<Any>
        destination.selected = destination.items.first { it.toString() == "spare" }
        clickButton(screen.stage.root, "Update node")
        check(source == saved && applied == null)
        // Две подписи ответов должны занимать разные полосы даже при общей цели.
        val branching = StoryDialogueGraphCanvas(StoryDialogue(id = "branch", startNodeId = "q", nodes = arrayListOf(
            StoryDialogueNode(id = "q", text = "Question", choices = arrayListOf(
                StoryChoice("yes", "Yes"), StoryChoice("no", "No")
            ))
        )), "q", {})
        val labels = actors(branching).filterIsInstance<Label>().filter { it.name?.startsWith("story-graph-edge:") == true }
        check(labels.size == 2)
        check(kotlin.math.abs(labels[0].y - labels[1].y) >= maxOf(labels[0].height, labels[1].height))
        screen.render(0f)
        val apply = actors(screen.stage.root).filterIsInstance<TextButton>()
            .single { it.text.toString() == "Apply dialogue".tr() }
        clickActor(apply)
        check(host.game.screen === host)
        val result = checkNotNull(applied)
        check(result.nodes.first { it.id == "root" }.nextNodeId == "spare")
        check(result.nodes.first { it.id == "spare" }.text == "Preserved across resize")
        check(source == saved)
    } finally {
        while (host.game.screen !== host && host.game.screenStack.size > 1) host.game.popScreen()
    }
}
