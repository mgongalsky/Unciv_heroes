package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.UncivGame
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate
import com.unciv.ui.utils.extensions.stageBoundingBox
import com.unciv.ui.popup.applyRoyalTextLayout

/** Checks constrained logical sizes and callbacks; capture does not approve a visual baseline. */
internal object ConfirmPopupGoldenChecks {
    fun open(screen: BaseScreen) {
        check(UncivGame.Current.settings.skin == "Minimal") {
            "Royal confirmation checks require the Minimal skin"
        }
        verifyRoyalTextReuse()
        verifyDropboxWarning(screen)
        MultiplayerStatusGoldenChecks.verify(screen)
        OptionsPopupGoldenChecks.verify(screen)
        val question = List(18) {
            "Replace the current army with the selected template? Existing changes will be lost."
        }.joinToString(" ")
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (accept in listOf(false, true)) {
                    var confirmed = 0
                    var cancelled = 0
                    val popup = ConfirmPopup(
                        screen, question, "Replace the army with the selected template",
                        restoreDefault = { cancelled++ }, action = { confirmed++ }
                    )
                    try {
                        screen.stage.addActor(popup)
                        popup.setFillParent(false)
                        popup.setSize(width, 480f)
                        popup.setPosition(0f, 0f)
                        popup.isVisible = true
                        popup.validate()
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        scroll.validate()
                        popup.innerTable.validate()
                        val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                        val dimensions =
                                "stage=$width popup=${popup.width} scroll=${scroll.width} " +
                                        "viewport=${scroll.scrollWidth} bar=${scroll.scrollBarWidth} " +
                                        "inner=${popup.innerTable.width} pref=${popup.innerTable.prefWidth} " +
                                        "min=${popup.innerTable.minWidth} maxX=${scroll.maxX} maxY=${scroll.maxY} " +
                                        "buttons=${
                                            buttons.map {
                                                listOf(
                                                    it.width,
                                                    it.prefWidth,
                                                    it.minWidth,
                                                    it.height
                                                )
                                            }
                                        }"
                        check(popup.innerTable.width <= width + 1f) { dimensions }
                        check(scroll.maxX <= 1f) { "Confirmation requires horizontal scrolling: $dimensions" }
                        check(scroll.maxY > 0f) { "Long question must exercise vertical scrolling: $dimensions" }
                        check(buttons.size == 2)
                        val cancel = buttons[0]
                        val confirm = buttons[1]
                        if (width == 360f) {
                            check(cancel.y >= confirm.y + confirm.height - 1f)
                        } else {
                            check(cancel.x + cancel.width <= confirm.x + 1f)
                        }
                        scroll.scrollY = scroll.maxY
                        scroll.updateVisualScroll()
                        scroll.layout()
                        val visible = scroll.stageBoundingBox
                        for (button in buttons) {
                            val bounds = button.stageBoundingBox
                            check(bounds.x >= visible.x - 1f && bounds.x + bounds.width <= visible.x + visible.width + 1f) { dimensions }
                            check(bounds.y >= visible.y - 1f && bounds.y + bounds.height <= visible.y + visible.height + 1f) {
                                "Confirmation button is not reachable after scrolling: $dimensions"
                            }
                        }
                        (if (accept) confirm else cancel).activate()
                        check(popup.parent == null)
                        check(confirmed == if (accept) 1 else 0)
                        check(cancelled == if (accept) 0 else 1)
                    } finally {
                        if (popup.parent != null) popup.close()
                    }
                }
            }
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
        }
        ConfirmPopup(
            screen,
            "Replace the current army with the selected template? Existing changes will be lost.",
            "Replace army", action = {}
        ).open(force = true)
    }
    private fun verifyRoyalTextReuse() {
        for (width in listOf(360f, 1000f)) {
            val stage = com.unciv.ui.UncivStage(
                com.badlogic.gdx.utils.viewport.FitViewport(
                    width,
                    480f
                )
            )
            try {
                stage.viewport.update(
                    com.badlogic.gdx.Gdx.graphics.width,
                    com.badlogic.gdx.Gdx.graphics.height,
                    true
                )
                val popup = com.unciv.ui.popup.Popup(stage)
                var closed = 0
                popup.closeListeners.add { closed++ }
                try {
                    popup.addGoodSizedLabel("Loading latest game state...")
                    popup.applyRoyalTextLayout()
                    popup.open(force = true)
                    popup.validate()
                    check(popup.innerTable.background is com.unciv.ui.utils.RoyalPanelDrawable)
                    check(popup.innerTable.children.filterIsInstance<TextButton>().isEmpty())
                    val message =
                            "The requested game could not be loaded. Please check the server and try again. ".repeat(
                                30
                            )
                    popup.reuseWith(message, true)
                    popup.applyRoyalTextLayout()
                    popup.applyRoyalTextLayout()
                    popup.validate()
                    val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                    scroll.validate()
                    popup.innerTable.validate()
                    stage.draw()
                    val detail = "Reused Royal popup at ${width}x480"
                    check(popup.innerTable.background is com.unciv.ui.utils.RoyalPanelDrawable) { detail }
                    val origin = scroll.localToStageCoordinates(com.badlogic.gdx.math.Vector2())
                    check(origin.x >= -1f && origin.y >= -1f && origin.x + scroll.width <= width + 1f && origin.y + scroll.height <= 481f) { detail }
                    check(scroll.maxX <= 1f && scroll.maxY > 0f) { detail }
                    val label =
                            popup.innerTable.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                                .single()
                    check(label.text.toString() == message && label.wrap && label.height + 1f >= label.prefHeight) { detail }
                    val color = label.color.cpy()
                    label.style.fontColor?.let { color.mul(it) }
                    check(color == com.badlogic.gdx.graphics.Color.valueOf("453925")) { detail }
                    val button = popup.innerTable.children.filterIsInstance<TextButton>().single()
                    check(button.label.wrap && button.label.height + 1f >= button.label.prefHeight) { detail }
                    val local = button.localToAscendantCoordinates(
                        scroll.actor,
                        com.badlogic.gdx.math.Vector2()
                    )
                    scroll.scrollTo(local.x, local.y, button.width, button.height)
                    scroll.updateVisualScroll()
                    stage.draw()
                    val point = button.localToStageCoordinates(
                        com.badlogic.gdx.math.Vector2(
                            button.width / 2f,
                            button.height / 2f
                        )
                    )
                    val hit = stage.hit(point.x, point.y, true)
                    check(hit === button || hit?.isDescendantOf(button) == true) { detail }
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(
                        point.x.toInt(),
                        point.y.toInt(),
                        0,
                        com.badlogic.gdx.Input.Buttons.LEFT
                    )
                    stage.touchUp(
                        point.x.toInt(),
                        point.y.toInt(),
                        0,
                        com.badlogic.gdx.Input.Buttons.LEFT
                    )
                    check(popup.parent == null && closed == 1) { detail }
                } finally {
                    popup.events.stopReceiving()
                    popup.remove()
                }
            } finally {
                stage.dispose()
            }
        }
    }

    private fun verifyDropboxWarning(screen: BaseScreen) {
        val settings = UncivGame.Current.settings.multiplayer
        val originalServer = settings.server
        val originalHidden = settings.hideDropboxWarning
        val stage = screen.stage as com.unciv.ui.UncivStage
        val originalViewport = stage.viewport
        val originalArea = com.badlogic.gdx.math.Rectangle(stage.lastKnownVisibleArea)
        try {
            settings.server = com.unciv.Constants.dropboxMultiplayerServer
            for (width in listOf(360f, 1000f)) {
                stage.viewport = com.badlogic.gdx.utils.viewport.FitViewport(width, 480f)
                stage.viewport.update(
                    com.badlogic.gdx.Gdx.graphics.width,
                    com.badlogic.gdx.Gdx.graphics.height,
                    true
                )
                com.unciv.logic.event.EventBus.send(
                    com.unciv.ui.UncivStage.VisibleAreaChanged(
                        com.badlogic.gdx.math.Rectangle(
                            0f,
                            0f,
                            width,
                            480f
                        )
                    )
                )
                for (hide in listOf(false, true)) {
                    settings.hideDropboxWarning = false
                    val previous = stage.actors.toList()
                    com.unciv.ui.multiplayer.MultiplayerHelpers.showDropboxWarning(screen)
                    val popup = stage.actors.filterIsInstance<com.unciv.ui.popup.Popup>()
                        .single { it !in previous }
                    try {
                        popup.validate()
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        val checkBox =
                                popup.innerTable.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.CheckBox>()
                                    .single()
                        checkBox.label.setText(
                            "Do not show this multiplayer server warning again on future visits ".repeat(
                                8
                            )
                        )
                        val warning =
                                popup.innerTable.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                                    .single()
                        warning.setText((warning.text.toString() + "\n\n").repeat(8))
                        popup.invalidateHierarchy()
                        popup.validate()
                        scroll.validate()
                        popup.innerTable.validate()
                        stage.draw()
                        val detail =
                                "Dropbox warning at ${width}x480, hide=$hide, maxX=${scroll.maxX}, maxY=${scroll.maxY}, scroll=${scroll.width}x${scroll.height}, inner=${popup.innerTable.width}x${popup.innerTable.height}"
                        check(popup.innerTable.background is com.unciv.ui.utils.RoyalPanelDrawable) { detail }
                        val origin = scroll.localToStageCoordinates(com.badlogic.gdx.math.Vector2())
                        check(origin.x >= -1f && origin.y >= -1f && origin.x + scroll.width <= width + 1f && origin.y + scroll.height <= 481f) { detail }
                        check(scroll.maxX <= 1f) { "Horizontal overflow: $detail" }
                        check(scroll.maxY > 0f) { "Fixture did not exercise vertical scrolling: $detail" }
                        check(warning.wrap && warning.height + 1f >= warning.prefHeight) { detail }
                        check(checkBox.label.wrap && checkBox.label.height + 1f >= checkBox.label.prefHeight) { detail }
                        val color = checkBox.label.color.cpy()
                        checkBox.label.style.fontColor?.let { color.mul(it) }
                        check(color == com.badlogic.gdx.graphics.Color.valueOf("453925")) { detail }
                        fun click(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                            val local = actor.localToAscendantCoordinates(
                                scroll.actor,
                                com.badlogic.gdx.math.Vector2()
                            )
                            scroll.scrollTo(local.x, local.y, actor.width, actor.height)
                            scroll.updateVisualScroll()
                            stage.draw()
                            val point = actor.localToStageCoordinates(
                                com.badlogic.gdx.math.Vector2(
                                    actor.width / 2f,
                                    actor.height / 2f
                                )
                            )
                            val hit = stage.hit(point.x, point.y, true)
                            check(hit === actor || hit?.isDescendantOf(actor) == true) { detail }
                            stage.stageToScreenCoordinates(point)
                            stage.touchDown(
                                point.x.toInt(),
                                point.y.toInt(),
                                0,
                                com.badlogic.gdx.Input.Buttons.LEFT
                            )
                            stage.touchUp(
                                point.x.toInt(),
                                point.y.toInt(),
                                0,
                                com.badlogic.gdx.Input.Buttons.LEFT
                            )
                        }
                        check(!checkBox.isChecked)
                        if (hide) click(checkBox)
                        check(checkBox.isChecked == hide) { detail }
                        check(!settings.hideDropboxWarning) { "Preference changed before closing" }
                        val close = popup.innerTable.children.filterIsInstance<TextButton>().last()
                        click(close)
                        check(popup.parent == null && settings.hideDropboxWarning == hide) { detail }
                        if (hide) {
                            com.unciv.ui.multiplayer.MultiplayerHelpers.showDropboxWarning(screen)
                            check(stage.actors.none { it is com.unciv.ui.popup.Popup && it !in previous }) { detail }
                        }
                    } finally {
                        popup.events.stopReceiving()
                        popup.remove()
                    }
                }
            }
        } finally {
            settings.server = originalServer
            settings.hideDropboxWarning = originalHidden
            stage.viewport = originalViewport
            originalViewport.update(
                com.badlogic.gdx.Gdx.graphics.width,
                com.badlogic.gdx.Gdx.graphics.height,
                true
            )
            com.unciv.logic.event.EventBus.send(
                com.unciv.ui.UncivStage.VisibleAreaChanged(
                    originalArea
                )
            )
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }
}
