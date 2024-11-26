package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(private val armyInfo: ArmyInfo) : Table() {

    // Массив для хранения TroopArmyView или null для пустых слотов
    private val troopViewsArray: Array<TroopArmyView?> = arrayOfNulls(armyInfo.getAllTroops().size)

    private var selectedTroop: TroopArmyView? = null // Ссылка на выделенный юнит


    init {
        // From CityContructionsTable
        bottom().left()
        pad(10f)
        // garrisonWidget.height(stageHeight / 8)
        //val tableHeight = stage.height / 8f
        //height = tableHeight
        updateView()
    }

    /**
     * Updates the view to match the current state of the [ArmyInfo].
     */
    fun updateView() {
        clear() // Remove existing actors from the table

        // Iterate through slots and create/update views
        armyInfo.getAllTroops().forEachIndexed { index, troop ->
            if (troop != null) {
                // If the troop exists, create its view
                val troopView = TroopArmyView(troop, this) // Передаем ArmyView для взаимодействия
                troopViewsArray[index] = troopView // Save to array
                add(troopView).size(64f).pad(5f)
            } else {
                // If the slot is empty, add a placeholder image
                val emptySlotImage = ImageGetter.getImage("OtherIcons/Wait")
                troopViewsArray[index] = null // Save null to array
                add(emptySlotImage).size(64f).pad(5f)
            }
        }

        pack() // Ensure layout is updated
    }

    /**
     * Updates a specific slot in the army view.
     * @param index The index of the slot to update.
     * @param troop The new troop to place in the slot, or null for an empty slot.
     */
    fun updateSlot(index: Int, troop: TroopInfo?) {
        if (index < 0 || index >= troopViewsArray.size) return // Check array bounds

        // Remove the current actor from the table
        troopViewsArray[index]?.remove()
        troopViewsArray[index] = null

        if (troop != null) {
            // Create a new TroopArmyView for the troop
            val troopView = TroopArmyView(troop, this)
            troopViewsArray[index] = troopView
            add(troopView).size(64f).pad(5f)
        } else {
            // Add a placeholder image for the empty slot
            val emptySlotImage = ImageGetter.getImage("OtherIcons/Wait")
            add(emptySlotImage).size(64f).pad(5f)
        }

        pack() // Update layout
    }

    /**
     * Deselects all troops in the army.
     */
    fun deselectAllTroops() {
        troopViewsArray.forEach { troopView ->
            troopView?.deselect() // Call deselect for all non-null TroopArmyView instances
        }
    }

    /**
     * Handles selection of a troop by index.
     * Ensures only one troop is selected at a time.
     * @param index The index of the troop to select.
     */
    fun selectTroop(index: Int) {
        deselectAllTroops() // Deselect all troops first
        troopViewsArray[index]?.select() // Select the specified troop
    }

    /**
     * Callback for when a TroopArmyView is clicked.
     * This method is called by TroopArmyView and manages selection logic.
     * @param clickedTroopView The clicked TroopArmyView instance.
     */
    fun onTroopClicked(clickedTroopView: TroopArmyView) {
        troopViewsArray.forEach { troopView ->
            if (troopView != clickedTroopView) {
                troopView?.deselect() // Deselect all other troops
            }
        }
        clickedTroopView.select() // Select the clicked troop
    }
}
