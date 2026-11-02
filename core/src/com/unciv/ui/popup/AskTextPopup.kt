package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.ui.images.IconCircleGroup
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toLabel

/** Prompts for text, filters forbidden characters and validates before invoking actionOnOk. */
class AskTextPopup(
    screen: BaseScreen,
    label: String = "Please enter some text",
    icon: IconCircleGroup = ImageGetter.getImage("OtherIcons/Pencil").apply { this.color = Color.BLACK }.surroundWithCircle(80f),
    defaultText: String = "",
    errorText: String = "Invalid input! Please enter a different string.",
    maxLength: Int = 32,
    validate: (input: String) -> Boolean = { true },
    actionOnOk: (input: String) -> Unit = {},
) : Popup(screen) {

    val illegalChars = "[]{}\"\\<>"

    init {
        val royal = UncivGame.Current.settings.skin == "Minimal"
        var contentWidth = 0f
        if (royal) {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<ScrollPane>().single()
            val style = scroll.style
            val chrome = innerTable.padLeft + innerTable.padRight + 10f +
                maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
                (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            contentWidth = minOf(620f, stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            getCell(scroll).width(contentWidth + chrome)
        }
        val stacked = royal && contentWidth < 400f
        val columns = if (stacked) 1 else 2
        val wrapper = Table()
        val prompt = label.toLabel(fontColor = if (royal) Color.valueOf("453925") else Color.WHITE)
        if (royal) {
            prompt.setWrap(true)
            prompt.setAlignment(Align.center)
            if (stacked) {
                wrapper.add(icon).size(80f).padBottom(10f).row()
                wrapper.add(prompt).width(contentWidth)
            } else {
                wrapper.add(icon).size(80f).padRight(10f)
                wrapper.add(prompt).width(contentWidth - 90f)
            }
        } else {
            wrapper.add(icon).padRight(10f)
            wrapper.add(prompt)
        }
        val heading = add(wrapper).colspan(columns)
        if (royal) heading.width(contentWidth)
        heading.row()

        val nameField = UncivTextField.create(label, defaultText)
        nameField.textFieldFilter = TextField.TextFieldFilter { _, char -> char !in illegalChars }
        nameField.maxLength = maxLength
        val fieldCell = add(nameField).growX().colspan(columns)
        if (royal) fieldCell.width(contentWidth)
        fieldCell.row()

        val errorLabel = errorText.toLabel(
            fontColor = if (royal) Color.valueOf("8C2923") else Color.RED
        )
        if (royal) {
            errorLabel.setWrap(true)
            errorLabel.setAlignment(Align.center)
        }
        var errorAdded = false
        val close = addCloseButton()
        if (stacked) close.row()
        val ok = addOKButton(
            validate = {
                val errorFound = !validate(nameField.text)
                if (errorFound && !errorAdded) {
                    row()
                    val errorCell = add(errorLabel).colspan(columns).center()
                    if (royal) errorCell.width(contentWidth)
                    errorAdded = true
                }
                !errorFound
            }
        ) {
            actionOnOk(nameField.text)
        }
        if (royal) {
            val buttonWidth = if (stacked) contentWidth else (contentWidth - 10f) / 2f
            for (cell in listOf(close, ok)) {
                cell.actor.label.setWrap(true)
                cell.actor.label.setAlignment(Align.center)
                cell.width(buttonWidth).minHeight(58f).fillX()
            }
        } else {
            equalizeLastTwoButtonWidths()
        }
        keyboardFocus = nameField
    }
}
