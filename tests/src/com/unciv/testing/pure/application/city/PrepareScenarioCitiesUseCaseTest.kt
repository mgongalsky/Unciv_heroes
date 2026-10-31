package com.unciv.testing.pure.application.city

import com.unciv.pure.application.city.PrepareScenarioCitiesUseCase
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет подготовку миссии без GameInfo, движка, DI и файлов. */
class PrepareScenarioCitiesUseCaseTest {
    private fun city(id: String, x: Float) = ScenarioCity(
        id = id, name = id, position = CityPosition(x, 0f),
        owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "slot")

    private fun input(cities: List<ScenarioCity>) = PrepareScenarioCitiesUseCase.Input(
        cities, listOf(ScenarioPlayerSlot("slot")),
        (0..3).map { CityPosition(it.toFloat(), 0f) }.toSet(), listOf("slot" to "Rome"))

    /** Тип игрока разрешается один раз, а нейтральный город сохраняет фиксированный тип. */
    @Test fun preparesOwnersTypesAndOneCapital() {
        val first = city("first", 0f)
        val preferred = city("preferred", 1f).apply { isCapital = true }
        val neutral = ScenarioCity(id = "neutral", name = "Fort", position = CityPosition(2f, 0f),
            typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle", isCapital = true)
        val result = PrepareScenarioCitiesUseCase.execute(input(listOf(first, preferred, neutral)))
        assertEquals(listOf("Rome", "Rome", null), result.map { it.ownerId })
        assertEquals(listOf("Rome", "Rome", "Castle"), result.map { it.resolvedType })
        assertEquals(listOf("preferred"), result.filter { it.description.isCapital }.map { it.description.id })
        assertFalse(result.last().description.isOriginalCapital)
        assertFalse(first.isCapital)
        assertTrue(neutral.isCapital)
    }

    /** Редактирование результата не меняет здания, гарнизон и территорию исходного сценария. */
    @Test fun resultOwnsItsMutableDescriptions() {
        val original = city("first", 0f).apply {
            buildings.add("Granary")
            garrison.add(ScenarioCityTroop("Guard", 5))
            territory.add(position)
        }
        val result = PrepareScenarioCitiesUseCase.execute(input(listOf(original))).single().description
        result.buildings.clear()
        result.garrison.single().amount = 1
        result.territory.clear()
        assertEquals(listOf("Granary"), original.buildings)
        assertEquals(5, original.garrison.single().amount)
        assertEquals(listOf(original.position), original.territory)
        assertFalse(original.isCapital)
    }

    /** Один участник не может занять два разных сценарных слота. */
    @Test(expected = IllegalArgumentException::class)
    fun rejectsParticipantAssignedToMultipleSlots() {
        PrepareScenarioCitiesUseCase.execute(input(emptyList()).copy(
            slots = listOf(ScenarioPlayerSlot("slot"), ScenarioPlayerSlot("other")),
            participants = listOf("slot" to "Rome", "other" to "Rome")))
    }

    /** Ошибочная территория отклоняется до подготовки результата и не меняет исходные флаги. */
    @Test fun invalidStructureDoesNotMutateInput() {
        val original = city("first", 0f).apply { territory.add(CityPosition(99f, 0f)) }
        try {
            PrepareScenarioCitiesUseCase.execute(input(listOf(original)))
            fail("Invalid territory must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertFalse(original.isCapital)
        assertEquals(listOf(CityPosition(99f, 0f)), original.territory)
    }

    /** Фиксированный тип города игрока не подменяется его нацией. */
    @Test fun ownedFixedTypeIsPreserved() {
        val fixed = city("first", 0f).apply {
            typeMode = ScenarioCityTypeMode.Fixed
            fixedType = "Castle"
        }
        assertEquals("Castle", PrepareScenarioCitiesUseCase.execute(input(listOf(fixed))).single().resolvedType)
    }
}
