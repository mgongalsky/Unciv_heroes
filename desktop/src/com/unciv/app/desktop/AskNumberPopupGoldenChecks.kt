package com.unciv.app.desktop

import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.ui.popup.AskNumberPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.activate

internal object AskNumberPopupGoldenChecks {
    fun open(screen: BaseScreen) {
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val oldFocus = screen.stage.keyboardFocus
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (steps in listOf(listOf(50, 500), listOf(5, 15))) {
                    val results = mutableListOf<Int>()
                    val popup = AskNumberPopup(
                        screen, label = "Enter the amount of gold offered to the other civilization",
                        defaultValue = "100", amountButtons = steps, bounds = 0..600,
                        errorText = "Enter a whole number within the available amount of gold.",
                        actionOnOk = { results.add(it) }
                    )
                    try {
                        screen.stage.addActor(popup)
                        popup.setFillParent(false)
                        popup.setSize(width, 480f)
                        popup.isVisible = true
                        val field = popup.innerTable.children.filterIsInstance<TextField>().single()
                        val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        check(screen.stage.keyboardFocus === field)
                        fun layout() {
                            popup.validate()
                            scroll.validate()
                            popup.innerTable.validate()
                            check(popup.innerTable.width <= width + 1f)
                            check(scroll.maxX <= 1f) { "Number prompt scrolls horizontally at $width: ${scroll.maxX}" }
                        }
                        fun adjust(value: Int) {
                            layout()
                            val button = popup.innerTable.findActor<Button>("numberAdjustment$value")
                            val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
                            for (eventType in listOf(InputEvent.Type.touchDown, InputEvent.Type.touchUp)) {
                                button.fire(InputEvent().apply {
                                    type = eventType
                                    stageX = point.x
                                    stageY = point.y
                                    pointer = 0
                                    this.button = Input.Buttons.LEFT
                                })
                            }
                        }
                        layout()
                        for (step in steps) {
                            field.text = "100"
                            adjust(step)
                            check(field.text.toInt() == (100 + step).coerceAtMost(600))
                            adjust(-step)
                            check(field.text == "100")
                            field.text = "599"
                            adjust(step)
                            check(field.text == "600")
                            field.text = "1"
                            adjust(-step)
                            check(field.text == "0")
                        }
                        field.text = "-"
                        adjust(steps.first())
                        check(field.text == "-")
                        buttons[1].activate()
                        check(results.isEmpty() && popup.parent != null)
                        val errorCells = popup.innerTable.cells.size
                        buttons[1].activate()
                        check(popup.innerTable.cells.size == errorCells)
                        layout()
                        field.text = "123"
                        buttons[1].activate()
                        check(results == listOf(123) && popup.parent == null)
                    } finally {
                        if (popup.parent != null) popup.close()
                    }
                }
            }
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            screen.stage.keyboardFocus = oldFocus
        }
        AskNumberPopup(screen, label = "Enter the amount of gold", defaultValue = "100",
            amountButtons = listOf(50, 500), bounds = 0..600).open(force = true)
    }
}
