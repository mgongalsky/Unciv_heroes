package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.logic.civilization.Spy
import com.unciv.logic.civilization.SpyAction
import org.junit.Assert.*
import org.junit.Test

class NeutralCityEspionageTest {
    @Test
    fun spyReturnsToHideoutWhenItsCityBecomesNeutral() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = fixture.foundCity(Vector2(4f, 0f))
            val spy = Spy("Test spy").apply { setTransients(fixture.civilization) }
            spy.moveTo(city)
            spy.endTurn()
            assertEquals(SpyAction.EstablishNetwork, spy.action)

            NeutralCityOwnership.neutralize(city)
            spy.endTurn()

            assertNull(spy.location)
            assertEquals(SpyAction.None, spy.action)
            assertEquals(0, spy.timeTillActionFinish)
        }
    }

    @Test
    fun rejectedNeutralAssignmentPreservesPreviousSpyState() {
        CityTestFixture().use { fixture ->
            val capital = fixture.foundCity()
            val city = fixture.foundCity(Vector2(4f, 0f))
            NeutralCityOwnership.neutralize(city)
            val spy = Spy("Test spy").apply { setTransients(fixture.civilization) }
            spy.moveTo(capital)
            val action = spy.action
            val remaining = spy.timeTillActionFinish
            try {
                spy.moveTo(city)
                fail("Neutral city must not accept a spy assignment")
            } catch (_: IllegalArgumentException) {
                assertEquals(capital.id, spy.location)
                assertEquals(action, spy.action)
                assertEquals(remaining, spy.timeTillActionFinish)
                assertNull(city.ownerOrNull)
            }
        }
    }
}
