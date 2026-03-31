package com.unciv.pure.application

import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.hero.HeroWithSupply
import kotlin.math.min

object TransferFoodUseCase {
    fun execute(hero: HeroWithSupply, city: CityInfo, amount: Float) {
        if (amount > 0f) {
            // город → герой
            val actualAmount = min(amount, min(
                hero.baseFoodCapacity - hero.currentFood,
                city.population.foodStored.toFloat()
            ))
            if (actualAmount <= 0f) return
            hero.addFood(actualAmount)
            city.population.foodStored -= actualAmount.toInt()
        } else if (amount < 0f) {
            // герой → город
            val actualAmount = min(-amount, hero.currentFood)
            if (actualAmount <= 0f) return
            hero.addFood(-actualAmount)
            city.population.foodStored += actualAmount.toInt()
        }
    }
}
