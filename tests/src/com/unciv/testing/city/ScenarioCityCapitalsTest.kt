package com.unciv.testing.city

import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

class ScenarioCityCapitalsTest {
    private fun city(id: String, slot: String = "first") = ScenarioCity(
        id = id, name = id, owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = slot)

    @Test
    fun missingFlagsChooseFirstCityForEachOwnerWithoutMutatingMap() {
        val input = listOf(city("a"), city("b"), city("c", "second"))
        val result = ScenarioCityCapitals.resolve(input)
        assertEquals(listOf("a", "c"), result.filter { it.isCapital }.map { it.id })
        assertEquals(listOf("a", "c"), result.filter { it.isOriginalCapital }.map { it.id })
        assertTrue(input.none { it.isCapital || it.isOriginalCapital })
        assertNotSame(input.first(), result.first())
    }

    @Test
    fun preferenceWinsOverLegacyFlagAndDuplicatePreferencesAreResolved() {
        val input = listOf(city("a").apply { isOriginalCapital = true },
            city("b").apply { isCapital = true }, city("c").apply { isCapital = true })
        val result = ScenarioCityCapitals.resolve(input)
        assertEquals("b", result.single { it.isCapital }.id)
        assertEquals("b", result.single { it.isOriginalCapital }.id)
        assertTrue(input.last().isCapital)
    }

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
