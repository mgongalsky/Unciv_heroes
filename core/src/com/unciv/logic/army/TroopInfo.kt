package com.unciv.logic.army

import com.badlogic.gdx.graphics.Color
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
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.ImageGetter.ruleset
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen

/** Battle units with specified [amount], [position] in hex coordinates and reference to a [baseUnit] */
class TroopInfo (
    var amount: Int = 0, // TODO: Maybe @transient is required
    var unitName: String = "Spearman"
    ) : IsPartOfGameInfoSerialization, Json.Serializable {

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    lateinit var baseUnit: BaseUnit // = ruleset.units[unitName]!!

    /** Current amount of units and health, which can be changed during the battle. We need it for resurrection. */
    @Transient
    var currentHealth = 0
    //baseUnit.health

    @Transient
    var currentAmount = amount
    // This is in offset coordinates:
    /** Position of a troop in hex coordinates */
    lateinit var position: Vector2 //= Vector2(2f,2f)

    // Новый удобный конструктор
    constructor(unitName: String, amount: Int, civInfo: CivilizationInfo) : this(amount, unitName) {
        this.civInfo = civInfo
        initializeVariables()
    }

    constructor() : this(0, "Spearman")

    init {
        initializeVariables()
      //  if (amount == null || unitName == null) {
        //    throw IllegalArgumentException("Amount and unitName must not be null")

       // }
    }

    fun setTransients(civInfo0: CivilizationInfo){
        civInfo = civInfo0
        baseUnit = ruleset.units[unitName]!!
        currentAmount = amount
        currentHealth = baseUnit.health
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
        //copiedTroop.position = this.position.cpy() // Ensures position is not shared by reference

        if (::civInfo.isInitialized) {
            copiedTroop.civInfo = this.civInfo
        }

        if (::baseUnit.isInitialized) {
            copiedTroop.baseUnit = this.baseUnit
        }

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
        //json.writeObjectStart()

        json.writeValue("name", "Troop") // Set the name for the object
        json.writeValue("amount", amount)
        json.writeValue("unitName", unitName)
      //  json.writeValue("position", position)
        //json.writeObjectEnd()
    }
//
    override fun read(json: Json, jsonData: JsonValue) {
        // Implement the read method if you also want to support deserialization
        // Read the values and assign them to the corresponding properties
        amount = json.readValue("amount", Int::class.java, jsonData)
        unitName = json.readValue("unitName", String::class.java, jsonData)
       // position = json.readValue("position", Vector2::class.java, jsonData)

        initializeVariables()
        // Read other properties if needed
    }


    /** Called when battle is started (or the troop is summoned). [number] corresponds to location in the hero's army and determines initial location */
    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean)
    {
        civInfo = civInfo0
        baseUnit = ruleset.units[unitName]!!

        // TODO: There is a mess with float and int coordinates. It's better to make int everywhere
        if(attacker)
            position = HexMath.evenQ2HexCoords(Vector2(-7f, 3f-number.toFloat()*2))
        else
            position = HexMath.evenQ2HexCoords(Vector2(6f, 3f-number.toFloat()*2))

        currentHealth = baseUnit.health
        currentAmount = amount

    }
}
