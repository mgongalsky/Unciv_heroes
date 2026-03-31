package com.unciv.pure.application

import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.hero.HeroWithSupply
import kotlin.math.min

object SupplyHeroMaximumUseCase {
    fun execute(hero: HeroWithSupply, city: CityInfo): Float {
        val amount = min(
            hero.baseFoodCapacity - hero.currentFood,
            city.population.foodStored.toFloat()
        )
        TransferFoodUseCase.execute(hero, city, amount)
        return amount
    }
}
