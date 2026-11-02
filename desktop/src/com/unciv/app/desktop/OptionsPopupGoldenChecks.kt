package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.logic.event.EventBus
import com.unciv.ui.UncivStage
import com.unciv.ui.options.OptionsPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.stageBoundingBox

/** Проверяет компоновку и навигацию; визуальное соответствие эталону не утверждается. */
internal object OptionsPopupGoldenChecks {
    private fun descendants(actor: Actor): Sequence<Actor> = sequence {
        yield(actor)
        if (actor is Group) for (child in actor.children) yieldAll(descendants(child))
    }

    fun verify(screen: BaseScreen) {
        val settings = screen.game.settings
        if (settings.skin != "Minimal") return
        val stage = screen.stage as UncivStage
        val originalViewport = stage.viewport
        val originalArea = Rectangle(stage.lastKnownVisibleArea)
        val tutorial = "Open the options table"
        val alreadyCompleted = settings.tutorialTasksCompleted.contains(tutorial)
        // Предотвращает вызов settings.save() из конструктора; отметка восстанавливается в finally.
        settings.tutorialTasksCompleted.add(tutorial)
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                var closed = 0
                val popup = OptionsPopup(screen, selectPage = 0, onClose = { closed++ })
                try {
                    popup.open(force = true)
                    for (caption in listOf("About", "Display", "Gameplay")) {
                        popup.tabs.selectPage(caption)
                        popup.validate()
                        stage.draw()
                        val detail = "Options $caption at ${width}x480"
                        check(popup.innerTable.background is RoyalPanelDrawable) { detail }
                        val bounds = popup.innerTable.stageBoundingBox
                        check(bounds.x >= -1f && bounds.y >= -1f && bounds.x + bounds.width <= width + 1f && bounds.y + bounds.height <= 481f) {
                            "$detail: frame=$bounds"
                        }
                        val content = popup.tabs.children.filterIsInstance<ScrollPane>().last()
                        if (caption != "About") {
                            val checkbox = descendants(content.actor).filterIsInstance<CheckBox>().first()
                            checkbox.label.setText("A long settings label that must remain readable without horizontal scrolling. ".repeat(6))
                            popup.invalidateHierarchy()
                            popup.validate()
                            stage.draw()
                            check(checkbox.label.wrap && checkbox.label.height + 1f >= checkbox.label.prefHeight) { "$detail: wrap=${checkbox.label.wrap}, label=${checkbox.label.width}x${checkbox.label.height}, labelPref=${checkbox.label.prefHeight}, checkbox=${checkbox.width}x${checkbox.height}, checkboxPref=${checkbox.prefHeight}, needsLayout=${checkbox.needsLayout()}, content=${content.width}x${content.height}, maxX=${content.maxX}, maxY=${content.maxY}" }
                            check(content.maxY > 0f) { "Expected vertical scrolling: $detail" }
                        }
                        check(content.maxX <= 1f) { "Horizontal overflow: $detail, maxX=${content.maxX}" }
                        for (label in descendants(content.actor).filterIsInstance<Label>().filter { it.wrap }) {
                            check(label.height + 1f >= label.prefHeight) { "Clipped label: $detail, text=${label.text}" }
                        }
                        popup.tabs.setPageScrollY(popup.tabs.activePage, content.maxY)
                        stage.draw()
                        check(kotlin.math.abs(content.scrollY - content.maxY) <= 1f) { "Cannot reach bottom: $detail" }
                    }
                    val close = popup.innerTable.children.filterIsInstance<TextButton>().single()
                    val point = close.localToStageCoordinates(Vector2(close.width / 2f, close.height / 2f))
                    val hit = stage.hit(point.x, point.y, true)
                    check(hit === close || hit?.isDescendantOf(close) == true) { "Options close is unreachable at width=$width" }
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    check(popup.parent == null && closed == 1) { "Options did not close exactly once" }
                } finally {
                    screen.game.musicController.onChange(null)
                    popup.events.stopReceiving()
                    popup.remove()
                }
            }
        } finally {
            if (!alreadyCompleted) settings.tutorialTasksCompleted.remove(tutorial)
            stage.viewport = originalViewport
            originalViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(originalArea))
            for (actor in stage.actors) (actor as? Layout)?.invalidateHierarchy()
        }
    }
}
