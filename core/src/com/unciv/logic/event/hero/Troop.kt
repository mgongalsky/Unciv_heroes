package com.unciv.logic.event.hero

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
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
class Troop (
    var amount: Int = 0, // TODO: Maybe @transient is required
    var unitName: String = "Spearman"
    ) : IsPartOfGameInfoSerialization, Json.Serializable {

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    lateinit var baseUnit: BaseUnit // = ruleset.units[unitName]!!

    // Group of actors for this troop. (Mostly pixmap of the unit and its amount)
    @Transient
    var troopGroup = Group()

    @Transient
    lateinit var troopImages: ArrayList<Image>

    /** Current amount of units and health, which can be changed during the battle. We need it for resurrection. */
    var currentHealth = 0
    //baseUnit.health

    var currentAmount = amount
    // This is in offset coordinates:
    /** Position of a troop in hex coordinates */
    lateinit var position: Vector2 //= Vector2(2f,2f)

    constructor() : this(0, "Spearman")

    init {
        initializeVariables()
      //  if (amount == null || unitName == null) {
        //    throw IllegalArgumentException("Amount and unitName must not be null")

       // }
    }

    private fun initializeVariables() {
        baseUnit = ruleset.units[unitName]!!
        currentAmount = amount
        currentHealth = baseUnit.health
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

        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name
        // TODO: There is a mess with float and int coordinates. It's better to make int everywhere
        if(attacker)
            position = HexMath.evenQ2HexCoords(Vector2(-7f, 3f-number.toFloat()*2))
        else
            position = HexMath.evenQ2HexCoords(Vector2(6f, 3f-number.toFloat()*2))

        currentHealth = baseUnit.health
        currentAmount = amount

        // Load images for all troops
        troopImages = ImageGetter.getLayeredImageColored(unitTroopString, null, civInfo.nation.getInnerColor(), civInfo.nation.getOuterColor())
    }

    fun initializeImages(civInfo0: CivilizationInfo)
    {
        civInfo = civInfo0
        baseUnit = ruleset.units[unitName]!!
        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name

        troopImages = ImageGetter.getLayeredImageColored(unitTroopString, null, civInfo.nation.getInnerColor(), civInfo.nation.getOuterColor())


    }

    /** Draw the troop in a city within specifed [Group]*/
    fun drawInCity(group: Group, isGarrison: Boolean)
    {
        // Draw amount of units
        val amountText = Label(currentAmount.toString(), BaseScreen.skin)
        amountText.setScale(0.2f)
        amountText.moveBy(group.width*0.4f, 0.5f)

        // Draw pixmap of a troop
        for (troopImage in troopImages) {
            troopImage.setScale(-0.125f, 0.125f)
            troopImage.moveBy(group.width*1.3f*0.5f, 0f)

            troopImage.setOrigin(group.originX, group.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            troopImage.touchable = Touchable.disabled
            troopImage.name = "troopImage"
            troopGroup.name = "troopGroup"
            troopGroup.findActor<Image>("troopImage")?.remove()

            troopGroup.addActor(troopImage)
        }

        // hexCoordsLabel is used for debug only. Shows various coordinates for the troop
        //val hexCoords = group.tileInfo.position
        /*
        var hexLabel = Label(hexCoords.x.toString() + ", " + hexCoords.y.toString() + "\r\n" +
                position.x.toString() + ", " + position.y.toString() + "\r\n" +
                group.tileInfo.position.x.toString() + ", " + group.tileInfo.position.y.toString(),
            BaseScreen.skin)

         */
        //hexLabel.name = "hexCoordsLabel"
        //hexLabel.touchable = Touchable.disabled
        amountText.name = "amountLabel"
        amountText.touchable = Touchable.disabled
        troopGroup.findActor<Label>("amountLabel")?.remove()
        //  troopGroup.findActor<>()
        troopGroup.addActor(amountText)
        // Uncomment this for debug with rendering of coordinates. Comment amountText label.
        //troopGroup.addActor(hexLabel)
        group.addActor(troopGroup)
    }



    /** Draw the troop on a battle within specifed [tileGroup]*/
    fun drawOnBattle(tileGroup: TileGroup, attacker: Boolean)
    {
        // Draw amount of units
        val amountText = Label(currentAmount.toString(), BaseScreen.skin)
        amountText.moveBy(tileGroup.width*0.5f, 0f)

        // Draw pixmap of a troop
        for (troopImage in troopImages) {
            if(attacker) {
                troopImage.setScale(-0.25f, 0.25f)
                troopImage.moveBy(tileGroup.width*1.3f, tileGroup.height*0.15f)
            }
            else {
                troopImage.setScale(0.25f, 0.25f)
                troopImage.moveBy(tileGroup.width*(-0.3f), tileGroup.height*0.15f)
            }
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            troopImage.touchable = Touchable.disabled
            troopImage.name = "troopImage"
            troopGroup.name = "troopGroup"
            troopGroup.findActor<Image>("troopImage")?.remove()

            troopGroup.addActor(troopImage)
        }

        // hexCoordsLabel is used for debug only. Shows various coordinates for the troop
        val hexCoords = tileGroup.tileInfo.position
        var hexLabel = Label(hexCoords.x.toString() + ", " + hexCoords.y.toString() + "\r\n" +
                position.x.toString() + ", " + position.y.toString() + "\r\n" +
                tileGroup.tileInfo.position.x.toString() + ", " + tileGroup.tileInfo.position.y.toString(),
            BaseScreen.skin)
        hexLabel.name = "hexCoordsLabel"
        hexLabel.touchable = Touchable.disabled
        amountText.name = "amountLabel"
        amountText.touchable = Touchable.disabled
        troopGroup.findActor<Label>("amountLabel")?.remove()
      //  troopGroup.findActor<>()
        troopGroup.addActor(amountText)
        // Uncomment this for debug with rendering of coordinates. Comment amountText label.
        //troopGroup.addActor(hexLabel)
        tileGroup.addActor(troopGroup)
    }

    fun perish(){
        troopGroup.remove()
    }
}
