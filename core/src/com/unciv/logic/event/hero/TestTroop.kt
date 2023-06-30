package com.unciv.logic.event.hero

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.ui.images.ImageGetter

/*
class PhoneNumber : Json.Serializable {
    private var name: String? = null
    private var number: String? = null
    fun write(json: Json) {
        json.writeValue(name, number)
    }

    fun read(json: Json?, jsonMap: JsonValue) {
        name = jsonMap.child().name()
        number = jsonMap.child().asString()
    }
}


 */
/** !! This class is for testing purposes only. Not used in the game */
class TestTroop(
    var amount: Int = 0, // TODO: Maybe @transient is required
    var unitName: String = ""

) : IsPartOfGameInfoSerialization, Json.Serializable {

  //  var amount: Int = 0 // TODO: Maybe @transient is required
  //  val unitName: String = ""

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    var baseUnit: BaseUnit? = null // = ImageGetter.ruleset.units[unitName]!!

    // Group of actors for this troop. (Mostly pixmap of the unit and its amount)
    @Transient
    var troopGroup = Group()

    @Transient
    lateinit var troopImages: ArrayList<Image>



    // Current amount of units and health, which can be changed during the battle.
  //  var currentHealth = baseUnit.health
    var currentAmount = 0
    //amount
    // This is in offset coordinates:
    /** Position of a troop in hex coordinates */
    lateinit var position: Vector2 //= Vector2(2f,2f)





    constructor() : this(0, "")

    init {
        initializeVariables()
    }

    // Function to update the baseUnit and currentAmount based on the unitName
    private fun initializeVariables() {
        baseUnit = ImageGetter.ruleset.units[unitName]
        currentAmount = amount
    }

    /*
    override fun write(json: Json) {
        with(json) {
            json.writeObjectStart()
            json.writeValue("name", "TestTroop") // Set the name for the object

            json.writeValue("amount", amount)
            json.writeValue("unitName", unitName)
            json.writeObjectEnd()
        }
    }

    override fun read(json: Json, jsonData: JsonValue) {
        with(jsonData) {
            amount = getInt("amount", 0)
            unitName = getString("unitName", "")
        }
        initializeVariables()
    }


     */
    override fun write(json: Json) {
        //json.writeObjectStart()

        json.writeValue("name", "TestTroop") // Set the name for the object
        json.writeValue("amount", amount)
        json.writeValue("unitName", unitName)
        //json.writeObjectEnd()
    }

    override fun read(json: Json, jsonData: JsonValue) {
        // Implement the read method if you also want to support deserialization
        // Read the values and assign them to the corresponding properties
        amount = json.readValue("amount", Int::class.java, jsonData)
        unitName = json.readValue("unitName", String::class.java, jsonData)
        initializeVariables()
        // Read other properties if needed
    }
/*
    override fun write(json: Json) {
        //json.writeValue(name, number)
        json.toJson(this)
        json.writeValue(amount)
        //json.writeValue(unitName)
    }


 */

/*
    override fun read(json: Json?, jsonMap: JsonValue) {
        json.fromJson()
        //super.read(json, jsonMap)
        //amount = jsonMap.child().asInt()
        //unitName = jsonMap.child().asString()
       // name = jsonMap.child().name()
       // number = jsonMap.child().asString()
    }


 */
}
