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

            // Create a new instance of TroopArmyView for the popup
            val popupTroopView = TroopArmyView(troopView, true)
            popupTroopView.deselect()

            // Draw the avatar for the new instance
            //popupTroopView.drawAvatar()

            // Add the new instance to the popup
            add(popupTroopView).size(80f).padBottom(10f).padTop(10f).colspan(2).row()


            // Add "Split [Unit Name]" label
            val troopName = troopView.troopInfo.unitName  // Use unit name or default to "Troop"
            val label = "Split $troopName".toLabel(fontSize = 20)
            add(label).expandX().left().padBottom(10f).colspan(2).align(Align.center).row()


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
            add(sliderTable).growX().colspan(2).row()

            // Add "OK" button using the predefined addOKButton method
            addOKButton(
                text = "OK",
                style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java),
                validate = { true } // Always true, adjust if validation is needed
            ) {
                val leftCount = troopSlider.value.roundToInt()
                val rightCount = troopCount - leftCount
                onSplit(leftCount, rightCount) // Perform the split logic
            }.apply {
                actor.label.setFontScale(0.4f) // Adjust text size relative to the button
                actor.label.setAlignment(Align.center) // Center-align the text
                actor.pad(5f, 10f, 5f, 10f) // Add padding for better appearance
            }

            // Add "Close" button using the predefined addCloseButton method
            addCloseButton(
                text = "Close",
                style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
            ) {
                // Additional actions (if any) can be added here
            }.apply {
                actor.label.setFontScale(0.4f) // Adjust text size relative to the button
                actor.label.setAlignment(Align.center) // Center-align the text
                actor.pad(5f, 10f, 5f, 10f) // Add padding for better appearance
            }

        }
    }
}
