package com.unciv.logic.army

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.unique.UniqueType

/**
 * Represents an army consisting of a fixed number of slots,
 * where each slot can hold a [TroopInfo] or be empty (null).
 *
 * An optional hero (leader) reference is maintained and passed to all troops.
 */
class ArmyInfo(
    @Transient
    var civInfo: CivilizationInfo = CivilizationInfo(),
    val maxSlots: Int = DEFAULT_ARMY_SIZE
) : IsPartOfGameInfoSerialization, Json.Serializable {

    companion object {
        // Default army size, can be configured globally
        const val DEFAULT_ARMY_SIZE = 4
    }

    // Array to hold troop slots (null means the slot is empty)
    private val troops: Array<TroopInfo?> = Array(maxSlots) { null }

    /**
     * Optional reference to the hero (MapUnit) leading this army.
     * This reference will be passed to all troops via setTransients.
     */
    @Transient
    var hero: com.unciv.logic.map.MapUnit? = null

    /** Convenience constructor to initialize army with a list of troops */
    constructor(civInfo: CivilizationInfo, vararg troops: Pair<String, Int>) : this(civInfo, maxSlots = maxOf(DEFAULT_ARMY_SIZE, troops.size)) {
        initializeTroops(troops)
    }

    // Default constructor
    constructor() : this(CivilizationInfo(), DEFAULT_ARMY_SIZE)

    /**
     * Sets transient properties for the army and passes the hero reference to all troops.
     *
     * @param civInfo0 The civilization info to be set.
     */
    fun setTransients(civInfo0: CivilizationInfo, hero0: MapUnit?) {
        civInfo = civInfo0
        hero = hero0
        troops.forEach { troop ->
            troop?.setTransients(civInfo, hero0)
        }
    }

    /** Initializes troops in slots from a list of pairs (name, count) */
    private fun initializeTroops(troops: Array<out Pair<String, Int>>) {
        for ((index, troop) in troops.withIndex()) {
            if (index >= maxSlots) break
            val (name, count) = troop
            // Create troop without hero; hero will be set later via setTransients.
            this.troops[index] = TroopInfo(name, count, civInfo, hero)
        }
        // Fill remaining slots with null if the number of troops is less than maxSlots
        for (index in troops.size until maxSlots) {
            this.troops[index] = null // Explicitly mark the slot as empty
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
        if (totalCount <= 0 || unitName.isBlank()) {
            throw IllegalArgumentException("Invalid unit name or total count")
        }
        for (i in troops.indices) {
            troops[i] = null
        }
        val troopsPerSlot = totalCount / maxSlots
        val remainder = totalCount % maxSlots
        for (i in troops.indices) {
            val countForThisSlot = troopsPerSlot + if (i < remainder) 1 else 0
            if (countForThisSlot > 0) {
                troops[i] = TroopInfo(unitName, countForThisSlot, civInfo, hero)
            }
        }
    }

    /**
     * Adds units to the army. If a unit of the same type exists, it increases its count.
     * If no such unit exists, it attempts to place the units in an empty slot.
     *
     * @param unitName The name of the unit to add.
     * @param amount The number of units to add.
     * @return True if the units were added successfully, false if no slot was available.
     */
    fun addUnits(unitName: String, amount: Int): Boolean {
        if (amount <= 0) return false
        for (slot in troops) {
            if (slot?.unitName == unitName) {
                slot.amount += amount
                return true
            }
        }
        val emptySlotIndex = troops.indexOfFirst { it == null }
        if (emptySlotIndex != -1) {
            troops[emptySlotIndex] = TroopInfo(unitName, amount, civInfo, hero)
            return true
        }
        return false
    }

    fun calculateFoodMaintenance(isInCity: Boolean): Float {
        var foodMaintenance = 0f
        troops.filterNotNull().forEach {
            if (!isInCity || !it.baseUnit.hasUnique(UniqueType.SelfFeeding))
                foodMaintenance += it.amount.toFloat() / 30f
        }
        return foodMaintenance
    }

    fun dismissByMostMaintenance() {
        val troopToDismiss = troops.maxBy { it?.amount ?: 0 } ?: return
        removeTroop(troopToDismiss)
    }

    /** Returns the troop at the given index or null if the slot is empty. */
    fun getTroopAt(index: Int): TroopInfo? {
        return troops.getOrNull(index)
    }

    /**
     * Checks if the given troop is part of this army.
     *
     * @param troop The troop to check.
     * @return True if the troop belongs to this army, false otherwise.
     */
    fun contains(troop: TroopInfo): Boolean {
        return troops.any { it == troop }
    }

    /**
     * Removes the specified troop from the army.
     *
     * @param troop The troop to remove.
     * @return True if the troop was successfully removed, false if not found.
     */
    fun removeTroop(troop: TroopInfo): Boolean {
        val index = troops.indexOfFirst { it == troop }
        if (index != -1) {
            troops[index] = null
            return true
        }
        return false
    }

    /** Sets a troop at the given index. */
    internal fun setTroopAt(index: Int, troop: TroopInfo?) {
        if (index in troops.indices) {
            troops[index] = troop
        }
    }

    /** Removes a troop from the given index and returns it. */
    internal fun removeTroopAt(index: Int): TroopInfo? {
        if (index in troops.indices) {
            val removedTroop = troops[index]
            troops[index] = null
            return removedTroop
        }
        return null
    }

    /** Adds a troop to the first available slot. Returns true if successful, false if full. */
    internal fun addTroop(troop: TroopInfo): Boolean {
        val emptySlotIndex = troops.indexOfFirst { it == null }
        return if (emptySlotIndex != -1) {
            troops[emptySlotIndex] = troop
            true
        } else {
            false
        }
    }

    /** Swaps two troops in the array by their indices. */
    internal fun swapTroops(index1: Int, index2: Int) {
        if (index1 in troops.indices && index2 in troops.indices) {
            val temp = troops[index1]
            troops[index1] = troops[index2]
            troops[index2] = temp
        }
    }

    fun getAllTroops(): Array<TroopInfo?> {
        return troops
    }

    // ===== Serialization Methods =====

    override fun write(json: Json) {
        json.writeArrayStart("slots")
        for (troop in troops) {
            json.writeValue(troop)
        }
        json.writeArrayEnd()
    }

    override fun read(json: Json, jsonData: JsonValue) {
        val slotArray = jsonData.get("slots")
        for (i in 0 until maxSlots) {
            val troopData = slotArray.get(i)
            troops[i] = if (troopData != null && troopData.has("amount")) {
                json.readValue(TroopInfo::class.java, troopData)
            } else {
                null
            }
        }
    }

    fun finishBattle() {
        troops.forEach { it?.finishBattle() }
    }

    /**
     * Creates a deep copy of the current slots array.
     * Each [TroopInfo] object is also deeply copied.
     *
     * @return A new array with the copied troop slots.
     */
    fun copySlots(): Array<TroopInfo?> {
        return troops.map { troop ->
            troop?.copy()
        }.toTypedArray()
    }

    fun clone(): ArmyInfo {
        val toReturn = ArmyInfo(civInfo)
        toReturn.copySlots()
        return toReturn
    }
}
