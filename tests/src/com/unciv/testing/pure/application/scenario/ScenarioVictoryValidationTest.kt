package com.unciv.testing.pure.application.scenario

import com.unciv.pure.application.scenario.ValidateScenarioUseCase
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import org.junit.Assert.*
import org.junit.Test

/** Проверяет ссылки победы на небольшом сценарии без движка и файлов. */
class ScenarioVictoryValidationTest {
    private val position = CityPosition(0f, 0f)
    private val city = ScenarioCity(id = "city", name = "Fort", position = position,
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle")
    private val slots = listOf(ScenarioPlayerSlot("one"), ScenarioPlayerSlot("two"))
    private fun settings() = ScenarioVictorySettings(
        rules = arrayListOf(ScenarioVictoryRule("CaptureCity", ScenarioVictoryMode.Required)),
        captureObjectives = arrayListOf(CaptureCityObjective("city", arrayListOf("one", "two")))
    )
    private fun validate(settings: ScenarioVictorySettings) =
        ValidateScenarioUseCase.execute(listOf(city), slots, setOf(position), settings)

    /** Два участника могут бороться за одну цель; валидация не изменяет исходные настройки. */
    @Test fun sharedObjectiveIsAcceptedWithoutMutation() {
        val settings = settings()
        val before = settings.deepCopy()
        validate(settings)
        assertEquals(before, settings)
    }

    /** Битые ссылки, пустые назначения и дубликаты отклоняются без исправления входных данных. */
    @Test fun invalidReferencesAndDuplicatesAreRejectedWithoutMutation() {
        val mutations: List<(ScenarioVictorySettings) -> Unit> = listOf(
            { it.captureObjectives.single().cityId = "missing" },
            { it.captureObjectives.single().eligibleSlotIds.add("missing") },
            { it.captureObjectives.single().eligibleSlotIds.clear() },
            { it.captureObjectives.single().eligibleSlotIds.add("one") },
            { it.captureObjectives.add(it.captureObjectives.single().deepCopy()) },
            { it.captureObjectives.clear() },
            { it.rules.add(it.rules.single().copy()) },
            { it.rules.single().victoryType = " " }
        )
        for ((index, mutate) in mutations.withIndex()) {
            val invalid = settings().also(mutate)
            val before = invalid.deepCopy()
            try {
                validate(invalid)
                fail("Expected rejection for case $index")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, invalid)
        }
    }

    /** Необязательный тип может не иметь целей в черновике; исполнимость проверит запуск при включении. */
    @Test fun optionalCaptureMayHaveNoObjectivesInMapDraft() {
        validate(ScenarioVictorySettings(rules = arrayListOf(ScenarioVictoryRule("CaptureCity"))))
    }
}
