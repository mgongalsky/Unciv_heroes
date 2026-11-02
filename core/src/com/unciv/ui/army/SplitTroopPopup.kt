package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.roundToInt

/** Окно распределения бойцов между исходным и целевым отрядами. */
class SplitTroopPopup(
    screen: BaseScreen,
    sourceTroopView: TroopArmyView,
    targetTroopView: TroopArmyView,
    onSplit: (Int, Int) -> Unit
) : Popup(screen) {

    init {
        val sourceTroop = sourceTroopView.troopInfo
        if (sourceTroop != null) {
            val royal = UncivGame.Current.settings.skin == "Minimal"
            val ink = if (royal) Color.valueOf("453925") else Color.WHITE
            val sourceCount = sourceTroop.amount
            val targetCount = targetTroopView.troopInfo?.amount ?: 0
            val totalCount = sourceCount + targetCount
            val horizontalPadding = if (royal) 52f else 24f
            // Учитываем рамку, отступы ячеек и свободное место по краям экрана.
            val contentWidth = minOf(420f, screen.stage.width - 2f * horizontalPadding - 30f)
                .coerceAtLeast(160f)

            if (royal) innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(horizontalPadding).padTop(if (royal) 58f else 48f)
            val portrait = TroopArmyView(sourceTroopView, true, 80f, false)
            add(portrait).size(80f).colspan(2).padTop(8f).padBottom(12f).row()

            val title = "Split ${sourceTroop.unitName}".toLabel(fontColor = ink, fontSize = 20).apply {
                if (royal && BaseScreen.skin.has("royal-display", BitmapFont::class.java)) {
                    style = Label.LabelStyle(style).apply {
                        font = BaseScreen.skin.get("royal-display", BitmapFont::class.java)
                    }
                }
                setAlignment(Align.center)
                setWrap(true)
            }
            add(title).width(contentWidth).colspan(2).padBottom(16f).row()

            val countStyle = Label.LabelStyle(BaseScreen.skin.get(Label.LabelStyle::class.java)).apply {
                fontColor = ink.cpy()
            }
            val leftCountLabel = Label(sourceCount.toString(), countStyle).apply {
                name = "splitLeftCount"
                setAlignment(Align.center)
            }
            val rightCountLabel = Label(targetCount.toString(), countStyle).apply {
                name = "splitRightCount"
                setAlignment(Align.center)
            }
            val troopSlider = Slider(0f, totalCount.toFloat(), 1f, false, BaseScreen.skin).apply {
                name = "splitTroopSlider"
                value = targetCount.toFloat()
            }
            troopSlider.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
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
