package com.unciv.pure.application

import com.unciv.pure.domain.hero.FoodDistributionState

data class FoodDistributionResult(
    val state: FoodDistributionState,
    val foodDurationTurns: Int
)

object CalculateFoodDistributionUseCase {
    fun execute(
        state: FoodDistributionState,
        sliderValue: Float, // TODO: заменить на foodToTransfer
        heroMaintenance: Float
    ): FoodDistributionResult {
        val newHeroFood = sliderValue + state.currFoodHero - state.minHero
        val newCityFood = state.foodRange - sliderValue + state.currFoodCity - state.minCity
        val newState = FoodDistributionState(
            currFoodHero = newHeroFood,
            currFoodCity = newCityFood,
            maxFoodHero = state.maxFoodHero,
            maxFoodCity = state.maxFoodCity
        )
        val foodDuration = if (heroMaintenance > 0)
            (newHeroFood / heroMaintenance).toInt()
        else Int.MAX_VALUE
        return FoodDistributionResult(newState, foodDuration)
    }
}
