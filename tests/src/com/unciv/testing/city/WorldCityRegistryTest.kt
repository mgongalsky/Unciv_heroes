package com.unciv.testing.city

import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityPosition
import org.junit.Assert.*
import org.junit.Test

/** Проверяет единый учёт городов мира и совместимость прежнего свойства CivilizationInfo.cities. */
class WorldCityRegistryTest {
    /** Ожидает отказ проверки дубликатов, а не произвольное исключение. */
    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Expected duplicate city to be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }

    /** Основание регистрирует город один раз; разрушение убирает его из всех актуальных представлений. */
    @Test
    fun foundedCityIsRegisteredOnceAndRemovedFromEveryView() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val owner = city.civInfo
            val game = owner.gameInfo
            val registry = game.cityRegistry
            repeat(3) {
                assertSame(registry, game.cityRegistry)
                assertEquals(listOf(city), game.getCities().toList())
                assertEquals(listOf(city), owner.cities)
                assertSame(city, registry.byId(city.state.id))
                assertSame(city, registry.at(city.state.position))
            }
            // Выданный ранее список является снимком состава и не меняется при удалении из реестра.
            val beforeRemoval = owner.cities
            city.destroyCity(overrideSafeties = true)
            assertTrue(owner.cities.isEmpty())
            assertTrue(game.getCities().none())
            assertNull(registry.byId(city.state.id))
            assertNull(registry.at(city.state.position))
            assertEquals(listOf(city), beforeRemoval)
        }
    }

    /** Ошибочная замена списка откатывает временную привязку владельца и сохраняет прежний состав. */
    @Test
    fun invalidCompatibilityReplacementLeavesMembershipAndOwnershipIntact() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val owner = city.civInfo
            val registry = owner.gameInfo.cityRegistry
            val duplicate = CityInfo().apply {
                id = city.id
                state.position = CityPosition(2f, 0f)
            }
            rejects { owner.cities = listOf(city, duplicate) }
            assertNull(duplicate.state.ownership)
            assertEquals(listOf(city), owner.cities)
            assertEquals(listOf(city), registry.all())
            assertSame(city, registry.byId(city.state.id))
            assertSame(owner, city.civInfo)
        }
    }

    /** Совместимый setter обновляет реестр, не удерживая изменяемый входной список вызывающей стороны. */
    @Test
    fun compatibilityReplacementCopiesInputAndUpdatesRegistry() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val owner = city.civInfo
            val registry = owner.gameInfo.cityRegistry
            val replacement = CityInfo().apply {
                state.position = CityPosition(2f, 0f)
            }
            val input = arrayListOf(replacement)
            owner.cities = input
            input.clear()
            assertEquals(listOf(replacement), owner.cities)
            assertSame(replacement, registry.byId(replacement.state.id))
            assertSame(replacement, registry.at(replacement.state.position))
            assertNull(registry.byId(city.state.id))
            assertSame(owner, replacement.civInfo)
            // Setter меняет только состав реестра; он не основывает город и не перестраивает территорию.
            assertEquals(1, owner.citiesCreated)
            owner.cities = listOf(city)
        }
    }
}
