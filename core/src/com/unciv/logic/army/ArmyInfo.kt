package com.unciv.logic.army

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import com.unciv.models.GameConstants
import com.unciv.pure.application.army.AddUnitsUseCase
import com.unciv.pure.application.army.CalculateArmyFoodMaintenanceUseCase
import com.unciv.pure.application.army.DismissByMostMaintenanceUseCase
import com.unciv.pure.application.army.FillArmyUseCase
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class ArmyInfo(
    @Transient var civInfo: CivilizationInfo = CivilizationInfo(),
    val maxSlots: Int = _defaultMaxSlots ?: GameConstants.armySize,
) : IsPartOfGameInfoSerialization, Json.Serializable, IArmy, KoinComponent {

    companion object {
        private var _defaultMaxSlots: Int? = null
        fun setTestingMaxSlots(slots: Int) {
            _defaultMaxSlots = slots
        }

        fun resetTestingMaxSlots() {
            _defaultMaxSlots = null
        }
    }

    private val army = Army(maxSlots)

    @delegate:Transient
    private val troopSource: ITroopDefinitionSource by inject()

    private val troops get() = army.getAllTroops()

    @Transient
    var hero: MapUnit? = null

    constructor(civInfo: CivilizationInfo, vararg troops: Pair<String, Int>) :
            this(civInfo, maxOf(_defaultMaxSlots ?: GameConstants.armySize, troops.size)) {
        initializeTroops(troops)
    }

    /** Side-effect-free constructor used by GDX Json; real ownership is restored by setTransients. */
    constructor() : this(MapUnit.monsterCivInfo, _defaultMaxSlots ?: GameConstants.armySize)

    constructor(civInfo: CivilizationInfo, unitName: String, totalCount: Int) :
            this(civInfo, GameConstants.armySize) {
        fillArmy(unitName, totalCount)
    }

    fun setTransients(civInfo0: CivilizationInfo, hero0: MapUnit?) {
        civInfo = civInfo0
        hero = hero0
    }

    private fun initializeTroops(initialTroops: Array<out Pair<String, Int>>) {
        for ((index, troop) in initialTroops.withIndex()) {
            if (index >= maxSlots) break
            troops[index] = TroopFactory.create(troop.first, troop.second, troopSource)
        }
        for (index in initialTroops.size until maxSlots) troops[index] = null
    }

    fun fillArmy(unitName: String, totalCount: Int) =
            FillArmyUseCase.execute(this, unitName, totalCount, troopSource)

    fun addUnits(unitName: String, amount: Int): Boolean =
            AddUnitsUseCase.execute(this, unitName, amount, troopSource)

    fun calculateFoodMaintenance(isInCity: Boolean): Float =
            CalculateArmyFoodMaintenanceUseCase.execute(this, isInCity)

    fun dismissByMostMaintenance() = DismissByMostMaintenanceUseCase.execute(this)

    fun getTroopAt(index: Int): Troop? = troops.getOrNull(index)
    fun contains(troop: Troop): Boolean = army.contains(troop)
    override fun getAllTroops(): Array<Troop?> = army.getAllTroops()
    override fun removeTroop(troop: Troop): Boolean = army.removeTroop(troop)
    override fun setTroopAt(index: Int, troop: Troop?) = army.setTroopAt(index, troop)

    internal fun removeTroopAt(index: Int): Troop? {
        if (index !in troops.indices) return null
        val removedTroop = troops[index]
        troops[index] = null
        return removedTroop
    }

    fun addTroop(troop: Troop): Boolean = army.addTroop(troop)

    internal fun swapTroops(index1: Int, index2: Int) {
        if (index1 !in troops.indices || index2 !in troops.indices) return
        val temporary = troops[index1]
        troops[index1] = troops[index2]
        troops[index2] = temporary
    }

    override fun write(json: Json) {
        json.writeArrayStart("slots")
        for (troop in troops) json.writeValue(troop)
        json.writeArrayEnd()
    }

    override fun read(json: Json, jsonData: JsonValue) {
        val slotArray = jsonData.get("slots") ?: return
        for (i in 0 until maxSlots) {
            val troopData = if (i < slotArray.size) slotArray.get(i) else null
            troops[i] = when {
                troopData == null || !troopData.has("amount") -> null
                troopData.has("speed") -> json.readValue(Troop::class.java, troopData).also {
                    it.restoreFormationIfMissing()
                }

                else -> {
                    val unitName = troopData.getString("unitName", "Spearman")
                    val amount = troopData.getInt("amount", 0)
                    TroopFactory.create(unitName, amount, troopSource)
                }
            }
        }
    }

    fun finishBattle() {
        for (index in troops.indices) {
            val troop = troops[index] ?: continue
            if (troop.currentAmount <= 0) {
                troops[index] = null
                continue
            }
            troop.amount = troop.currentAmount
            troop.currentHealth = troop.maxHealth
            troop.resetFormation()
        }
    }
    fun copySlots(): Array<Troop?> = troops

    fun clone(): ArmyInfo {
        val copy = ArmyInfo(civInfo, maxSlots)
        for (index in troops.indices) copy.setTroopAt(index, troops[index])
        return copy
    }
}
