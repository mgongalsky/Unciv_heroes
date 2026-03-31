package com.unciv.pure.domain.supply

import com.unciv.logic.army.ArmyInfo
import com.unciv.pure.domain.hero.HeroWithSupply

class NoSupplyMechanic : SupplyMechanic {
    override val isEnabled = false
    override fun consumeFood(hero: HeroWithSupply, army: ArmyInfo) = Unit
    override fun isStarving(hero: HeroWithSupply) = false
    override fun onStarving(hero: HeroWithSupply, army: ArmyInfo) = Unit
}
