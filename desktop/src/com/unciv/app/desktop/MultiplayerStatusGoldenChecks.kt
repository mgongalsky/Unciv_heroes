package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.logic.event.EventBus
import com.unciv.ui.UncivStage
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.stageBoundingBox
import com.unciv.ui.worldscreen.status.MultiplayerStatusPopup

internal object MultiplayerStatusGoldenChecks {
    fun verify(screen: BaseScreen) {
        val stage = screen.stage as UncivStage
        val originalViewport = stage.viewport
        val originalArea = Rectangle(stage.lastKnownVisibleArea)
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                val popup = MultiplayerStatusPopup(screen)
                var closed = 0
                popup.closeListeners.add { closed++ }
                try {
                    val picker = popup.pickerPane
                    check(!picker.rightSideButton.isEnabled && popup.selectedGame == null)
                    picker.rightSideButton.setText("Load the selected multiplayer game with a long name")
                    picker.closeButton.setText("Close this multiplayer game selection window")
                    picker.descriptionLabel.setText("A long multiplayer game description. ".repeat(20))
                    popup.open(force = true)
                    popup.validate()
                    stage.draw()
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    val detail = "Multiplayer status at ${width}x480: outer=${scroll.width}x${scroll.height}, maxX=${scroll.maxX}, picker=${picker.width}x${picker.height}"
                    check(popup.innerTable.background is RoyalPanelDrawable) { detail }
                    val bounds = scroll.stageBoundingBox
                    check(bounds.x >= -1f && bounds.y >= -1f && bounds.x + bounds.width <= width + 1f && bounds.y + bounds.height <= 481f) { detail }
                    check(scroll.maxX <= 1f && picker.scrollPane.maxX <= 1f) { detail }
                    for (button in listOf(picker.closeButton, picker.rightSideButton)) {
                        check(button.label.wrap && button.label.height + 1f >= button.label.prefHeight) { "Clipped action: $detail" }
                        val buttonBounds = button.stageBoundingBox
                        val bottomBounds = picker.bottomTable.stageBoundingBox
                        check(buttonBounds.x >= bottomBounds.x - 1f && buttonBounds.x + buttonBounds.width <= bottomBounds.x + bottomBounds.width + 1f) { detail }
                        check(buttonBounds.y >= bottomBounds.y - 1f && buttonBounds.y + buttonBounds.height <= bottomBounds.y + bottomBounds.height + 1f) { "Action exceeds bottom panel: $detail" }
                    }
                    val close = picker.closeButton
                    val local = close.localToAscendantCoordinates(scroll.actor, Vector2())
                    scroll.scrollTo(local.x, local.y, close.width, close.height)
                    scroll.updateVisualScroll()
                    stage.draw()
                    val point = close.localToStageCoordinates(Vector2(close.width / 2f, close.height / 2f))
                    val hit = stage.hit(point.x, point.y, true)
                    check(hit === close || hit?.isDescendantOf(close) == true) { "Close is not reachable: $detail" }
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    check(popup.parent == null && closed == 1) { detail }
                } finally {
                    popup.events.stopReceiving()
                    popup.remove()
                }
            }
        } finally {
            stage.viewport = originalViewport
            originalViewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(originalArea))
            for (actor in stage.actors) (actor as? Layout)?.invalidateHierarchy()
        }
    }
}
