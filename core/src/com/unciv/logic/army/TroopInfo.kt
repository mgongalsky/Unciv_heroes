package com.unciv.logic.army

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.HexMath
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.MovableUnit
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.ImageGetter.ruleset
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen

// Import MapUnit as the hero type
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileMap

/**
 * Represents battle units with a specified [amount], [position] in hex coordinates,
 * a reference to a [baseUnit], and optionally a reference to a hero (MapUnit).
 *
 * The hero reference allows accessing additional battle-related parameters (e.g., morale).
 */
class TroopInfo(
    var amount: Int = 0,
    var unitName: String = "Spearman"
) : IsPartOfGameInfoSerialization, Json.Serializable, MovableUnit() {

    /** Current total health and unit count, which may change during battle. */
    @Transient
    var currentHealth = 0

    @Transient
    var currentAmount = amount

    /** Position of the troop in hex coordinates (offset coordinates). */
    lateinit var position: Vector2

    /**
     * Optional reference to the hero (MapUnit) that leads this troop.
     * May be null if the troop is in a garrison without a hero.
     */
    @Transient
    var hero: MapUnit? = null

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
        currentHealth = baseUnit.health
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

        if (isBaseUnitInitialized()) {
            copiedTroop.baseUnit = this.baseUnit
        }

        if (isCurrentTileInitialized()) {
            copiedTroop.currentTile = this.currentTile
        }


        // Copy hero reference (shallow copy; assume MapUnit is managed elsewhere)
        copiedTroop.hero = this.hero

        return copiedTroop
    }

    private fun initializeVariables() {
        baseUnit = ruleset.units[unitName]!!
        currentAmount = amount
        currentHealth = baseUnit.health
    }

    fun isPlayerControlled(): Boolean {
        return civInfo.isPlayerCivilization()
    }

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

    /**
     * Called when the battle is started (or the troop is summoned).
     * [number] corresponds to the troop's index in the army and determines the initial position.
     *
     * @param civInfo0 The civilization information.
     * @param number The troop's index.
     * @param attacker Flag indicating if the troop is an attacker.
     */
    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean, battleField0: TileMap) {
        civInfo = civInfo0
        baseUnit = ruleset.units[unitName]!!

        battleField = battleField0
        // Set initial position based on whether the troop is attacking or defending.
        if (attacker)
            position = HexMath.evenQ2HexCoords(Vector2(-7f, 3f - number.toFloat() * 2))
        else
            position = HexMath.evenQ2HexCoords(Vector2(6f, 3f - number.toFloat() * 2))

        currentTile = battleField!![position]
        currentMovement = baseUnit.speed.toFloat()
        currentTile.troopUnit = this

        currentHealth = baseUnit.health
        currentAmount = amount
    }

    fun moveToPosition(targetPosition: Vector2){
        position = targetPosition
        if(battleField != null) {
            currentTile.troopUnit = null
            currentTile = battleField!![position]
            currentTile.troopUnit = this
        }

    }

    fun finishBattle() {
        amount = currentAmount
        currentHealth = baseUnit.health
        currentTile.troopUnit = null
        battleField = null
    }
}
