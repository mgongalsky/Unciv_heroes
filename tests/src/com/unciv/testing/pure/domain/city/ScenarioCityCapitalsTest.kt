package com.unciv.testing.pure.domain.city

import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет выбор единственной стартовой столицы для каждого слота без изменения авторской карты. */
class ScenarioCityCapitalsTest {
    /** Минимальное описание города игрока; координаты и правила здесь не участвуют в выборе столицы. */
    private fun city(id: String, slot: String = "first") = ScenarioCity(
        id = id, name = id, owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = slot)

    /** Без отметок выбирается первый город каждого владельца, а флаги выставляются только на копиях. */
    @Test
    fun missingFlagsChooseFirstCityForEachOwnerWithoutMutatingMap() {
        val input = listOf(city("a"), city("b"), city("c", "second"))
        val result = ScenarioCityCapitals.resolve(input)
        assertEquals(listOf("a", "c"), result.filter { it.isCapital }.map { it.id })
        assertEquals(listOf("a", "c"), result.filter { it.isOriginalCapital }.map { it.id })
        assertTrue(input.none { it.isCapital || it.isOriginalCapital })
        assertNotSame(input.first(), result.first())
    }

    /** Новая отметка столицы приоритетнее старого флага; из нескольких отметок выбирается первая. */
    @Test
    fun preferenceWinsOverLegacyFlagAndDuplicatePreferencesAreResolved() {
        val input = listOf(city("a").apply { isOriginalCapital = true },
            city("b").apply { isCapital = true }, city("c").apply { isCapital = true })
        val result = ScenarioCityCapitals.resolve(input)
        assertEquals("b", result.single { it.isCapital }.id)
        assertEquals("b", result.single { it.isOriginalCapital }.id)
        assertTrue(input.last().isCapital)
    }

    /** При отсутствии новой отметки учитывается старая исходная столица; нейтральные города исключаются. */
    @Test
    fun legacyOriginalCapitalIsUsedAndNeutralFlagsAreCleared() {
        val neutral = ScenarioCity(id = "neutral", isCapital = true, isOriginalCapital = true,
            typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Default")
        val result = ScenarioCityCapitals.resolve(listOf(city("a"),
            city("b").apply { isOriginalCapital = true }, neutral))
        assertEquals("b", result.single { it.isCapital }.id)
        assertEquals("b", result.single { it.isOriginalCapital }.id)
        assertFalse(result.last().isCapital)
        assertTrue(neutral.isCapital)
    }
}
