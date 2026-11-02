package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.UncivGame
import com.unciv.ui.popup.AskTextPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate
import com.unciv.ui.utils.extensions.stageBoundingBox

/** Exercises text entry and validation at constrained logical sizes. */
internal object AskTextPopupGoldenChecks {
    fun open(screen: BaseScreen) {
        check(UncivGame.Current.settings.skin == "Minimal")
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val oldFocus = screen.stage.keyboardFocus
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (accept in listOf(false, true)) {
                    val results = mutableListOf<String>()
                    val popup = AskTextPopup(
                        screen,
                        label = "Enter a name for the selected army. This name will be displayed in the army overview.",
                        defaultText = "invalid",
                        errorText = List(8) { "Please choose a different name for this army." }.joinToString(" "),
                        maxLength = 24,
                        validate = { it == "Valid army" },
                        actionOnOk = { results.add(it) }
                    )
                    try {
                        screen.stage.addActor(popup)
                        popup.setFillParent(false)
                        popup.setSize(width, 480f)
                        popup.setPosition(0f, 0f)
                        popup.isVisible = true
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        val field = popup.innerTable.children.filterIsInstance<TextField>().single()
                        val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                        check(buttons.size == 2)
                        check(screen.stage.keyboardFocus === field)
                        check(field.text == "invalid" && field.maxLength == 24)
                        for (char in popup.illegalChars) check(!field.textFieldFilter.acceptChar(field, char))
                        check(field.textFieldFilter.acceptChar(field, 'A'))
                        fun layout() {
                            popup.validate()
                            scroll.validate()
                            popup.innerTable.validate()
                            check(popup.innerTable.width <= width + 1f)
                            check(scroll.maxX <= 1f) {
                                "Text prompt scrolls horizontally at $width: ${scroll.maxX}"
                            }
                        }
                        layout()
                        val cellsBeforeError = popup.innerTable.cells.size
                        buttons[1].activate()
                        check(results.isEmpty() && popup.parent != null)
                        check(popup.innerTable.cells.size == cellsBeforeError + 1)
                        layout()
                        val cellsAfterError = popup.innerTable.cells.size
                        buttons[1].activate()
                        check(results.isEmpty() && popup.parent != null)
                        check(popup.innerTable.cells.size == cellsAfterError) {
                            "Repeated validation added another error cell"
                        }
                        layout()
                        val target = if (accept) buttons[1] else buttons[0]
                        scroll.scrollTo(target.x, target.y, target.width, target.height)
                        scroll.updateVisualScroll()
                        scroll.layout()
                        val bounds = target.stageBoundingBox
                        val visible = scroll.stageBoundingBox
                        check(bounds.y >= visible.y - 1f && bounds.y + bounds.height <= visible.y + visible.height + 1f)
                        if (accept) field.text = "Valid army"
                        target.activate()
                        check(popup.parent == null)
                        check(results == if (accept) listOf("Valid army") else emptyList<String>())
                    } finally {
                        if (popup.parent != null) popup.close()
                    }
                }
            }
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            screen.stage.keyboardFocus = oldFocus
        }
        AskTextPopup(screen, label = "Name this army", defaultText = "Royal guard").open(force = true)
    }
}
