package com.unciv.logic.hero

import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.models.ruleset.unit.BaseUnit

class Monster(
    var amount: Int, // TODO: Maybe @transient is required
    val unitName: String
) : IsPartOfGameInfoSerialization {
  //  @Transient
  //  lateinit var baseUnit: BaseUnit
    @Transient
    var troops = mutableListOf<Troop>()

    init {
        troops.clear()
        val amountOfTroops = 4
        for(i in 1..amountOfTroops)
        {
            troops.add(Troop(amount/amountOfTroops, unitName))

        }
    }


}
