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

    fun showMoraleBird(){

        val moraleImage = ImageGetter.getExternalImage("MoraleBird.png")
        //val moraleImage = ImageGetter.getExternalImage("LuckRainbow.png")

        moraleImage.setScale(0.075f, 0.075f)
        //moraleImage.setScale(0.130f, 0.130f)
        moraleImage.moveBy(troopGroup.parent.width*(-0.035f), troopGroup.parent.height*1.85f)
        //moraleImage.moveBy(troopGroup.parent.width*(-0.3f), troopGroup.parent.height*0.65f)
        moraleImage.touchable = Touchable.disabled
        moraleImage.name = "moraleImage"
        moraleImage.color = Color.WHITE.cpy().apply { a = 0f }

        val fadeIn = Actions.alpha(1f, 0.5f)  // Make image appear over 2 seconds
        val delay = Actions.delay(0.3f)  // Wait for 0.5 seconds
        val fadeOut = Actions.alpha(0f, 0.5f)  // Make image vanish over 2 seconds

        val endAnimation = Actions.run {
            // Code to execute after the animation ends
            // This is where you could resume your game's logic
        }

        moraleImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, endAnimation))
        //  moraleImage.addAction(Actions.alpha(1f, 0.5f))
        //moraleImage.addAction(Actions.alpha(0f, 1f))
        troopGroup.addActor(moraleImage)
        //troopGroup.stage.act(2f)
        //troopGroup.stage.draw()

        //stage.addActor(logoImage)

    }

    fun showLuckRainbow(){

        //val moraleImage = ImageGetter.getExternalImage("MoraleBird.png")
        val moraleImage = ImageGetter.getExternalImage("LuckRainbow.png")

        //moraleImage.setScale(0.075f, 0.075f)
        moraleImage.setScale(0.130f, 0.130f)
        //moraleImage.moveBy(troopGroup.parent.width*(-0.035f), troopGroup.parent.height*1.85f)
        moraleImage.moveBy(troopGroup.parent.width*(-0.3f), troopGroup.parent.height*0.65f)
        moraleImage.touchable = Touchable.disabled
        moraleImage.name = "moraleImage"
        moraleImage.color = Color.WHITE.cpy().apply { a = 0f }

        val fadeIn = Actions.alpha(1f, 0.5f)  // Make image appear over 2 seconds
        val delay = Actions.delay(0.3f)  // Wait for 0.5 seconds
        val fadeOut = Actions.alpha(0f, 0.5f)  // Make image vanish over 2 seconds

        val endAnimation = Actions.run {
            // Code to execute after the animation ends
            // This is where you could resume your game's logic
        }

        moraleImage.addAction(Actions.sequence(fadeIn, delay, fadeOut, endAnimation))
        //  moraleImage.addAction(Actions.alpha(1f, 0.5f))
        //moraleImage.addAction(Actions.alpha(0f, 1f))
        troopGroup.addActor(moraleImage)
        //troopGroup.stage.act(2f)
        //troopGroup.stage.draw()

        //stage.addActor(logoImage)

    }

    /** Called when battle is started (or the troop is summoned). [number] corresponds to location in the hero's army and determines initial location */
    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean, oldVersion: Boolean = true)
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

        if (oldVersion)
            enterBattleView(civInfo0, number, attacker)

    }

    fun enterBattleView(civInfo0: CivilizationInfo, number: Int, attacker: Boolean)
    {
        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name
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
    fun DeleteDrawInCity(group: Group, isGarrison: Boolean)
    {
        // Draw amount of units
        // TODO: Here we need to make text smaller, but looks like we need to introduce new font for this.
        val amountText = Label(currentAmount.toString(), BaseScreen.skin)
        amountText.scaleBy(0.5f)
        //amountText.style.font.data.scale(0.5f)

        amountText.moveBy(group.width*0.6f, 0.5f)
        //amountText.style.font.data.scale(0.5f)

        // Draw pixmap of a troop
        for (troopImage in troopImages) {
            troopImage.setScale(-0.125f, 0.125f)
            troopImage.moveBy(group.width*0.8f, 0f)

            troopImage.setOrigin(group.originX, group.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            troopImage.touchable = Touchable.disabled
            troopImage.name = "troopImage"
            troopGroup.name = "troopGroup"
            troopGroup.findActor<Image>("troopImage")?.remove()

            troopGroup.addActor(troopImage)
        }

        amountText.name = "amountLabel"
        amountText.touchable = Touchable.disabled
        troopGroup.findActor<Label>("amountLabel")?.remove()
        troopGroup.addActor(amountText)
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

        //showMoraleBird()
    }

    fun perish(){
        troopGroup.remove()
    }
}
