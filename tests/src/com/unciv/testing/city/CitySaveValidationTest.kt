package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.CitySaveValidation
import org.junit.Assert.*
import org.junit.Test

class CitySaveValidationTest {
    private fun rejects(message: String, action: () -> Unit) {
        val failure = try { action(); null } catch (error: IllegalArgumentException) { error }
        assertNotNull("Expected $message", failure)
        assertTrue(failure!!.message.orEmpty(), failure.message.orEmpty().contains(message))
    }

    @Test
    fun validTerritoryIsAcceptedWithoutChangingTileOwnership() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            repeat(2) { CitySaveValidation.validateTerritory(listOf(city), fixture.game.tileMap) }
            assertSame(city, city.getCenterTile().getCity())
            assertEquals(1, fixture.civilization.citiesCreated)
        }
    }

    @Test
    fun missingCenterAndOutOfMapTerritoryAreRejected() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val territory = city.tiles
            city.tiles = HashSet(territory).apply { remove(city.location) }
            rejects("center is missing") { CitySaveValidation.validateTerritory(listOf(city), fixture.game.tileMap) }
            city.tiles = HashSet(territory).apply { add(Vector2(999f, 999f)) }
            rejects("outside the map") { CitySaveValidation.validateTerritory(listOf(city), fixture.game.tileMap) }
            city.tiles = territory
        }
    }

    @Test
    fun overlappingTerritoryIsRejectedBeforeOwnershipChanges() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val shared = city.getTiles().first { it.position != city.location }
            val other = CityInfo().apply {
                location = shared.position
                tiles.add(location)
            }
            rejects("also claimed by city") {
                CitySaveValidation.validateTerritory(listOf(city, other), fixture.game.tileMap)
            }
            assertSame(city, shared.getCity())
            assertSame(city, fixture.civilization.cities.single())
        }
    }
}
