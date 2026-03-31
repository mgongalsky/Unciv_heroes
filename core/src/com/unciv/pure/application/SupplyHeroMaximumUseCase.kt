package com.unciv.pure.application

import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.hero.HeroWithSupply
import kotlin.math.min

object SupplyHeroMaximumUseCase {
    fun execute(hero: HeroWithSupply, city: CityInfo): Float {
        val heroSpaceLeft = hero.baseFoodCapacity - hero.currentFood
        val foodToTransfer = min(heroSpaceLeft, city.population.foodStored.toFloat())
        if (foodToTransfer <= 0f) return 0f
        hero.addFood(foodToTransfer)
        city.population.foodStored -= foodToTransfer.toInt()
        return foodToTransfer
    }
}
