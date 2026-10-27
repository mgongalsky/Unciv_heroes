package com.unciv.testing.city

import com.unciv.pure.domain.city.CityHistory
import com.unciv.pure.domain.city.CityId
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.CityState
import org.junit.Assert.*
import org.junit.Test

class CityStateTest {
    @Test
    fun stateCanBeCreatedWithoutGameOrGeneratedIdentity() {
        val state = CityState(CityId("scenario-city-1"))
        assertEquals("scenario-city-1", state.id.value)
        assertEquals("", state.name)
        assertEquals(CityHistory(), state.history)
        assertEquals(200, state.health)
    }

    @Test
    fun copyKeepsHistoryAndAllowsIndependentLocalChanges() {
        val original = CityState(CityId("rome"), "Rome", CityHistory("Rome", "Greece", 7), 120)
        val copy = original.copy()
        assertEquals(original, copy)
        assertNotSame(original, copy)
        copy.name = "Renamed"
        copy.health = 50
        copy.history = copy.history.copy(previousPlayerId = "Egypt", turnAcquired = 8)
        assertEquals("Rome", original.name)
        assertEquals(120, original.health)
        assertEquals(CityHistory("Rome", "Greece", 7), original.history)
        assertEquals(original.id, copy.id)
    }

    @Test
    fun positionAndOwnershipAreValuesIndependentOfCivilizations() {
        assertEquals(CityPosition(2f, -1f), CityPosition(2f, -1f))
        assertEquals(CityOwnership.Player("slot-1"), CityOwnership.Player("slot-1"))
        assertNotEquals(CityOwnership.Neutral, CityOwnership.Player("slot-1"))
    }
}
