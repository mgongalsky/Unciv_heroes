package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.unciv.UncivGame
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate
import com.unciv.ui.utils.extensions.stageBoundingBox

/** Checks constrained logical sizes and callbacks; capture does not approve a visual baseline. */
internal object ConfirmPopupGoldenChecks {
    fun open(screen: BaseScreen) {
        check(UncivGame.Current.settings.skin == "Minimal") {
            "Royal confirmation checks require the Minimal skin"
        }
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
}
