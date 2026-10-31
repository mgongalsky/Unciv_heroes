package com.unciv.testing.city

import com.unciv.UncivGame
import com.unciv.models.ruleset.RulesetCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.GlobalContext

/** Проверяет саму фикстуру: она создаёт настоящий город и не оставляет глобальные зависимости теста. */
class CityTestFixtureTest {
    /** Основание связывает владельца и центр, а закрытие возвращает Current, кеш правил и DI. */
    @Test
    fun fixtureFoundsARealCityAndRestoresGlobals() {
        val currentField = UncivGame::class.java.getDeclaredField("Current").apply { isAccessible = true }
        val previousCurrent = currentField.get(null)
        val previousRulesets = HashMap(RulesetCache)

        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()

            assertSame(city, fixture.civilization.cities.single())
            assertSame(fixture.civilization, city.civInfo)
            assertSame(city, fixture.game.tileMap[city.location].getCity())
            assertTrue(city.getCenterTile().isCityCenter())
            println("City fixture probe: population=${city.population.population}, tiles=${city.tiles.size}, buildings=${city.cityConstructions.builtBuildings.sorted()}")
        }

        assertSame(previousCurrent, currentField.get(null))
        assertEquals(previousRulesets.keys, RulesetCache.keys)
        for ((name, ruleset) in previousRulesets) assertSame(ruleset, RulesetCache[name])
        assertTrue(GlobalContext.getOrNull() == null)
    }

    /** Исключение внутри use не отменяет очистку и возвращается вызывающему коду без подмены. */
    @Test
    fun fixtureRestoresGlobalsWhenTestBodyThrows() {
        val currentField = UncivGame::class.java.getDeclaredField("Current").apply { isAccessible = true }
        val previousCurrent = currentField.get(null)
        val sentinel = IllegalStateException("Expected test body failure")
        var caught = false

        try {
            CityTestFixture().use { throw sentinel }
        } catch (failure: IllegalStateException) {
            assertSame(sentinel, failure)
            caught = true
        }

        assertTrue(caught)
        assertSame(previousCurrent, currentField.get(null))
        assertTrue(GlobalContext.getOrNull() == null)
    }
}
