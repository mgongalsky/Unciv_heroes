package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.toLabel

/** Confirmation dialog with cancel/confirm actions and their keyboard shortcuts. */
open class ConfirmPopup(
    stageToShowOn: Stage,
    question: String,
    confirmText: String,
    isConfirmPositive: Boolean = false,
    restoreDefault: () -> Unit = {},
    action: () -> Unit
) : Popup(stageToShowOn) {

    constructor(
        screen: BaseScreen,
        question: String,
        confirmText: String,
        isConfirmPositive: Boolean = false,
        restoreDefault: () -> Unit = {},
        action: () -> Unit
    ) : this(screen.stage, question, confirmText, isConfirmPositive, restoreDefault, action)

    private val promptLabel = question.toLabel()

    init {
        val royal = UncivGame.Current.settings.skin == "Minimal"
        promptLabel.setAlignment(Align.center)
        if (royal) {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            promptLabel.style = com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(
                promptLabel.style
            ).apply { fontColor = Color.valueOf("453925") }
        }

        val scroll = children.filterIsInstance<ScrollPane>().single()
        // getScrollBarWidth() is zero before vertical scrolling becomes active.
        // Read the style dimensions so wrapped content reserves space on its first layout.
        val scrollStyle = scroll.style
        val barWidth = maxOf(
            scrollStyle.vScroll?.minWidth ?: 0f,
            scrollStyle.vScrollKnob?.minWidth ?: 0f
        )
        val scrollBackground = scrollStyle.background
        val scrollInsets = (scrollBackground?.leftWidth ?: 0f) +
            (scrollBackground?.rightWidth ?: 0f)
        val horizontalChrome = innerTable.padLeft + innerTable.padRight + 10f +
            barWidth + scrollInsets
        val contentWidth = minOf(620f, stageToShowOn.width - 24f - horizontalChrome)
            .coerceAtLeast(1f)
        if (royal) getCell(scroll).width(contentWidth + horizontalChrome)
        val stacked = royal && contentWidth < 400f
        val columns = if (stacked) 1 else 2
        val promptCell = add(promptLabel).colspan(columns)
        if (royal) {
            promptLabel.setWrap(true)
            promptCell.width(contentWidth).padBottom(16f)
        }
        promptCell.row()

        val cancel = addCloseButton("Cancel", KeyCharAndCode('n'), action = restoreDefault)
        if (stacked) cancel.row()
        val confirmStyleName = if (isConfirmPositive) "positive" else "negative"
        val confirmStyle = BaseScreen.skin.get(confirmStyleName, TextButtonStyle::class.java)
        val confirm = addOKButton(confirmText, KeyCharAndCode('y'), confirmStyle, action = action)
        if (royal) {
            val buttonWidth = if (stacked) contentWidth else (contentWidth - 10f) / 2f
            for (cell in listOf(cancel, confirm)) {
                cell.actor.label.setWrap(true)
                cell.actor.label.setAlignment(Align.center)
                cell.width(buttonWidth).minHeight(58f).fillX()
            }
        } else {
            equalizeLastTwoButtonWidths()
        }
    }
}
