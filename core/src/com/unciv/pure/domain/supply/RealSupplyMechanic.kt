package com.unciv.pure.domain.supply

import com.unciv.logic.army.ArmyInfo
import com.unciv.pure.domain.hero.HeroWithSupply

class RealSupplyMechanic : SupplyMechanic {
    override val isEnabled = true

    override fun consumeFood(hero: HeroWithSupply, army: ArmyInfo) {
        hero.addFood(-army.calculateFoodMaintenance(isInCity = false))
    }

    override fun isStarving(hero: HeroWithSupply) = hero.currentFood <= 0f

    override fun onStarving(hero: HeroWithSupply, army: ArmyInfo) {
        // TODO: disbandWeakestTroop
    }
}
