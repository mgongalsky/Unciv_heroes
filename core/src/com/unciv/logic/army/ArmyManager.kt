package com.unciv.logic.army

class ArmyManager(
    private val army1: ArmyInfo, // The first army
    private val army2: ArmyInfo? = null // The second army for exchange operations (optional)
) {


    // Not tested
    /**
     * Adds a troop to the specified army.
     * @param troop The troop to add.
     * @param toArmy1 If true, adds to army1; otherwise, to army2.
     * @return True if the troop was added successfully, false otherwise.
     */
    fun addTroop(troop: TroopInfo, toArmy1: Boolean = true): Boolean {
        val targetArmy = if (toArmy1) army1 else army2 ?: return false
        return targetArmy.addTroop(troop)
    }

    // Not tested
    /**
     * Removes a troop from the specified army.
     * @param index The index of the troop to remove.
     * @param fromArmy1 If true, removes from army1; otherwise, from army2.
     * @return The removed troop, or null if the operation failed.
     */
    fun removeTroop(index: Int, fromArmy1: Boolean = true): TroopInfo? {
        val targetArmy = if (fromArmy1) army1 else army2 ?: return null
        return targetArmy.removeTroopAt(index)
    }



    /**
     * Retrieves a troop by index from the specified army.
     * @param index The index of the troop to retrieve.
     * @param fromArmy1 If true, retrieves from army1; otherwise, from army2.
     * @return The troop at the specified index, or null if not found.
     */
    fun getTroop(index: Int, fromArmy1: Boolean = true): TroopInfo? {
        val targetArmy = if (fromArmy1) army1 else army2 ?: return null
        return targetArmy.getTroopAt(index)
    }

    /**
     * Swaps or combines two troops between the same or different armies.
     * @param firstArmy The first army involved in the operation.
     * @param firstIndex The index of the troop in the first army.
     * @param secondArmy The second army involved in the operation.
     * @param secondIndex The index of the troop in the second army.
     * @param combine If true, attempts to combine troops if they are of the same type; otherwise, swaps them.
     * @return True if the operation was successful, false otherwise.
     */
    fun swapOrCombineTroops(
        firstArmy: ArmyInfo,
        firstIndex: Int,
        secondArmy: ArmyInfo,
        secondIndex: Int,
        combine: Boolean = true
    ): Boolean {
        // Retrieve troops from both armies
        val firstTroop = firstArmy.getTroopAt(firstIndex)
        val secondTroop = secondArmy.getTroopAt(secondIndex)

        // If both slots are empty
        if (firstTroop == null && secondTroop == null) {
            println("Error: two empty slots are selected")
            return false
        }

        if (combine && firstTroop != null && secondTroop != null && firstTroop.unitName == secondTroop.unitName) {
            // Combine troops if they are of the same type and combining is enabled
            firstTroop.currentAmount += secondTroop.currentAmount
            secondArmy.removeTroopAt(secondIndex)
            return true
        } else {
            // Perform the swap
            if (secondTroop != null) {
                firstArmy.setTroopAt(firstIndex, secondTroop)
            } else {
                firstArmy.removeTroopAt(firstIndex)
            }

            if (firstTroop != null) {
                secondArmy.setTroopAt(secondIndex, firstTroop)
            } else {
                secondArmy.removeTroopAt(secondIndex)
            }

            return true
        }
    }
}
