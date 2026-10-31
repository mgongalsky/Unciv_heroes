package com.unciv.testing.pure.application.scenario

import com.unciv.pure.application.scenario.ScenarioPlayerBindings
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import org.junit.Assert.*
import org.junit.Test

/** Назначения участников проверяются в памяти, независимо от сохранений и игровых цивилизаций. */
class ScenarioPlayerBindingsTest {
    private val slots = listOf(ScenarioPlayerSlot("first"), ScenarioPlayerSlot("second"))

    /** Порядок участников не определяет владельца; участники без слота пропускаются. */
    @Test
    fun resolvesExplicitIdsAndIgnoresUnassignedParticipants() {
        val assignments = listOf("second" to "B", "" to "Observer", "first" to "A")
        val before = assignments.toList()
        assertEquals(
            mapOf("first" to "A", "second" to "B"),
            ScenarioPlayerBindings.resolve(slots, assignments)
        )
        assertEquals(before, assignments)
    }

    /** Каждый объявленный слот требует ровно одного известного назначения. */
    @Test fun rejectsMissingDuplicateAndUnknownAssignments() {
        rejects { ScenarioPlayerBindings.resolve(slots, listOf("first" to "A")) }
        rejects { ScenarioPlayerBindings.resolve(slots, listOf("first" to "A", "first" to "B")) }
        rejects { ScenarioPlayerBindings.resolve(slots, listOf("unknown" to "A")) }
    }

    /** Пустые и повторные ID карты недопустимы даже без городов. */
    @Test fun rejectsInvalidSlotIdentities() {
        rejects { ScenarioPlayerBindings.resolve(listOf(ScenarioPlayerSlot(" ")), emptyList<Pair<String, String>>()) }
        rejects { ScenarioPlayerBindings.resolve(listOf(ScenarioPlayerSlot("a"), ScenarioPlayerSlot("a")), listOf("a" to "A")) }
    }

    /** Пустой сценарий допускает участников без сценарного слота. */
    @Test fun emptyScenarioNeedsNoAssignments() {
        assertTrue(ScenarioPlayerBindings.resolve(emptyList(), listOf("" to "A")).isEmpty())
    }

    private fun rejects(action: () -> Unit) {
        try { action(); fail("Expected invalid bindings to be rejected") }
        catch (_: IllegalArgumentException) { }
    }
}
