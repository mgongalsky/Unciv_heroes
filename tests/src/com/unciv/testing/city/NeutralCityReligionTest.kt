package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.models.Religion
import org.junit.Assert.*
import org.junit.Test

class NeutralCityReligionTest {
    @Test
    fun neutralCityRetainsReadableReligionWithoutPressureUpdatesOrFounderIncome() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = fixture.foundCity(Vector2(4f, 0f))
            val religion = Religion("Test religion", fixture.game, fixture.civilization.civName)
            fixture.game.religions[religion.name] = religion
            fixture.civilization.religionManager.religion = religion
            city.religion.addPressure(religion.name, 1000, shouldUpdateFollowers = false)
            city.religion.setTransients(city)
            assertSame(religion, city.religion.getMajorityReligion())
            val pressures = city.religion.getPressures().toMap()
            val followers = city.religion.getNumberOfFollowers().toMap()

            NeutralCityOwnership.neutralize(city)
            city.religion.addPressure(religion.name, 500)
            city.religion.endTurn()

            assertNull(city.ownerOrNull)
            assertSame(religion, city.religion.getMajorityReligion())
            assertEquals(pressures, city.religion.getPressures().toMap())
            assertEquals(followers, city.religion.getNumberOfFollowers().toMap())
            assertTrue(city.religion.getPressuresFromSurroundingCities().isEmpty())
            assertEquals(0, fixture.civilization.religionManager.numberOfCitiesFollowingThisReligion())
            assertEquals(0, fixture.civilization.religionManager.numberOfFollowersFollowingThisReligion("in foreign cities"))
        }
    }
}
