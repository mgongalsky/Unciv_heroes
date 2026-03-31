package com.unciv.testing.pure.application

import com.unciv.logic.city.CityInfo
import com.unciv.pure.application.SupplyHeroMaximumUseCase
import com.unciv.pure.domain.hero.HeroFactory
import com.unciv.pure.domain.hero.HeroWithSupply
import com.unciv.pure.infrastructure.hero.HardcodedHeroDefinitionSource
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class SupplyHeroMaximumUseCaseCharTest {

    private fun makeHero(currentFood: Float, baseFoodCapacity: Float = 15f): HeroWithSupply =
            HeroFactory.create(
                source = HardcodedHeroDefinitionSource(
                    attackSkill = 5,
                    defenseSkill = 5,
                    foodCapacity = baseFoodCapacity
                ),
                unitName = "test",
                currentFood = currentFood
            )

    private fun makeCity(foodStored: Int): CityInfo {
        val city = CityInfo()
        city.population.foodStored = foodStored
        return city
    }

    @Test
    fun `hero empty city full`() {
        val hero = makeHero(currentFood = 0f)
        val city = makeCity(foodStored = 20)

        SupplyHeroMaximumUseCase.execute(hero, city)

        assertEquals(15f, hero.currentFood, 0.001f)
        assertEquals(5, city.population.foodStored)
    }

    @Test
    fun `hero half full city full`() {
        val hero = makeHero(currentFood = 5f)
        val city = makeCity(foodStored = 20)

        SupplyHeroMaximumUseCase.execute(hero, city)

        assertEquals(15f, hero.currentFood, 0.001f)
        assertEquals(10, city.population.foodStored)
    }

    @Test
    fun `hero full city full`() {
        val hero = makeHero(currentFood = 15f)
        val city = makeCity(foodStored = 20)

        SupplyHeroMaximumUseCase.execute(hero, city)

        assertEquals(15f, hero.currentFood, 0.001f)
        assertEquals(20, city.population.foodStored)
    }

    @Test
    fun `hero empty city empty`() {
        val hero = makeHero(currentFood = 0f)
        val city = makeCity(foodStored = 0)

        SupplyHeroMaximumUseCase.execute(hero, city)

        assertEquals(0f, hero.currentFood, 0.001f)
        assertEquals(0, city.population.foodStored)
    }

    @Test
    fun `hero empty city less than capacity`() {
        val hero = makeHero(currentFood = 0f)
        val city = makeCity(foodStored = 5)

        SupplyHeroMaximumUseCase.execute(hero, city)

        assertEquals(5f, hero.currentFood, 0.001f)
        assertEquals(0, city.population.foodStored)
    }

}

// TODO: что если city.foodStored отрицательный?
// TODO: что если hero.currentFood > baseFoodCapacity (переполнен)?
// Сейчас Use Case это не проверяет — героSpaceLeft уйдёт в минус
// и мы заберём еду у города в пользу отрицательного пространства
