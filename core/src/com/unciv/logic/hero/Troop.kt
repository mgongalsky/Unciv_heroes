package com.unciv.logic.hero

import com.badlogic.gdx.math.Vector2
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

class Troop (
    var amount: Int, // TODO: Maybe @transient is required
    val unitName: String
    ) : IsPartOfGameInfoSerialization {

    @Transient
    lateinit var civInfo: CivilizationInfo

    @Transient
    lateinit var baseUnit: BaseUnit

    @Transient
    lateinit var troopImages: ArrayList<Image>

    lateinit  var position: Vector2 //= Vector2(2f,2f)
    // type, amount, currentHealth, currentAmount, spells, ref2unittype, promotions
    init{
        baseUnit = ruleset.units[unitName]!!
        baseUnit.ruleset = ruleset
        var currentHealth: Int = baseUnit.strength // TODO: Change to Health in Units.json
        var currentAmount = amount
       // position = Vector2(2f, 2f)
    }

    fun enterBattle(civInfo0: CivilizationInfo, number: Int, attacker: Boolean)
    {
        civInfo = civInfo0
        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name
        // TODO: There is a mess with float and int coordinates. It's better to make int everywhere
        if(attacker)
            position = Vector2(-7f, 3f-number.toFloat()*2)
        else
            position = Vector2(6f, 3f-number.toFloat()*2)

//        val amountText = Label(amount.toString(), BaseScreen.skin)
        troopImages = ImageGetter.getLayeredImageColored(unitTroopString, null, civInfo.nation.getInnerColor(), civInfo.nation.getOuterColor())


    }

    fun drawOnBattle(tileGroup: TileGroup, attacker: Boolean)
    {
        val amountText = Label(amount.toString(), BaseScreen.skin)
        amountText.moveBy(tileGroup.width*0.8f, 0f)

        for (troopImage in troopImages) {
            if(attacker) {
                troopImage.setScale(-2f, 2f)
                troopImage.moveBy(tileGroup.width*3.2f, -tileGroup.height*0.6f)
            }
            else {
                troopImage.setScale(2f, 2f)
                troopImage.moveBy(tileGroup.width*(-2.2f), -tileGroup.height*0.6f)
            }
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            val hexCoords =
                    HexMath.hexTranspose(HexMath.hex2EvenQCoords(tileGroup.tileInfo.position))
            val hexLabel = Label(hexCoords.x.toString() + ", " + hexCoords.y.toString(),
                BaseScreen.skin
            )
        //    if(hexCoords == position || true)// && tileGroup.tileInfo.longitude==3f)
         //   {
            tileGroup.addActor(troopImage)
            //tileGroup.addActor(amountText)
            tileGroup.addActor(hexLabel)
          //  }
            //     setHexagonImageSize(troopImage)// Treat this as A TILE, which gets overlayed on the base tile.
        }





    }
}
