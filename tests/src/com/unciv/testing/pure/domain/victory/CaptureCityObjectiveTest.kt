package com.unciv.testing.pure.domain.victory

import com.unciv.pure.domain.scenario.ScenarioVictorySettings
import com.unciv.pure.domain.scenario.ScenarioVictoryRule
import com.unciv.pure.domain.victory.CaptureCityObjective
import org.junit.Assert.*
import org.junit.Test

/** Проверяет владение несколькими целями и изоляцию исходных настроек от копий. */
class CaptureCityObjectiveTest {
    /** Победа требует всех целей данного слота, но не целей другого участника. */
    @Test fun allAssignedCitiesMustBelongToParticipant() {
        val objectives = listOf(
            CaptureCityObjective("a", arrayListOf("one")),
            CaptureCityObjective("b", arrayListOf("one")),
            CaptureCityObjective("c", arrayListOf("two"))
        )
        assertTrue(CaptureCityObjective.isCompleted(objectives, "one", "Rome", mapOf("a" to "Rome", "b" to "Rome")))
        assertFalse(CaptureCityObjective.isCompleted(objectives, "one", "Rome", mapOf("a" to "Rome", "b" to "Greece")))
        assertFalse(CaptureCityObjective.isCompleted(objectives, "two", "Rome", mapOf("c" to "Greece")))
    }

    /** Отсутствующие, нейтральные и не назначенные участнику цели не дают ложной победы. */
    @Test fun missingNeutralAndEmptyObjectivesDoNotComplete() {
        val objectives = listOf(CaptureCityObjective("a", arrayListOf("one")))
        assertFalse(CaptureCityObjective.isCompleted(objectives, "one", "Rome", emptyMap()))
        assertFalse(CaptureCityObjective.isCompleted(objectives, "one", "Rome", mapOf("a" to null)))
        assertFalse(CaptureCityObjective.isCompleted(objectives, "other", "Rome", mapOf("a" to "Rome")))
        assertFalse(CaptureCityObjective.isCompleted(emptyList(), "one", "Rome", emptyMap()))
    }

    /** Изменение вложенных данных копии не меняет настройки исходной карты. */
    @Test fun deepCopySeparatesRulesAndEligibleSlots() {
        val original = ScenarioVictorySettings(
            arrayListOf(ScenarioVictoryRule("CaptureCity")),
            arrayListOf(CaptureCityObjective("a", arrayListOf("one")))
        )
        val copy = original.deepCopy()
        copy.rules.single().defaultEnabled = false
        copy.captureObjectives.single().eligibleSlotIds.add("two")
        copy.captureObjectives.single().cityId = "b"
        assertTrue(original.rules.single().defaultEnabled)
        assertEquals(listOf("one"), original.captureObjectives.single().eligibleSlotIds)
        assertEquals("a", original.captureObjectives.single().cityId)
    }
}
