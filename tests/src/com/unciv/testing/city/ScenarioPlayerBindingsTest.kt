package com.unciv.testing.city

import com.unciv.Constants
import com.unciv.json.json
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.Player
import com.unciv.pure.domain.city.ScenarioPlayerBindings
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import org.junit.Assert.*
import org.junit.Test

class ScenarioPlayerBindingsTest {
    @Test
    fun explicitAssignmentsSurviveSerializationAndReordering() {
        val first = Player(Constants.random).apply { scenarioSlotId = "first"; playerId = "same-account" }
        val second = Player("Rome").apply { scenarioSlotId = "second"; playerId = "same-account" }
        val parameters = GameParameters().apply { players = arrayListOf(second, first) }
        val restored = json().fromJson(GameParameters::class.java, json().toJson(parameters.clone()))
        val slots = listOf(ScenarioPlayerSlot("first"), ScenarioPlayerSlot("second"))
        val bindings = ScenarioPlayerBindings.resolve(slots, restored.players.map { it.scenarioSlotId to it })
        assertEquals(Constants.random, bindings.getValue("first").chosenCiv)
        assertEquals("Rome", bindings.getValue("second").chosenCiv)
        assertNotSame(bindings.getValue("first"), bindings.getValue("second"))
    }

    @Test
    fun missingDuplicateAndUnknownAssignmentsAreRejected() {
        val slots = listOf(ScenarioPlayerSlot("first"))
        for ((assignments, expected) in listOf(
            emptyList<Pair<String, String>>() to "no participant",
            listOf("first" to "Rome", "first" to "Greece") to "more than once",
            listOf("missing" to "Rome") to "Unknown scenario player slot"
        )) {
            try {
                ScenarioPlayerBindings.resolve(slots, assignments)
                fail("Invalid assignments must be rejected")
            } catch (invalid: IllegalArgumentException) {
                assertTrue(invalid.message.orEmpty().contains(expected))
            }
        }
    }
}
