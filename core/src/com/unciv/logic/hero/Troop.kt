package com.unciv.logic.hero

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.ui.images.ImageGetter.ruleset

class Troop (
    var amount: Int, // TODO: Maybe @transient is required
    val unitName: String
    ) : IsPartOfGameInfoSerialization {

    var position: Vector2 = Vector2(5f,5f)
    // type, amount, currentHealth, currentAmount, spells, ref2unittype, promotions
    init{
        val baseUnit = ruleset.units[unitName]!!
        baseUnit.ruleset = ruleset
        var currentHealth: Int = baseUnit.strength // TODO: Change to Health in Units.json
        var currentAmount = amount
    }



}
