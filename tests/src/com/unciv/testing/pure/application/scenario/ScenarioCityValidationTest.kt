package com.unciv.testing.pure.application.scenario

import com.unciv.pure.application.scenario.ValidateScenarioUseCase

import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Структурные инварианты карты проверяются на небольших данных в памяти. */
class ScenarioCityValidationTest {
    private val center = CityPosition(0f, 0f)
    private val other = CityPosition(1f, 0f)
    private val positions = setOf(center, other)
    private val slots = listOf(ScenarioPlayerSlot("slot"))
    private fun city() = ScenarioCity(id = "city", name = "Fort", position = center,
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle")

    /** Пустая территория означает центр; проверка не заполняет исходные списки. */
    @Test fun acceptsNeutralAndPlayerCitiesWithoutChangingInput() {
        val neutral = city()
        val player = city().apply {
            id = "player"; position = other; owner = ScenarioCityOwner.PlayerSlot
            ownerSlotId = "slot"; typeMode = ScenarioCityTypeMode.FromStartingPlayer; fixedType = ""
        }
        val before = listOf(neutral.deepCopy(), player.deepCopy())
        ValidateScenarioUseCase.execute(listOf(neutral, player), slots, positions)
        assertEquals(before, listOf(neutral, player))
    }

    /** Невалидные значения отклоняются независимо от игровых определений. */
    @Test fun rejectsInvalidCityFields() {
        val mutations: List<(ScenarioCity) -> Unit> = listOf(
            { it.id = "" }, { it.name = " " }, { it.population = 0 }, { it.foodStored = -1 },
            { it.position = CityPosition(9f, 9f) }, { it.ownerSlotId = "slot" },
            { it.typeMode = ScenarioCityTypeMode.FromStartingPlayer; it.fixedType = "" },
            { it.fixedType = " " }, { it.buildings.add("") },
            { it.buildings.addAll(listOf("Granary", "Granary")) },
            { it.garrison.add(ScenarioCityTroop("Guard", 0)) },
            { it.garrison.add(ScenarioCityTroop("", 1)) },
            { it.territory.add(other) },
            { it.territory.addAll(listOf(center, center)) },
            { it.territory.addAll(listOf(center, CityPosition(9f, 9f))) }
        )
        for ((index, mutate) in mutations.withIndex()) {
            val invalid = city().also(mutate)
            rejects("case $index") { ValidateScenarioUseCase.execute(listOf(invalid), slots, positions) }
        }
    }

    /** Два города не могут иметь одинаковый ID или претендовать на одну клетку. */
    @Test fun rejectsDuplicateIdsAndOverlappingTerritory() {
        rejects("ID") { ValidateScenarioUseCase.execute(listOf(city(), city().apply { position = other }), slots, positions) }
        rejects("center") { ValidateScenarioUseCase.execute(listOf(city(), city().apply { id = "second" }), slots, positions) }
        val first = city().apply { territory.addAll(listOf(center, other)) }
        val second = city().apply { id = "second"; position = other }
        rejects("territory") { ValidateScenarioUseCase.execute(listOf(first, second), slots, positions) }
    }

    /** Ошибочные слоты и ссылки на отсутствующего участника не принимаются. */
    @Test fun rejectsInvalidSlotsAndOwnerReferences() {
        rejects("empty slot") { ValidateScenarioUseCase.execute(emptyList(), listOf(ScenarioPlayerSlot("")), positions) }
        rejects("duplicate slot") { ValidateScenarioUseCase.execute(emptyList(), slots + slots, positions) }
        val unknown = city().apply { owner = ScenarioCityOwner.PlayerSlot; ownerSlotId = "missing" }
        rejects("unknown owner") { ValidateScenarioUseCase.execute(listOf(unknown), slots, positions) }
    }

    private fun rejects(label: String, action: () -> Unit) {
        try { action(); fail("Expected rejection: $label") }
        catch (_: IllegalArgumentException) { }
    }
}
