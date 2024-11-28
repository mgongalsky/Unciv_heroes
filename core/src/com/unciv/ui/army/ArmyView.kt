package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(private val armyInfo: ArmyInfo?, private val armyManager: ArmyManager) : Table() {
    // !! Note here, ArmyInfo is nullable, which is weird, but unfortunately it's the easiest way to handle the bug
    // of libGDX, which goes crazy if you feed it nullable (but not null!) Table-derived class.
    // So visitingArmyView exists always, but there might be no visiting hero ) Crazy, but it works.
    // Need to be corrected if the bug in libGDX fixed

    // Array to store TroopArmyView or null for empty slots
    private val troopViewsArray: Array<TroopArmyView?> =
            Array(armyInfo?.getAllTroops()?.size ?: 0) { null }

    private var selectedTroop: TroopArmyView? = null // Reference to the selected troop

    // Reference to another ArmyView for potential exchanges
    private var exchangeArmyView: ArmyView? = null

    init {
        // Align the table and set padding
        //right().bottom()

        //bottom().left()
        //pad(10f)
        updateView()
    }

    /**
     * Updates the view to match the current state of the [ArmyInfo].
     */
    fun updateView() {
        clear() // Remove existing actors from the table


        // Iterate through slots and create/update views
        armyInfo?.getAllTroops()?.forEachIndexed { index, troop ->
                val troopView = TroopArmyView(troop, this) // Pass ArmyView for interaction
                troopViewsArray[index] = troopView // Save to array
                add(troopView).size(64f).pad(5f)
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
     * Returns the index of the selected troop in the army.
     * @return The index of the selected troop, or null if no troop is selected.
     */
    internal fun getSelectedTroopIndex(): Int? {
        // Если selectedTroop равен null, значит ничего не выделено
        if (selectedTroop == null) return null

        // Ищем индекс выделенного отряда в массиве
        return troopViewsArray.indexOf(selectedTroop).takeIf { it != -1 }
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
     * Sets the reference to another ArmyView for exchanges.
     * @param armyView The other ArmyView.
     */
    fun setExchangeArmyView(armyView: ArmyView?) {
        exchangeArmyView = armyView
    }


    /**
     * Checks if the ArmyInfo is null.
     * @return True if the army is not null,  otherwise, false.
     */
    fun isHero(): Boolean {
        return armyInfo != null
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
     * This method is called by TroopArmyView and delegates to ArmyManager.
     * Handles cross-army selection if exchangeArmyView is set.
     * @param clickedTroopView The clicked TroopArmyView instance.
     */
    fun onTroopClicked(clickedTroopView: TroopArmyView) {
        val clickedIndex = troopViewsArray.indexOf(clickedTroopView)
        if (clickedIndex == -1) return // If the troop is not found, do nothing

        if (exchangeArmyView != null) {
            // Handle cross-army interaction
            val exchangeSelectedIndex = exchangeArmyView?.getSelectedTroopIndex()
            if (exchangeSelectedIndex != null) {
                // Perform troop swap between the two armies
                val currentArmyInfo = armyInfo ?: return
                val exchangeArmyInfo = exchangeArmyView?.armyInfo ?: return

                val success = armyManager.swapTroops(
                    currentArmyInfo,
                    clickedIndex,
                    exchangeArmyInfo,
                    exchangeSelectedIndex
                )

                if (success) {
                    updateView() // Refresh the current army view
                    exchangeArmyView?.updateView() // Refresh the exchange army view
                    exchangeArmyView?.deselectAllTroops() // Clear selection in the exchange army
                    deselectAllTroops() // Clear selection in the current army
                }
            } else {
                // Check if there's a selected troop in the current army
                val currentSelectedIndex = getSelectedTroopIndex()
                if (currentSelectedIndex != null) {
                    // Swap the selected troop in the current army with the clicked troop
                    val currentArmyInfo = armyInfo ?: return
                    val success = armyManager.swapTroops(
                        currentArmyInfo,
                        currentSelectedIndex,
                        currentArmyInfo,
                        clickedIndex
                    )
                    if (success) {
                        updateView() // Refresh the current army view
                    }
                    deselectAllTroops() // Clear selection in the current army
                } else {
                    // No troop selected in the current army, select the clicked troop here
                    deselectAllTroops() // Deselect all troops in the current army
                    clickedTroopView.select()
                    selectedTroop = clickedTroopView
                }
            }
        } else {
            // Handle single-army interaction
            if (selectedTroop != null) {
                val selectedIndex = troopViewsArray.indexOf(selectedTroop)
                if (selectedIndex != -1 && selectedIndex != clickedIndex) {
                    // Swap the selected troop with the clicked troop within the same army
                    val currentArmyInfo = armyInfo ?: return

                    val success = armyManager.swapTroops(
                        currentArmyInfo,
                        selectedIndex,
                        currentArmyInfo,
                        clickedIndex
                    )

                    if (success) {
                        updateView() // Refresh the view to reflect the change
                    }
                }
                deselectAllTroops() // Clear selection in the current army
            } else {
                // If no troop is selected, select the clicked troop
                if (!clickedTroopView.isEmptySlot()) {
                    deselectAllTroops() // Deselect all troops in the current army
                    clickedTroopView.select()
                    selectedTroop = clickedTroopView
                }
            }
        }
    }



}
