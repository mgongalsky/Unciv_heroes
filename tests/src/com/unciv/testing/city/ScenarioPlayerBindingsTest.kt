package com.unciv.testing.city

import com.unciv.Constants
import com.unciv.json.json
import com.unciv.models.metadata.GameParameters
import com.unciv.models.metadata.Player
import com.unciv.pure.application.scenario.ScenarioPlayerBindings
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import org.junit.Assert.*
import org.junit.Test

/** Проверяет явное соответствие слотов карты участникам запуска независимо от нации и учётной записи. */
class ScenarioPlayerBindingsTest {
    /** Назначения переживают копирование и JSON, даже если порядок игроков изменён, а аккаунт совпадает. */
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

    /** Каждый слот требует ровно одного назначения; неизвестные ID не допускаются. */
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
