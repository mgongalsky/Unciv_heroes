package com.unciv.testing.pure.application.scenario

import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.application.scenario.ScenarioPlayerSlotDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

/** Проверяет начальное распределение слотов в UI до строгой проверки назначений при запуске. */
class ScenarioPlayerSlotDefaultsTest {
    private val slots = listOf(ScenarioPlayerSlot("first", "Player 1"), ScenarioPlayerSlot("second", "Player 2"))

    /** Автоматические назначения идут по порядку игроков; лишние участники остаются без слота. */
    @Test
    fun assignsInPlayerOrderAndLeavesExtraPlayersWithoutSlots() {
        assertEquals(listOf("first", "second", ""), ScenarioPlayerSlotDefaults.assign(slots, listOf("", "", "")))
    }

    /** Ручной выбор резервирует слот до распределения остальных, даже если выбран у более позднего игрока. */
    @Test
    fun preservesManualChoicesBeforeFillingFreeSlots() {
        assertEquals(listOf("second", "first"), ScenarioPlayerSlotDefaults.assign(slots, listOf("", "first")))
        assertEquals(listOf("second", "first"), ScenarioPlayerSlotDefaults.assign(slots, listOf("second", "first")))
    }

    /** null исключает участника из автоподбора: так сохраняются явное отсутствие назначения и наблюдатели. */
    @Test
    fun respectsExplicitlyUnassignedPlayersAndSpectators() {
        assertEquals(listOf(null, "first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf(null, "", "")))
    }

    /** Устаревшие и повторные назначения исправляются в предсказуемом порядке доступных слотов. */
    @Test
    fun repairsStaleAndDuplicateAssignmentsDeterministically() {
        assertEquals(listOf("first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf("first", "first")))
        assertEquals(listOf("first", "second"), ScenarioPlayerSlotDefaults.assign(slots, listOf("old-map", "")))
    }
}
