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

    var position: Vector2 = Vector2(2f,2f)
    // type, amount, currentHealth, currentAmount, spells, ref2unittype, promotions
    init{
        baseUnit = ruleset.units[unitName]!!
        baseUnit.ruleset = ruleset
        var currentHealth: Int = baseUnit.strength // TODO: Change to Health in Units.json
        var currentAmount = amount
    }

    fun enterBattle(civInfo0: CivilizationInfo)
    {
        civInfo = civInfo0
        val unitTroopString = "TileSets/AbsoluteUnits/Units/" + baseUnit.name
//        val amountText = Label(amount.toString(), BaseScreen.skin)
        troopImages = ImageGetter.getLayeredImageColored(unitTroopString, null, civInfo.nation.getInnerColor(), civInfo.nation.getOuterColor())


    }

    fun drawOnBattle(tileGroup: TileGroup)
    {
        val amountText = Label(amount.toString(), BaseScreen.skin)
        amountText.moveBy(tileGroup.width*0.8f, 0f)

        for (troopImage in troopImages) {
            //     var troopTileGroup = tileGroup.clone()
            troopImage.setScale(-2f, 2f)
            troopImage.moveBy(tileGroup.width*3.2f, -tileGroup.height*0.6f)
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            //    troopImage.
            //    troopTileGroup.resourceImage = troopImage

            //     stage.addActor(img)
            //  troopImage.setPosition(3f,3f)
            //   if(tileGroup.tileInfo.longitude==3f)// && tileGroup.tileInfo.longitude==3f)
            //  {
            /// TODO: Seems like latitude and longitude work incorrectly
            val hexCoords =
                    HexMath.hexTranspose(HexMath.hex2EvenQCoords(tileGroup.tileInfo.position))
            val hexLabel = Label(hexCoords.x.toString() + ", " + hexCoords.y.toString(),
                BaseScreen.skin
            )
            if(hexCoords == position)// && tileGroup.tileInfo.longitude==3f)
            {
                //          tileGroup.tileInfo.position.
                tileGroup.addActor(troopImage)
                //            tileGroup.addActor(amountText)
                tileGroup.addActor(hexLabel)
            }
            //     setHexagonImageSize(troopImage)// Treat this as A TILE, which gets overlayed on the base tile.
        }





    }
}
