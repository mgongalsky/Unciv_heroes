package com.unciv.testing.city

import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет смену принадлежности на уровне реестра без дополнительных игровых эффектов владения. */
class CityNeutralMembershipTest {
    /** Повторный переход игрок → нейтральный → игрок сохраняет объект, армию, историю и локальный прогресс. */
    @Test
    fun membershipTransitionsPreserveCityIdentityAndLocalState() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity().apply {
                health = 91
                population.foodStored = 17
                currConscription = 13
                setFlag(CityFlags.Resistance, 4)
                cityConstructions.inProgressConstructions["Scout"] = 23
            }
            val registry = fixture.game.cityRegistry
            val id = city.state.id
            val position = city.state.position
            val army = city.state.garrison
            val troop = city.garrisonInfo.getTroopAt(0)
            val history = city.state.history
            val territory = city.tiles.map { it.cpy() }.toSet()
            val created = fixture.civilization.citiesCreated
            repeat(2) {
                registry.neutralize(city)
                assertEquals(CityOwnership.Neutral, city.state.ownership)
                assertNull(city.ownerOrNull)
                assertNull(city.garrisonInfo.ownerOrNull)
                // Нейтральность убирает владельца, но сохраняет привязку города к игровому миру.
                assertSame(fixture.game, city.gameInfo)
                assertTrue(fixture.civilization.cities.isEmpty())
                assertSame(city, registry.byId(id))
                assertSame(city, registry.at(position))

                registry.transfer(city, fixture.civilization)
                assertSame(fixture.civilization, city.civInfo)
                assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
                assertEquals(listOf(city), fixture.civilization.cities)
                assertEquals(listOf(city), registry.all())
                assertEquals(id, city.state.id)
                assertEquals(position, city.state.position)
                assertSame(army, city.state.garrison)
                assertSame(troop, city.garrisonInfo.getTroopAt(0))
                assertEquals(history, city.state.history)
                assertEquals(territory, city.tiles)
                assertEquals(91, city.health)
                assertEquals(17, city.population.foodStored)
                assertEquals(13, city.currConscription)
                assertEquals(4, city.getFlag(CityFlags.Resistance))
                assertEquals(23, city.cityConstructions.inProgressConstructions["Scout"])
                assertEquals(created, fixture.civilization.citiesCreated)
            }
        }
    }

    /** Попытка нейтрализовать незарегистрированный город отклоняется до изменения его владельца. */
    @Test
    fun unregisteredCityIsRejectedBeforeOwnershipChanges() {
        CityTestFixture().use { fixture ->
            val city = CityInfo().apply { civInfo = fixture.civilization }
            try {
                fixture.game.cityRegistry.neutralize(city)
                fail("Unregistered city must be rejected")
            } catch (_: IllegalStateException) {
                assertSame(fixture.civilization, city.ownerOrNull)
                assertEquals(CityOwnership.Player("Rome"), city.state.ownership)
                assertTrue(fixture.game.getCities().none())
            }
        }
    }
}
