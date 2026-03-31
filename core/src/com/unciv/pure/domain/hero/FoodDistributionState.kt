package com.unciv.pure.domain.hero

import kotlin.math.min

data class FoodDistributionState(
    val currFoodHero: Float,
    val currFoodCity: Float,
    val maxFoodHero: Float,
    val maxFoodCity: Float
) {
    val freeFoodHero = maxFoodHero - currFoodHero
    val freeFoodCity = maxFoodCity - currFoodCity
    val minHero = min(currFoodHero, freeFoodCity)
    val minCity = min(currFoodCity, freeFoodHero)
    val foodRange = minHero + minCity
}
