package com.unciv.testing.city

import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет пустое создание CityInfo для JSON без игры, графического runner и DI. */
class CityConstructionTest {
    /** Конструктор создаёт локальные значения по умолчанию, но не основывает город на карте. */
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

    /** Пища, очередь и коллекции двух новых городов не должны разделять изменяемое состояние. */
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

    /** Флаг сопротивления — локальные данные: его установка и снятие не требуют игрового мира. */
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
