package com.unciv.testing.city

import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.city.CityHistory
import com.unciv.pure.domain.city.CityId
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.CityState
import com.unciv.pure.domain.troop.Formation
import com.unciv.pure.domain.troop.Troop
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
        assertNull(state.garrison)
        assertEquals(state, state.deepCopy())
    }

    @Test
    fun copyKeepsHistoryAndAllowsIndependentLocalChanges() {
        val original = CityState(CityId("rome"), "Rome", CityHistory("Rome", "Greece", 7), 120)
        val copy = original.deepCopy()
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

    @Test
    fun snapshotOwnsIndependentGarrisonAndPreservesHistory() {
        val army = Army(3)
        val troop = Troop(
            unitName = "Guard", amount = 10, maxHealth = 100,
            formation = Formation(40, 100),
            formationHealthPercent = 10, formationDamageReductionPercent = 20
        )
        army.setTroopAt(1, troop)
        val original = CityState(
            CityId("city-1"), "Fort", CityHistory("Rome", "Greece", 7), 120, army
        )
        val snapshot = original.deepCopy()
        val copiedArmy = snapshot.garrison!!
        val copiedTroop = copiedArmy.getTroopAt(1)!!
        assertEquals(original.id, snapshot.id)
        assertEquals(original.history, snapshot.history)
        assertEquals(original.name, snapshot.name)
        assertEquals(original.health, snapshot.health)
        assertNotSame(army, copiedArmy)
        assertNotSame(troop, copiedTroop)
        assertNotSame(troop.formation, copiedTroop.formation)
        assertEquals(3, copiedArmy.maxSlots)
        assertNull(copiedArmy.getTroopAt(0))
        assertNull(copiedArmy.getTroopAt(2))
        copiedTroop.currentAmount = 2
        copiedTroop.formation.current = 0
        copiedArmy.setTroopAt(1, null)
        snapshot.history = snapshot.history.copy(previousPlayerId = "Egypt")
        assertSame(troop, army.getTroopAt(1))
        assertEquals(10, troop.currentAmount)
        assertEquals(40, troop.formation.current)
        assertEquals("Greece", original.history.previousPlayerId)
    }
}
