package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.ArmyManager
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.city.CityInfo
import com.unciv.ui.cityscreen.CityScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.SimplePopup
import com.unciv.ui.popup.SplitTroopPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick

/**
 * A view for displaying an [ArmyInfo] in the UI.
 * Handles rendering of troops or empty slots and interactions.
 */
class ArmyView(
    private val armyInfo: ArmyInfo?,
    private val armyManager: ArmyManager,
    private val screen: BaseScreen,
    private val slotSize: Float = 64f // Размер слотов по умолчанию
) : Table() {
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
        updateView(initialized = false)
    }

    /**
     * Updates the view to match the current state of the [ArmyInfo].
     */
    fun updateView(initialized: Boolean = true) {
        clear() // Remove existing actors from the table


        // Iterate through slots and create/update views
        armyInfo?.getAllTroops()?.forEachIndexed { index, troop ->
                val troopView = TroopArmyView(troop, this, slotSize = slotSize) // Pass ArmyView for interaction
                troopViewsArray[index] = troopView // Save to array
                add(troopView).size(slotSize).pad(5f)
        }

        pack() // Ensure layout is updated


        if (initialized && screen is CityScreen) {
            screen.city.cityStats.updateTileStats()
            screen.update(triggeredByArmyView = true)
        }
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
            val troopView = TroopArmyView(troop, this, slotSize = slotSize)
            troopViewsArray[index] = troopView
            add(troopView).size(slotSize).pad(5f)
        } else {
            // Add a placeholder image for the empty slot
            val emptySlotImage = ImageGetter.getImage("OtherIcons/Wait")
            add(emptySlotImage).size(slotSize).pad(5f)
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
        selectedTroop = null

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

    /** Expose screen for child views that need to open popups */
    fun getScreen(): BaseScreen = screen

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
     * Handles interactions with a troop slot in the army view.
     * This function processes troop selection, deselection, and actions such as troop splitting,
     * swapping, or combining between the selected slot and the clicked slot. The behavior depends
     * on whether the clicked troop is in the same army or a different army.
     *
     * - If no troop is selected, clicking a slot selects it.
     * - If the selected troop is clicked again, it is deselected.
     * - If a different troop is clicked, the function performs the appropriate action (split, swap, or combine).
     *
     * @param clickedTroopView The visual representation of the clicked troop slot.
     * @param isTroopSplitting A flag indicating whether a troop splitting action is required.
     */
    fun onTroopClicked(clickedTroopView: TroopArmyView, isTroopSplitting: Boolean) {

        // First of all let's handle first click, when just a selection without any other actions required
        if (    (exchangeArmyView == null && selectedTroop == null) || // If there is only one army and no selection in that army
                (exchangeArmyView != null && exchangeArmyView?.selectedTroop == null && selectedTroop == null)) // there is two armies, but no selection in both
        {
            deselectAllTroops() // Deselect all troops in the current army
            if(!clickedTroopView.isEmptySlot()) {
                clickedTroopView.select()
                selectedTroop = clickedTroopView
            }
            return
        }

        // Second, let's check if previously selected troop was clicked for the second time. We need to deselect it.
        val clickedIndex = troopViewsArray.indexOf(clickedTroopView)
        if (clickedIndex == -1) return // If the troop is not found, do nothing

        if (clickedIndex == getSelectedTroopIndex()){
            deselectAllTroops() // Deselect all troops in the current army
            selectedTroop = null
            return
        }

        // Ok, now we sure, that non-trivial action (swap, combine or split) is required. So let's determine target (clicked troop) and source (previously selected troop)

        // Target army is this army, because callBack comes from the clicked slot, and it is a target
        val targetArmyInfo = armyInfo ?: return
        val targetTroop = targetArmyInfo.getTroopAt(clickedIndex)
        val isSameArmy = (getSelectedTroopIndex() != null) // Check if interaction is within same army or different

        // Now let's declare source parameters
        val sourceIndex = if (isSameArmy) {
            getSelectedTroopIndex()
        } else {
            exchangeArmyView?.getSelectedTroopIndex()
        }

        if (sourceIndex == null) return // That should not be the case, but we need convertion from int? to int

        val sourceArmyView = if (isSameArmy) this else exchangeArmyView ?: return
        val sourceArmyInfo = if (isSameArmy) armyInfo else exchangeArmyView?.armyInfo ?: return
        val sourceTroop = sourceArmyInfo.getTroopAt(sourceIndex)
        val sourceTroopView = sourceArmyView.troopViewsArray[sourceIndex] ?: return


        if (isTroopSplitting) {
            // Open the split popup with updated logic for handling two non-empty slots
            SplitTroopPopup(
                screen = screen,
                sourceTroopView = sourceTroopView,
                targetTroopView = clickedTroopView, // Pass the clicked TroopArmyView as the target
                onSplit = { firstPart, secondPart ->
                    armyManager.splitTroop(
                        sourceArmy = sourceArmyInfo,
                        sourceIndex = sourceIndex,
                        targetArmy = targetArmyInfo,
                        targetIndex = clickedIndex,
                        finalFirstTroopCount = firstPart
                    )
                    updateView()
                    if (!isSameArmy)
                        exchangeArmyView?.updateView()
                }
            ).open()
        } else {
            val success = armyManager.swapOrCombineTroops(
                firstArmy = sourceArmyInfo,
                firstIndex = sourceIndex,
                secondArmy = targetArmyInfo,
                secondIndex = clickedIndex,
                combine = true
            )
            if (!success)
                println("Army swapping or combining is unsuccessful")
        }

        updateView() // Refresh the current army view
        deselectAllTroops() // Clear selection in the current army

        if (!isSameArmy) {
            exchangeArmyView?.updateView() // Refresh the exchange army view
            exchangeArmyView?.deselectAllTroops() // Clear selection in the exchange army
        }
    }

}
