package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityAcquisitionTest {
    private fun neutralCity(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2)
        cityConstructions.builtBuildings.add("Granary")
        cityConstructions.constructionQueue.add("Scout")
        cityConstructions.inProgressConstructions["Scout"] = 23
        health = 91
        population.foodStored = 17
        currConscription = 13
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    @Test
    fun acquisitionPreservesLocalStateWithoutConquestRewardsOrLosses() {
        CityTestFixture().use { fixture ->
            val capital = fixture.foundCity()
            val city = neutralCity(fixture)
            val owner = fixture.civilization
            fixture.game.turns = 7
            val id = city.id
            val army = city.state.garrison
            val buildings = city.cityConstructions.builtBuildings.toSet()
            val population = city.population.population
            val gold = owner.gold
            val created = owner.citiesCreated
            val diplomacy = owner.diplomacy.toMap()
            NeutralCityOwnership.acquireUndefended(city, owner)
            assertEquals(id, city.id)
            assertSame(city, fixture.game.cityRegistry.byId(city.state.id))
            assertSame(owner, city.civInfo)
            assertSame(owner, city.garrisonInfo.ownerOrNull)
            assertSame(army, city.state.garrison)
            assertSame(owner, city.getCenterTile().getOwner())
            assertSame(capital, owner.getCapital())
            assertEquals(buildings, city.cityConstructions.builtBuildings)
            assertEquals(population, city.population.population)
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(13, city.currConscription)
            assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
            assertEquals(mapOf("Scout" to 23), city.cityConstructions.inProgressConstructions)
            assertFalse(city.hasFlag(CityFlags.Resistance))
            assertEquals(7, city.turnAcquired)
            assertEquals("", city.previousOwner)
            assertEquals(gold, owner.gold)
            assertEquals(created, owner.citiesCreated)
            assertEquals(diplomacy, owner.diplomacy)
        }
    }

    @Test
    fun livingGarrisonRejectsAcquisitionWithoutChangingWorld() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = neutralCity(fixture)
            city.garrisonInfo.addUnits("Archer", 1)
            val history = city.state.history
            try {
                NeutralCityOwnership.acquireUndefended(city, fixture.civilization)
                fail("A defended city requires victory first")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message!!.contains("defending garrison"))
                assertEquals(CityOwnership.Neutral, city.state.ownership)
                assertEquals(history, city.state.history)
                assertNull(city.ownerOrNull)
                assertNull(city.garrisonInfo.ownerOrNull)
                assertNull(city.getCenterTile().getOwner())
                assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.amount)
                assertFalse(city in fixture.civilization.cities)
            }
        }
    }
}
