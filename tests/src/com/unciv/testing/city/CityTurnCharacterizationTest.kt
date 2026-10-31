package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityFlags
import com.unciv.models.stats.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяет наблюдаемые эффекты начала и конца хода, накопления культуры и разрушения города. */
class CityTurnCharacterizationTest {
    /** Закрепляет обновление флагов, производство, рост и лечение, затем освобождение территории. */
    @Test
    fun turnUpdatesFlagsProductionPopulationAndHealingBeforeDestruction() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val successor = fixture.foundCity(Vector2(4f, 0f))
            city.health = 50
            city.attackedThisTurn = true
            city.population.foodStored = 2
            city.setFlag(CityFlags.Resistance, 2)
            city.setFlag(CityFlags.ResourceDemand, 10)
            city.cityConstructions.constructionQueue.clear()
            city.cityConstructions.constructionQueue.add("Granary")
            val initialTerritory = city.tiles.toSet()

            city.startTurn()

            assertEquals(50, city.health)
            assertFalse(city.attackedThisTurn)
            assertEquals(1, city.getFlag(CityFlags.Resistance))
            assertEquals(9, city.getFlag(CityFlags.ResourceDemand))
            assertEquals(1f, city.cityStats.currentCityStats.production, 0f)
            assertEquals(0f, city.cityStats.currentCityStats.food, 0f)
            // Задаём доход конца хода явно, чтобы проверить его применение независимо от расчёта статистики.
            city.cityStats.currentCityStats = Stats(food = 4f, production = 3f, culture = 50f)

            city.endTurn()

            assertEquals(70, city.health)
            assertEquals(2, city.population.population)
            assertEquals(1, city.population.foodStored)
            assertEquals(50, city.expansion.cultureStored)
            assertFalse(initialTerritory == city.tiles)
            assertEquals(mapOf("Scout" to 3), city.cityConstructions.inProgressConstructions)
            val territoryBeforeDestruction = city.tiles.toSet()

            // Исходную столицу защищает обычный вызов; принудительное разрушение обходит эту защиту.
            city.destroyCity()
            assertTrue(fixture.civilization.cities.contains(city))
            city.destroyCity(overrideSafeties = true)

            assertEquals(listOf(successor), fixture.civilization.cities)
            assertTrue(city.tiles.isEmpty())
            for (position in territoryBeforeDestruction) {
                assertNull(fixture.game.tileMap[position].owningCity)
            }
            assertEquals("City ruins", city.getCenterTile().improvement)
            assertFalse(city.getCenterTile().isCityCenter())
            assertTrue(successor.isCapital())
            assertFalse(successor.isOriginalCapital)
        }
    }

    /** Превышение порога культуры само по себе не расширяет город; рост населения здесь исключён. */
    @Test
    fun cultureAccumulatesWithoutAutomaticallyClaimingTiles() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val territory = city.tiles.toSet()
            val threshold = city.expansion.getCultureToNextTile()

            city.expansion.nextTurn(threshold.toFloat() + 50f)

            assertEquals(threshold + 50, city.expansion.cultureStored)
            assertEquals(territory, city.tiles)
            assertEquals(1, city.population.population)
        }
    }
}
