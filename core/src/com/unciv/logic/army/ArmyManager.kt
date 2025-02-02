package com.unciv.logic.army

/**
 * Manages operations on armies, including swapping, combining, and splitting troops.
 * Ensures that hero references are updated accordingly when troops move between armies.
 */
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
        // Update the troop's hero reference to match the target army
        troop.hero = targetArmy.hero
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
     * When swapping, the hero reference is updated to the target army's hero.
     *
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

        // If both slots are empty, nothing to do.
        if (firstTroop == null && secondTroop == null) {
            println("Error: two empty slots are selected")
            return false
        }

        if (combine && firstTroop != null && secondTroop != null && firstTroop.unitName == secondTroop.unitName) {
            // Combine troops if they are of the same type.
            secondTroop.amount += firstTroop.amount
            firstArmy.removeTroopAt(firstIndex)
            // Update hero reference in the combined troop to match the target army (secondArmy)
            secondTroop.hero = secondArmy.hero
            return true
        } else {
            // Perform the swap
            if (secondTroop != null) {
                firstArmy.setTroopAt(firstIndex, secondTroop)
                // Update hero reference: troop now belongs to firstArmy.
                secondTroop.hero = firstArmy.hero
            } else {
                firstArmy.removeTroopAt(firstIndex)
            }

            if (firstTroop != null) {
                secondArmy.setTroopAt(secondIndex, firstTroop)
                // Update hero reference: troop now belongs to secondArmy.
                firstTroop.hero = secondArmy.hero
            } else {
                secondArmy.removeTroopAt(secondIndex)
            }
            return true
        }
    }

    /**
     * Splits a troop into two separate slots within the same or different armies.
     * The newly created troop will receive the hero reference from the target army.
     *
     * @param sourceArmy The army containing the troop to split.
     * @param sourceIndex The index of the troop to split in the source army.
     * @param targetArmy The army where the split portion will be placed.
     * @param targetIndex The index in the target army where the split portion will go.
     * @param finalFirstTroopCount The final number of units that should remain in the source troop.
     * @return True if the split was successful, false otherwise.
     */
    fun splitTroop(
        sourceArmy: ArmyInfo,
        sourceIndex: Int,
        targetArmy: ArmyInfo,
        targetIndex: Int,
        finalFirstTroopCount: Int
    ): Boolean {
        val sourceTroop = sourceArmy.getTroopAt(sourceIndex) ?: return false

        // Validate the final count for the source troop
        if (finalFirstTroopCount < 0 || finalFirstTroopCount >= sourceTroop.amount) return false

        val targetTroop = targetArmy.getTroopAt(targetIndex)
        val splitAmount = sourceTroop.amount - finalFirstTroopCount

        if (targetTroop == null) {
            // Update the source troop count
            sourceTroop.amount = finalFirstTroopCount
            sourceArmy.setTroopAt(sourceIndex, sourceTroop)

            // Create a new troop for the target slot with the split amount,
            // passing along civInfo and setting hero from targetArmy.
            val newTroop = TroopInfo(sourceTroop.unitName, splitAmount, sourceArmy.civInfo, targetArmy.hero)
            targetArmy.setTroopAt(targetIndex, newTroop)
        } else if (targetTroop.unitName == sourceTroop.unitName) {
            // Combine with the existing troop in the target slot.
            targetTroop.amount += splitAmount
            sourceTroop.amount = finalFirstTroopCount
            if (sourceTroop.amount == 0) {
                sourceArmy.removeTroopAt(sourceIndex)
            }
            // Update target troop's hero reference (if needed).
            targetTroop.hero = targetArmy.hero
        } else {
            return false // Invalid target: different troop types.
        }
        return true
    }
}
