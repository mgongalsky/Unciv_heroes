package com.unciv.logic.army

import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.MovableUnit
import com.unciv.logic.civilization.CivilizationInfo

// Import MapUnit as the hero type
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.TroopEntersBattleUseCase
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Represents battle units with a specified [amount],
 * a reference to a [baseUnit], and optionally a reference to a hero (MapUnit).
 *
 * The hero reference allows accessing additional battle-related parameters (e.g., morale).
 */
class TroopInfo(
    var amount: Int = 0,
    var unitName: String = "Spearman"
) : IsPartOfGameInfoSerialization, Json.Serializable, MovableUnit(), KoinComponent {

    @delegate:Transient
    private val ruleset: Ruleset by inject()

    var troop: Troop = TroopFactory.create(
        unitName = "Spearman",
        amount = 0,
        source = HardcodedTroopDefinitionSource(
            speed = 0,
            damage = 0,
            maxHealth = 1,
            rangedStrength = 0
        )
    )

    val speed: Int
        get() = troop.speed

    val damage: Int
        get() = troop.damage

    val maxHealth: Int
        get() = troop.maxHealth

    val rangedStrength: Int
        get() = troop.rangedStrength

    /** Current total health and unit count, which may change during battle. */
    var currentAmount: Int
        get() = troop.currentAmount
        set(value) { troop.currentAmount = value }

    var currentHealth: Int
        get() = troop.currentHealth
        set(value) { troop.currentHealth = value }

    /** Position of the troop in hex coordinates (offset coordinates). */
    //lateinit var position: Vector2

    /**
     * Optional reference to the hero (MapUnit) that leads this troop.
     * May be null if the troop is in a garrison without a hero.
     */
    @Transient
    var hero: MapUnit? = null

    @Transient
    var isPlayerControlledOverride: Boolean? = null

    /**
     * Checks if a hero (MapUnit) is assigned to this troop.
     *
     * @return True if a hero is present, false otherwise.
     */
    fun hasHero(): Boolean = hero != null

    /**
     * Retrieves the morale value from the assigned hero (MapUnit).
     * If no hero is present, returns a default value (e.g., 0).
     *
     * @return The hero's morale or 0 if absent.
     */
    fun getHeroMorale(): Int = hero?.morale ?: 0

    var battleField: TileMap? = null

    // New convenient constructor with hero parameter.
    constructor(unitName: String, amount: Int, civInfo: CivilizationInfo, hero: MapUnit?) : this(amount, unitName) {
        this.civInfo = civInfo
        this.hero = hero
        initializeVariables()
    }

    constructor() : this(0, "Spearman")

    init {
        initializeVariables()
    }

    fun setTransients(civInfo0: CivilizationInfo, hero0: MapUnit?) {
        civInfo = civInfo0
        baseUnit = ruleset.units[unitName]!!
        currentAmount = amount
        currentHealth = maxHealth
        hero = hero0
    }

    /**
     * Creates a deep copy of the current TroopInfo instance.
     */
    fun copy(): TroopInfo {
        val copiedTroop = TroopInfo(
            amount = this.amount,
            unitName = this.unitName
        )
        copiedTroop.currentAmount = this.currentAmount
        copiedTroop.currentHealth = this.currentHealth

        if (isCivilizationInfoInitialized()) {
            copiedTroop.civInfo = this.civInfo
        }

        //if (isBaseUnitInitialized()) {
        //    copiedTroop.baseUnit = this.baseUnit
        //}

        if (isCurrentTileInitialized()) {
            copiedTroop.currentTile = this.currentTile
        }


        // Copy hero reference (shallow copy; assume MapUnit is managed elsewhere)
        copiedTroop.hero = this.hero

        return copiedTroop
    }

    private fun initializeVariables() {
        // baseUnit deprecated. just for interability.
        val unit = ruleset.units[unitName] ?: return
        baseUnit = unit
        troop = TroopFactory.create(
            unitName = unitName,
            amount = amount,
            source = RulesetTroopDefinitionSource(ruleset)
        )
    }

    fun isPlayerControlled(): Boolean =
            isPlayerControlledOverride ?: civInfo.isPlayerCivilization()

    override fun write(json: Json) {
        // Write minimal data for serialization
        json.writeValue("name", "Troop") // Object identifier
        json.writeValue("amount", amount)
        json.writeValue("unitName", unitName)
        // Position is omitted from serialization for brevity
    }

    override fun read(json: Json, jsonData: JsonValue) {
        // Read values and initialize properties
        amount = json.readValue("amount", Int::class.java, jsonData)
        unitName = json.readValue("unitName", String::class.java, jsonData)
        initializeVariables()
    }

    fun enterBattle(isPlayerControlled: Boolean, number: Int, attacker: Boolean, battleField0: TileMap) {
        TroopEntersBattleUseCase.execute(TroopEntersBattleUseCase.Input(
            troop = this,
            isPlayerControlled = isPlayerControlled,
            number = number,
            isAttacker = attacker,
            battleField = battleField0
        ))
    }

    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean, battleField0: TileMap) {
        civInfo = civInfo0
        TroopEntersBattleUseCase.execute(TroopEntersBattleUseCase.Input(
            troop = this,
            isPlayerControlled = civInfo0.isPlayerCivilization(),
            number = number,
            isAttacker = attacker,
            battleField = battleField0
        ))
    }

    /*
    fun moveToPosition(targetPosition: Vector2){
        position = targetPosition
        if(battleField != null) {
            currentTile.troopUnit = null
            currentTile = battleField!![position]
            currentTile.troopUnit = this
        }

    }

     */

    /**
     * Перемещает отряд на указанную клетку.
     *
     * @param targetTile Целевая клетка.
     */
    fun moveToTile(targetTile: TileInfo) {
        // Если у отряда есть текущая клетка, очищаем ссылку на него
        currentTile?.troopUnit = null

        // Обновляем текущую клетку и её позицию
        currentTile = targetTile
        //position = targetTile.position

        // Устанавливаем отряд на новую клетку
        currentTile?.troopUnit = this
    }


    fun finishBattle() {
        amount = currentAmount
        currentHealth = maxHealth
        currentTile.troopUnit = null
        battleField = null
    }

    fun perish() {
        if(isCurrentTileInitialized())
            currentTile.troopUnit = null
    }
}
