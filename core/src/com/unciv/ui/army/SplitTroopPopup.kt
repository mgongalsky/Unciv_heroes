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
        // Check if sourceTroopView contains valid data
        if (sourceTroopView.troopInfo == null) {
            remove() // Close the popup if there is no troop info
        } else {
            val sourceCount = sourceTroopView.troopInfo.currentAmount
            val targetCount = if (targetTroopView.isEmptySlot()) 0 else targetTroopView.troopInfo?.currentAmount ?: 0
            val totalCount = sourceCount + targetCount

            // Set the minimum size of the popup
            this.setSize(400f, 300f)

            // Create a visual component for sourceTroopView
            val popupSourceView = TroopArmyView(sourceTroopView, true)
            popupSourceView.deselect()

            // Add the avatar of the source troop
            add(popupSourceView).size(80f).padBottom(10f).padTop(10f).colspan(2).row()

            // Add a header label
            val troopName = sourceTroopView.troopInfo.unitName
            val label = "Split $troopName".toLabel(fontSize = 20)
            add(label).expandX().left().padBottom(10f).colspan(2).align(Align.center).row()

            // Current values for troop distribution
            val leftCountLabel = Label(sourceCount.toString(), BaseScreen.skin)
            val rightCountLabel = Label(targetCount.toString(), BaseScreen.skin)

            // Create a slider
            val troopSlider = Slider(0f, totalCount.toFloat(), 1f, false, BaseScreen.skin)
            troopSlider.value = targetCount.toFloat() // Initial position is the current targetCount
            troopSlider.addListener { _ ->
                val rightCount = troopSlider.value.roundToInt()
                val leftCount = totalCount - rightCount

                leftCountLabel.setText(leftCount.toString())
                rightCountLabel.setText(rightCount.toString())
                false
            }

            // Table for the slider and labels
            val sliderTable = Table()
            sliderTable.defaults().pad(5f)
            sliderTable.add(leftCountLabel).padRight(10f)
            sliderTable.add(troopSlider).growX().pad(5f)
            sliderTable.add(rightCountLabel).padLeft(10f)
            add(sliderTable).growX().colspan(2).row()

            // Add "OK" button
            addOKButton(
                text = "OK",
                style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java),
                validate = { true } // Always true; add validation if necessary
            ) {
                val rightCount = troopSlider.value.roundToInt()
                val leftCount = totalCount - rightCount
                onSplit(leftCount, rightCount) // Perform the split logic
            }.apply {
                actor.label.setFontScale(0.4f)
                actor.label.setAlignment(Align.center)
                actor.pad(5f, 10f, 5f, 10f)
            }

            // Add "Close" button
            addCloseButton(
                text = "Close",
                style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
            ).apply {
                actor.label.setFontScale(0.4f)
                actor.label.setAlignment(Align.center)
                actor.pad(5f, 10f, 5f, 10f)
            }
        }
    }
}
