package com.unciv.logic.event.hero

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
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
    var amount: Int, // TODO: Maybe @transient is required
    val unitName: String
    ) : IsPartOfGameInfoSerialization {

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    var baseUnit: BaseUnit = ruleset.units[unitName]!!

    // Group of actors for this troop. (Mostly pixmap of the unit and its amount)
    @Transient
    var troopGroup = Group()

    @Transient
    lateinit var troopImages: ArrayList<Image>

    // Current amount of units and health, which can be changed during the battle.
    var currentHealth = baseUnit.health
    var currentAmount = amount
    // This is in offset coordinates:
    /** Position of a troop in hex coordinates */
    lateinit var position: Vector2 //= Vector2(2f,2f)

    /** Called when battle is started (or the troop is summoned). [number] corresponds to location in the hero's army and determines initial location */
    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean)
    {
        civInfo = civInfo0
        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name
        // TODO: There is a mess with float and int coordinates. It's better to make int everywhere
        if(attacker)
            position = HexMath.evenQ2HexCoords(Vector2(-7f, 3f-number.toFloat()*2))
        else
            position = HexMath.evenQ2HexCoords(Vector2(6f, 3f-number.toFloat()*2))

        // Load images for all troops
        troopImages = ImageGetter.getLayeredImageColored(unitTroopString, null, civInfo.nation.getInnerColor(), civInfo.nation.getOuterColor())
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
                troopImage.setScale(-2f, 2f)
                troopImage.moveBy(tileGroup.width*2.2f, -tileGroup.height*0.3f)
            }
            else {
                troopImage.setScale(2f, 2f)
                troopImage.moveBy(tileGroup.width*(-1.2f), -tileGroup.height*0.3f)
            }
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            troopImage.touchable = Touchable.disabled
            troopImage.name = "troopImage"
            troopGroup.name = "troopGroup"
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
        troopGroup.addActor(amountText)
        // Uncomment this for debug with rendering of coordinates. Comment amountText label.
        //troopGroup.addActor(hexLabel)
        tileGroup.addActor(troopGroup)
    }

    fun perish(){
        troopGroup.remove()
    }
}
