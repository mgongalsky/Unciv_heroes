package com.unciv.testing.city

import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercises the JSON construction path without a game, a GDX runner, or DI setup. */
class CityConstructionTest {
    @Test
    fun defaultConstructorCreatesUnregisteredCityState() {
        val city = CityInfo()

        assertTrue(city.id.isNotBlank())
        assertEquals("", city.name)
        assertEquals("", city.foundingCiv)
        assertEquals(200, city.health)
        assertEquals(1, city.population.population)
        assertEquals(0, city.population.foodStored)
        assertTrue(city.tiles.isEmpty())
        assertTrue(city.workedTiles.isEmpty())
        assertTrue(city.lockedTiles.isEmpty())
        assertTrue(city.garrison.isEmpty())
        assertFalse(city.isOriginalCapital)
        assertFalse(city.isInResistance())
    }

    @Test
    fun separatelyConstructedCitiesDoNotSharePopulationOrConstructionState() {
        val first = CityInfo()
        val second = CityInfo()

        assertNotSame(first.population, second.population)
        assertNotSame(first.cityConstructions, second.cityConstructions)
        assertNotSame(first.tiles, second.tiles)
        assertNotSame(first.garrison, second.garrison)

        first.population.addFoodPoints(17)
        first.cityConstructions.constructionQueue.add("Construction probe")

        assertEquals(17, first.population.foodStored)
        assertEquals(0, second.population.foodStored)
        assertTrue(second.cityConstructions.constructionQueue.isEmpty())
    }

    @Test
    fun resistanceFlagCanBeChangedWithoutAttachingAGame() {
        val city = CityInfo()
        val otherCity = CityInfo()

        city.setFlag(CityFlags.Resistance, 3)

        assertTrue(city.isInResistance())
        assertEquals(3, city.getFlag(CityFlags.Resistance))
        assertFalse(otherCity.isInResistance())

        city.removeFlag(CityFlags.Resistance)

        assertFalse(city.isInResistance())
    }
}
