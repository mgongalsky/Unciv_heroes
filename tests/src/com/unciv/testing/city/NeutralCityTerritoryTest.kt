package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityInfo
import com.unciv.logic.map.RoadStatus
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityTerritoryTest {
    @Test
    fun repeatedRestorationBindsTerritoryWithoutPlayerOrFoundingEffects() {
        CityTestFixture().use { fixture ->
            val city = CityInfo().apply {
                state.ownership = CityOwnership.Neutral
                state.garrison = Army(2)
                location = Vector2(3f, 0f)
                tiles.add(location)
                tiles.add(fixture.game.tileMap[location].neighbors.first().position.cpy())
                population.foodStored = 17
                health = 91
                cityConstructions.constructionQueue.add("Scout")
            }
            val road = fixture.game.tileMap[city.tiles.first { it != city.location }]
            road.roadStatus = RoadStatus.Road
            road.roadOwner = fixture.civilization.civName
            val created = fixture.civilization.citiesCreated
            val territory = city.tiles.map { it.cpy() }.toSet()
            city.bindNeutralWorld(fixture.game)

            repeat(2) {
                city.setTransients()
                fixture.game.tileMap.setNeutralTransients()
                assertSame(city, city.getCenterTile().getCity())
                assertTrue(city.getCenterTile().isCityCenter())
                for (tile in city.getTiles()) {
                    assertSame(city, tile.getCity())
                    assertNull(tile.getOwner())
                }
                assertNull(city.garrisonInfo.ownerOrNull)
                assertSame(city, city.population.cityInfo)
                assertSame(city, city.expansion.cityInfo)
                assertSame(city, city.cityConstructions.cityInfo)
                assertSame(city, city.religion.cityInfo)
                assertSame(city, city.espionage.cityInfo)
                assertEquals(territory, city.tiles)
                assertEquals(17, city.population.foodStored)
                assertEquals(91, city.health)
                assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
                assertEquals(created, fixture.civilization.citiesCreated)
                assertTrue(fixture.civilization.cities.isEmpty())
                assertEquals(fixture.civilization.civName, road.roadOwner)
                assertTrue(road.position in fixture.civilization.neutralRoads)
            }
        }
    }
}
