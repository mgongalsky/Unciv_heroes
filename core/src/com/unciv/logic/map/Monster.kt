package com.unciv.logic.map

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.logic.event.hero.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen

class Monster(var amount: Int, var monsterName: String) : MapUnit() {
 //   var troops = mutableListOf<Troop>()
 // Group of actors for this troop. (Mostly pixmap of the unit and its amount)
    @Transient
    var monsterGroup = Group()

    @Transient
    lateinit var monsterImages: ArrayList<Image>

    init{
        troops.clear()
        baseUnit = ImageGetter.ruleset.units[monsterName]!!
        //   amount = amount0
        val amountOfTroops = 4
        for(i in 1..amountOfTroops)
        {
            troops.add(Troop(amount/amountOfTroops, monsterName))

        }
        val imageString = "TileSets/AbsoluteUnits/Units/" + monsterName

        monsterImages = ImageGetter.getLayeredImageColored(imageString, null)


    }
    constructor(amount: Int, monsterName: String, tileGroup: TileGroup) : this(amount, monsterName)
    {

        currentTile = tileGroup.tileInfo
        drawOnBattle(tileGroup)


    }
    /** Draw the troop on a battle within specifed [tileGroup]*/
    fun drawOnBattle(tileGroup: TileGroup)
    {
        // Draw amount of units
      ////  val amountText = Label(currentAmount.toString(), BaseScreen.skin)
       // amountText.moveBy(tileGroup.width*0.5f, 0f)

        // Draw pixmap of a troop
        for (monsterImage in monsterImages) {
            monsterImage.setScale(0.15f, 0.15f)
            monsterImage.moveBy(tileGroup.width*(-0.001f), tileGroup.height*0.15f)
            monsterImage.setOrigin(tileGroup.originX, tileGroup.originY)
            /// TODO: Seems like latitude and longitude work incorrectly in main map
            monsterImage.touchable = Touchable.disabled
            monsterImage.name = "monsterImage"
            monsterGroup.name = "monsterGroup"
            monsterGroup.addActor(monsterImage)
        }

        // hexCoordsLabel is used for debug only. Shows various coordinates for the troop
       /* val hexCoords = tileGroup.tileInfo.position
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
*/
        tileGroup.addActor(monsterGroup)
        tileGroup.update()
    }

    fun remove(){
        monsterGroup.remove()
    }


/*
    init {
        troops.clear()
        val amountOfTroops = 4
        for(i in 1..amountOfTroops)
        {
            troops.add(Troop(amount/amountOfTroops, unitName))

        }
    }

 */

}
