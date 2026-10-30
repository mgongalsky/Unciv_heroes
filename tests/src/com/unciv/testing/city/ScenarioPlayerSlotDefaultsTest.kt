package com.unciv.testing.city

import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.city.ScenarioPlayerSlotDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioPlayerSlotDefaultsTest {
    private val slots = listOf(ScenarioPlayerSlot("first", "Player 1"), ScenarioPlayerSlot("second", "Player 2"))

    @Test
    fun assignsInPlayerOrderAndLeavesExtraPlayersWithoutSlots() {
        assertEquals(listOf("first", "second", ""), ScenarioPlayerSlotDefaults.assign(slots, listOf("", "", "")))
    }

    @Test
    fun preservesManualChoicesBeforeFillingFreeSlots() {
        assertEquals(listOf("second", "first"), ScenarioPlayerSlotDefaults.assign(slots, listOf("", "first")))
        assertEquals(listOf("second", "first"), ScenarioPlayerSlotDefaults.assign(slots, listOf("second", "first")))
    }

    @Test
    fun respectsExplicitlyUnassignedPlayersAndSpectators() {
        assertEquals(listOf(null, "first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf(null, "", "")))
    }

    @Test
    fun repairsStaleAndDuplicateAssignmentsDeterministically() {
        assertEquals(listOf("first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf("first", "first")))
        assertEquals(listOf("first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf("old-map", "")))
    }
}
