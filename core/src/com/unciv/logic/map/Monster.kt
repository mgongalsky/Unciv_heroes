package com.unciv.logic.map

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.event.hero.Troop
import com.unciv.ui.images.ImageGetter

class Monster(var amount: Int, var monsterName: String) : MapUnit() {
 //   var troops = mutableListOf<Troop>()

    init{
        troops.clear()
        baseUnit = ImageGetter.ruleset.units[monsterName]!!
        //   amount = amount0
        val amountOfTroops = 4
        for(i in 1..amountOfTroops)
        {
            troops.add(Troop(amount/amountOfTroops, monsterName))

        }


    }
    constructor(amount: Int, monsterName: String, pos: TileInfo) : this(amount, monsterName)
    {

        currentTile = pos



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
