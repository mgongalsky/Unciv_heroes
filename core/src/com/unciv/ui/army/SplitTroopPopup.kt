package com.unciv.ui.popup

import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
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
    troopView: TroopArmyView,
    onSplit: (Int, Int) -> Unit
) : Popup(screen) {

    init {
        // Check if troopInfo is null
        if (troopView.troopInfo == null) {
            remove() // Close popup if troopInfo is null
        } else {

            val troopCount = troopView.troopInfo.currentAmount

            // Set minimum popup size
            this.setSize(400f, 300f)

            // Wrapper for avatar and label
            val wrapper = Table()
            wrapper.defaults().pad(10f)

            // Add avatar from TroopArmyView
            troopView.drawAvatar()
            wrapper.add(troopView).size(80f).padBottom(10f).row()

            // Add "Adjust troop split" label
            val label = "Adjust troop split".toLabel(fontSize = 20)
            wrapper.add(label).expandX().left().padBottom(10f).row()

            // Create labels for left and right troop counts
            val leftCountLabel = Label("0", BaseScreen.skin)
            val rightCountLabel = Label(troopCount.toString(), BaseScreen.skin)

            // Create slider
            val troopSlider = Slider(0f, troopCount.toFloat(), 1f, false, BaseScreen.skin)
            troopSlider.value = 0f
            troopSlider.addListener { _ ->
                val leftCount = troopSlider.value.roundToInt()
                val rightCount = troopCount - leftCount
                leftCountLabel.setText(leftCount.toString())
                rightCountLabel.setText(rightCount.toString())
                false
            }

            // Create table for slider and labels
            val sliderTable = Table()
            sliderTable.defaults().pad(5f)
            sliderTable.add(leftCountLabel).padRight(10f)
            sliderTable.add(troopSlider).growX().pad(5f)
            sliderTable.add(rightCountLabel).padLeft(10f)
            wrapper.add(sliderTable).growX().row()

            // Add wrapper to popup
            add(wrapper).expand().fill().row()

            // Create button table
            val buttonTable = Table()
            buttonTable.defaults().pad(10f)

            // Add "Close" button
            val closeButton = TextButton("Close", BaseScreen.skin)
            closeButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    remove() // Close popup when clicked
                }
            })
            buttonTable.add(closeButton).padRight(10f)

            // Add "OK" button
            val okButton = TextButton("OK", BaseScreen.skin)
            okButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    val leftCount = troopSlider.value.roundToInt()
                    val rightCount = troopCount - leftCount
                    onSplit(leftCount, rightCount) // Perform action on confirmation
                    remove() // Close popup
                }
            })
            buttonTable.add(okButton)

            // Add button table to popup
            add(buttonTable).expandX().fillX().padTop(20f).row()
        }
    }
}
