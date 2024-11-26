package com.unciv.logic.army

class ArmyManager(
    private val army1: ArmyInfo, // The first army
    private val army2: ArmyInfo? = null // The second army for exchange operations (optional)
) {
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
     * Transfers a troop between armies.
     * @param index The index of the troop to transfer.
     * @param toArmy1 If true, transfers to army1; otherwise, to army2.
     * @return True if the transfer was successful, false otherwise.
     */
    fun transferTroop(index: Int, toArmy1: Boolean): Boolean {
        if (army2 == null) return false // Transfer is impossible if there is no second army

        val fromArmy = if (toArmy1) army2 else army1
        val toArmy = if (toArmy1) army1 else army2

        val troop = fromArmy.removeTroopAt(index) ?: return false
        return toArmy.addTroop(troop)
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
     * Swaps two troops within the same army.
     * @param firstIndex The index of the first troop.
     * @param secondIndex The index of the second troop.
     * @param isArmy1 If true, the operation is performed on army1; otherwise, on army2.
     * @return True if the swap was successful, false otherwise.
     */
    fun swapTroops(firstIndex: Int, secondIndex: Int, isArmy1: Boolean): Boolean {
        val targetArmy = if (isArmy1) army1 else army2 ?: return false

        // Retrieve both troops
        val firstTroop = targetArmy.getTroopAt(firstIndex)
        val secondTroop = targetArmy.getTroopAt(secondIndex)

        // If both slots are empty
        if (firstTroop == null && secondTroop == null) {
            println("Error: two empty slot are selected")
            return false
        }

        // Set them in their new positions
        if(secondTroop != null)
            targetArmy.setTroopAt(firstIndex, secondTroop)
        else
            targetArmy.removeTroopAt(firstIndex)
        if(firstTroop != null)
            targetArmy.setTroopAt(secondIndex, firstTroop)
        else
            targetArmy.removeTroopAt(secondIndex)



        return true
    }
}
