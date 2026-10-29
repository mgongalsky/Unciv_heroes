package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityReleaseTest {
    @Test
    fun releasingLastCityPreservesLocalStateAndClearsCapitalConnections() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity().apply {
                health = 91
                population.foodStored = 17
                currConscription = 13
                setFlag(CityFlags.Resistance, 4)
                cityConstructions.inProgressConstructions["Scout"] = 23
            }
            val id = city.state.id
            val buildings = city.cityConstructions.builtBuildings.toSet()
            val territory = city.tiles.toSet()
            val army = city.state.garrison
            val troop = city.garrisonInfo.getTroopAt(0)
            val turn = fixture.game.turns
            fixture.civilization.initialSetCitiesConnectedToCapitalTransients()

            NeutralCityOwnership.neutralize(city)

            assertEquals(CityOwnership.Neutral, city.state.ownership)
            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
            assertSame(army, city.state.garrison)
            assertSame(troop, city.garrisonInfo.getTroopAt(0))
            assertSame(city, fixture.game.cityRegistry.byId(id))
            assertTrue(fixture.civilization.cities.isEmpty())
            assertTrue(fixture.civilization.citiesConnectedToCapitalToMediums.isEmpty())
            assertNull(fixture.civilization.getCapital())
            assertEquals(buildings, city.cityConstructions.builtBuildings.toSet())
            assertEquals(territory, city.tiles.toSet())
            assertTrue(city.getTiles().all { it.getCity() === city && it.getOwner() == null })
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(13, city.currConscription)
            assertEquals(4, city.getFlag(CityFlags.Resistance))
            assertEquals(23, city.cityConstructions.inProgressConstructions["Scout"])
            assertEquals("Rome", city.previousOwner)
            assertEquals(turn, city.turnAcquired)
            assertEquals(turn, fixture.game.turns)
        }
    }

    @Test
    fun releasingCapitalAssignsRemainingCityWithoutRemovingNeutralBuildings() {
        CityTestFixture().use { fixture ->
            val capital = fixture.foundCity()
            val remaining = fixture.foundCity(Vector2(4f, 0f))
            val buildings = capital.cityConstructions.builtBuildings.toSet()
            assertSame(capital, fixture.civilization.getCapital())

            NeutralCityOwnership.neutralize(capital)

            assertNull(capital.ownerOrNull)
            assertEquals(buildings, capital.cityConstructions.builtBuildings.toSet())
            assertSame(remaining, fixture.civilization.getCapital())
            assertEquals(listOf(remaining), fixture.civilization.cities)
            assertFalse(fixture.civilization.citiesConnectedToCapitalToMediums.containsKey(capital))
            assertSame(capital, fixture.game.cityRegistry.byId(capital.state.id))
        }
    }
}
