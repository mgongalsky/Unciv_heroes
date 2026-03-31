package com.unciv.testing.pure.application


import com.unciv.logic.city.CityInfo
import com.unciv.pure.application.TransferFoodUseCase
import com.unciv.pure.domain.hero.HeroFactory
import com.unciv.pure.domain.hero.HeroWithSupply
import com.unciv.pure.infrastructure.hero.HardcodedHeroDefinitionSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TransferFoodUseCaseTest {

    private fun makeHero(currentFood: Float, baseFoodCapacity: Float = 15f): HeroWithSupply =
            HeroFactory.create(
                source = HardcodedHeroDefinitionSource(5, 5, baseFoodCapacity),
                unitName = "test",
                currentFood = currentFood
            )

    private fun makeCity(foodStored: Int): CityInfo {
        val city = CityInfo()
        city.population.foodStored = foodStored
        return city
    }

    // --- Передача еды городом герою (положительная дельта) ---

    @Test
    fun `transfers exact amount from city to hero`() {
        val hero = makeHero(currentFood = 5f)
        val city = makeCity(foodStored = 10)
        TransferFoodUseCase.execute(hero, city, 3f)
        assertEquals(8f, hero.currentFood, 0.001f)
        assertEquals(7, city.population.foodStored)
    }

    @Test
    fun `does not exceed hero capacity when transferring to hero`() {
        val hero = makeHero(currentFood = 13f)
        val city = makeCity(foodStored = 10)
        TransferFoodUseCase.execute(hero, city, 5f)
        assertEquals(15f, hero.currentFood, 0.001f)
        assertEquals(8, city.population.foodStored)
    }

    @Test
    fun `does not take more than city has when transferring to hero`() {
        val hero = makeHero(currentFood = 5f)
        val city = makeCity(foodStored = 2)
        TransferFoodUseCase.execute(hero, city, 5f)
        assertEquals(7f, hero.currentFood, 0.001f)
        assertEquals(0, city.population.foodStored)
    }

    // --- Возврат еды герою городу (отрицательная дельта) ---

    @Test
    fun `transfers exact amount from hero to city`() {
        val hero = makeHero(currentFood = 10f)
        val city = makeCity(foodStored = 5)
        TransferFoodUseCase.execute(hero, city, -3f)
        assertEquals(7f, hero.currentFood, 0.001f)
        assertEquals(8, city.population.foodStored)
    }

    @Test
    fun `does not take more than hero has when returning to city`() {
        val hero = makeHero(currentFood = 2f)
        val city = makeCity(foodStored = 5)
        TransferFoodUseCase.execute(hero, city, -5f)
        assertEquals(0f, hero.currentFood, 0.001f)
        assertEquals(7, city.population.foodStored)
    }


    // TODO: проверка вместимости города при возврате еды —
    // ограничение должно приходить от FoodDistributionState через слайдер,
    // а не от TransferFoodUseCase. Требует FakeCityInfo с инициализированным cityInfo.

    // --- Граничные случаи ---

    @Test
    fun `does nothing when amount is zero`() {
        val hero = makeHero(currentFood = 5f)
        val city = makeCity(foodStored = 10)
        TransferFoodUseCase.execute(hero, city, 0f)
        assertEquals(5f, hero.currentFood, 0.001f)
        assertEquals(10, city.population.foodStored)
    }
}
