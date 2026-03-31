package com.unciv.pure.domain.supply

import com.unciv.logic.army.ArmyInfo
import com.unciv.pure.domain.hero.HeroWithSupply

interface SupplyMechanic {
    val isEnabled: Boolean
    fun consumeFood(hero: HeroWithSupply, army: ArmyInfo)
    fun isStarving(hero: HeroWithSupply): Boolean
    fun onStarving(hero: HeroWithSupply, army: ArmyInfo)
}
