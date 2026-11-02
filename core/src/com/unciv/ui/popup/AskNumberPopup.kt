package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
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
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toLabel

/** Prompts for an integer with optional adjustment buttons and validation before confirmation. */
class AskNumberPopup(
    screen: BaseScreen,
    label: String = "Please enter a number",
    icon: IconCircleGroup = ImageGetter.getImage("OtherIcons/Pencil").apply { this.color = Color.BLACK }.surroundWithCircle(80f),
    defaultValue: String = "",
    amountButtons: List<Int> = listOf(),
    bounds: IntRange = IntRange(Int.MIN_VALUE, Int.MAX_VALUE),
    errorText: String = "Invalid input! Please enter a valid number.",
    validate: (input: Int) -> Boolean = { true },
    actionOnOk: (input: Int) -> Unit = {},
) : Popup(screen) {
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

        val nameField = UncivTextField.create(label, defaultValue)
        nameField.textFieldFilter = TextField.TextFieldFilter { _, char -> char.isDigit() || char == '-' }
        fun isValidInt(input: String) = input.toIntOrNull() != null
        fun clampInBounds(input: String): String {
            val int = input.toIntOrNull() ?: return input
            if (bounds.first > int) return bounds.first.toString()
            if (bounds.last < int) return bounds.last.toString()
            return input
        }
        nameField.onChange {
            nameField.text = clampInBounds(nameField.text)
        }

        fun valueButton(value: Int): Button {
            val caption = (if (value > 0) "+$value" else value.toString()).toLabel(
                fontColor = if (royal) Color.valueOf("FFF0CB") else Color.WHITE
            )
            if (royal) {
                caption.setWrap(true)
                caption.setAlignment(Align.center)
            }
            return Button(caption, skin).apply {
                name = "numberAdjustment$value"
                onClick {
                    if (isValidInt(nameField.text))
                        nameField.text = clampInBounds((nameField.text.toInt() + value).toString())
                }
            }
        }
        val centerTable = Table(skin)
        if (royal) {
            add(nameField).width(contentWidth).growX().colspan(columns).row()
            val values = amountButtons.reversed().map { -it } + amountButtons
            val gridColumns = (contentWidth / 100f).toInt().coerceIn(1, 4)
            val buttonWidth = (contentWidth - 10f * (gridColumns - 1)) / gridColumns
            values.forEachIndexed { index, value ->
                val cell = centerTable.add(valueButton(value)).width(buttonWidth).minHeight(48f)
                    .padBottom(5f)
                if (index % gridColumns != gridColumns - 1) cell.padRight(10f)
                if (index % gridColumns == gridColumns - 1) centerTable.row()
            }
            if (values.isNotEmpty()) add(centerTable).width(contentWidth).colspan(columns).row()
        } else {
            for (value in amountButtons.reversed()) centerTable.add(valueButton(-value)).pad(5f)
            centerTable.add(nameField).growX().pad(10f)
            add(centerTable).colspan(columns).row()
            for (value in amountButtons) centerTable.add(valueButton(value)).pad(5f)
        }

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
                val errorFound = !isValidInt(nameField.text) || !validate(nameField.text.toInt())
                if (errorFound && !errorAdded) {
                    row()
                    val cell = add(errorLabel).colspan(columns).center()
                    if (royal) cell.width(contentWidth)
                    errorAdded = true
                }
                !errorFound
            }
        ) {
            actionOnOk(nameField.text.toInt())
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
