package com.unciv.logic.army

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo

/**
 * Represents an army consisting of a fixed number of slots,
 * where each slot can hold a [TroopInfo] or be empty (null).
 */
class ArmyInfo(
    var civInfo: CivilizationInfo,
    private val maxSlots: Int = DEFAULT_ARMY_SIZE
) : IsPartOfGameInfoSerialization, Json.Serializable {

    companion object {
        // Default army size, can be configured globally
        const val DEFAULT_ARMY_SIZE = 4
    }

    // Array to hold troop slots (null means the slot is empty)
    private val slots: Array<TroopInfo?> = Array(maxSlots) { null }


    /** Convenience constructor to initialize army with a list of troops */
    constructor(civInfo: CivilizationInfo, vararg troops: Pair<String, Int>) : this(civInfo, maxSlots = maxOf(DEFAULT_ARMY_SIZE, troops.size)) {
        initializeTroops(troops)
    }

    /** Initializes troops in slots from a list of pairs (name, count) */
    private fun initializeTroops(troops: Array<out Pair<String, Int>>) {
        for ((index, troop) in troops.withIndex()) {
            if (index >= maxSlots) break
            val (name, count) = troop
            slots[index] = TroopInfo(name, count, civInfo)
        }
        // Fill remaining slots with null if the number of troops is less than maxSlots
        for (index in troops.size until maxSlots) {
            slots[index] = null // Explicitly mark the slot as empty
        }
    }

    /** Convenience constructor to initialize army with a list of troops */
    constructor(civInfo: CivilizationInfo, unitName: String, totalCount: Int) : this(civInfo, maxSlots = DEFAULT_ARMY_SIZE) {
        fillArmy(unitName, totalCount)
    }
    /**
     * Fills the army slots with troops of the given name and total count.
     * Distributes the total count evenly across all slots, with differences of at most 1.
     *
     * @param unitName The name of the troop unit to fill the army with.
     * @param totalCount The total number of troops to distribute across all slots.
     */
    fun fillArmy(unitName: String, totalCount: Int) {
        // Validate inputs
        if (totalCount <= 0 || unitName.isBlank()) {
            throw IllegalArgumentException("Invalid unit name or total count")
        }

        // Clear current slots
        for (i in slots.indices) {
            slots[i] = null
        }

        // Calculate even distribution of troops across slots
        val troopsPerSlot = totalCount / maxSlots
        val remainder = totalCount % maxSlots

        // Distribute troops to each slot
        for (i in slots.indices) {
            val countForThisSlot = troopsPerSlot + if (i < remainder) 1 else 0
            if (countForThisSlot > 0) {
                slots[i] = TroopInfo(unitName, countForThisSlot, civInfo)
            }
        }
    }


    /** Returns the troop at the given index or null if the slot is empty. */
    fun getTroopAt(index: Int): TroopInfo? {
        return slots.getOrNull(index)
    }

    /** Sets a troop at the given index. If the index is out of bounds, nothing happens. */
    internal fun setTroopAt(index: Int, troop: TroopInfo?) {
        if (index in slots.indices) {
            slots[index] = troop
        }
    }

    /** Removes a troop from the given index (makes the slot empty) and returns it. */
    internal fun removeTroopAt(index: Int): TroopInfo? {
        if (index in slots.indices) {
            val removedTroop = slots[index]
            slots[index] = null
            return removedTroop
        }
        return null
    }

    /** Adds a troop to the first available slot. Returns true if successful, false if full. */
    internal fun addTroop(troop: TroopInfo): Boolean {
        val emptySlotIndex = slots.indexOfFirst { it == null }
        return if (emptySlotIndex != -1) {
            slots[emptySlotIndex] = troop
            true
        } else {
            false
        }
    }

    /** Swaps two troops in the array by their indices. */
    internal fun swapTroops(index1: Int, index2: Int) {
        if (index1 in slots.indices && index2 in slots.indices) {
            val temp = slots[index1]
            slots[index1] = slots[index2]
            slots[index2] = temp
        }
    }

    fun getAllTroops(): Array<TroopInfo?> {
        return slots // Возвращаем внутренний массив слотов
    }

    // ===== Serialization Methods =====

    override fun write(json: Json) {
        json.writeArrayStart("slots")
        for (troop in slots) {
            json.writeValue(troop) // null values are handled automatically
        }
        json.writeArrayEnd()
    }

    override fun read(json: Json, jsonData: JsonValue) {
        val slotArray = jsonData.get("slots")
        for (i in 0 until maxSlots) {
            val troopData = slotArray.get(i)
            slots[i] = if (troopData != null) json.readValue(TroopInfo::class.java, troopData) else null
        }
    }
}
