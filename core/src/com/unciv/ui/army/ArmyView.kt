package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(private val armyInfo: ArmyInfo) : Table() {

    /** Callback for when a troop slot is clicked. Provides the index of the clicked slot. */
    var onTroopClick: ((Int) -> Unit)? = null

    init {
        updateView()
    }

    /**
     * Updates the view to match the current state of the [ArmyInfo].
     */
    fun updateView() {
        clear() // Remove existing actors from the table

        // Access slots directly using `armyInfo.getAllTroops()`
        armyInfo.getAllTroops().forEachIndexed { index, troop ->
            val troopView = if (troop != null) {
                // Create a view for an occupied slot
                TroopArmyView(troop)
            } else {
                // Create a placeholder for an empty slot
                EmptySlotView()
            }

            // Attach a click listener to each slot
            troopView.onClick { onTroopClick?.invoke(index) }

            // Add the slot view to the table with spacing
            add(troopView).size(64f).pad(5f) // Customize size and padding as needed
        }

        // Ensure layout is updated
        pack()
    }
}
