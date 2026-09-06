package com.unciv.ui.popup

import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.Align
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.roundToInt

/**
 * Popup for splitting a troop into two parts.
 * @param screen The screen from which the popup is opened.
 * @param troopView The TroopArmyView representing the troop.
 * @param onSplit Lambda that gets the split result: (leftTroopCount, rightTroopCount).
 */
class SplitTroopPopup(
    screen: BaseScreen,
    sourceTroopView: TroopArmyView,
    targetTroopView: TroopArmyView,
    onSplit: (Int, Int) -> Unit
) : Popup(screen) {

    init {
        val sourceTroop = sourceTroopView.troopInfo
        if (sourceTroop != null) {
            val sourceCount = sourceTroop.amount
            val targetCount = targetTroopView.troopInfo?.amount ?: 0
            val totalCount = sourceCount + targetCount
            val contentWidth = minOf(420f, screen.stage.width - 80f).coerceAtLeast(220f)

            innerTable.pad(24f).padTop(48f)
            val portrait = TroopArmyView(sourceTroopView, true, 80f, false)
            add(portrait).size(80f).colspan(2).padTop(8f).padBottom(12f).row()

            val title = "Split ${sourceTroop.unitName}".toLabel(fontSize = 20).apply {
                setAlignment(Align.center)
                setWrap(true)
            }
            add(title).width(contentWidth).colspan(2).padBottom(16f).row()

            val leftCountLabel = Label(sourceCount.toString(), BaseScreen.skin).apply {
                name = "splitLeftCount"
                setAlignment(Align.center)
            }
            val rightCountLabel = Label(targetCount.toString(), BaseScreen.skin).apply {
                name = "splitRightCount"
                setAlignment(Align.center)
            }
            val troopSlider = Slider(0f, totalCount.toFloat(), 1f, false, BaseScreen.skin).apply {
                name = "splitTroopSlider"
                value = targetCount.toFloat()
            }
            troopSlider.addListener(object :
                com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                override fun changed(
                    event: ChangeEvent?,
                    actor: com.badlogic.gdx.scenes.scene2d.Actor?
                ) {
                    val rightCount = troopSlider.value.roundToInt()
                    leftCountLabel.setText((totalCount - rightCount).toString())
                    rightCountLabel.setText(rightCount.toString())
                }
            })

            val countWidth = maxOf(44f, totalCount.toString().toLabel().prefWidth + 12f)
            val sliderTable = Table().apply {
                add(leftCountLabel).width(countWidth).padRight(8f)
                add(troopSlider).growX().minWidth(80f)
                add(rightCountLabel).width(countWidth).padLeft(8f)
            }
            add(sliderTable).width(contentWidth).colspan(2).padBottom(18f).row()

            val buttonStyle = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
            val ok = addOKButton(style = buttonStyle) {
                val rightCount = troopSlider.value.roundToInt()
                onSplit(totalCount - rightCount, rightCount)
            }
            val cancel = addCloseButton(text = "Close", style = buttonStyle)
            for (cell in listOf(ok, cancel)) {
                cell.actor.label.setFontScale(0.4f)
                cell.actor.label.setAlignment(Align.center)
                cell.actor.pad(5f, 10f, 5f, 10f)
                cell.width((contentWidth - 10f) / 2f).height(58f)
            }
        }
    }
}
