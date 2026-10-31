package com.unciv.testing.pure.application.victory

import com.unciv.pure.application.victory.ProcessCityCapturedUseCase
import com.unciv.pure.domain.victory.CaptureCityObjective
import org.junit.Assert.*
import org.junit.Test

/** Проверяет границу события захвата независимо от игрового мира и тактического боя. */
class ProcessCityCapturedUseCaseTest {
    /** Последняя захваченная цель завершает набор; повтор после фиксации результата ничего не даёт. */
    @Test fun finalObjectiveWinsOnlyBeforeResultIsRecorded() {
        val objectives = listOf(CaptureCityObjective("a", arrayListOf("one")), CaptureCityObjective("b", arrayListOf("one")))
        val owners = mapOf("a" to "Rome", "b" to "Rome")
        assertTrue(ProcessCityCapturedUseCase.execute("b", "Rome", "one", objectives, owners, listOf("CaptureCity"), false))
        assertFalse(ProcessCityCapturedUseCase.execute("b", "Rome", "one", objectives, owners, listOf("CaptureCity"), true))
        assertEquals(mapOf("a" to "Rome", "b" to "Rome"), owners)
    }

    /** Выключенный тип, посторонний город и чужой слот не завершают даже уже принадлежащие цели. */
    @Test fun disabledUnrelatedAndIneligibleCaptureDoNotWin() {
        val objectives = listOf(CaptureCityObjective("a", arrayListOf("one")))
        val owners = mapOf("a" to "Rome")
        assertFalse(ProcessCityCapturedUseCase.execute("a", "Rome", "one", objectives, owners, emptyList(), false))
        assertFalse(ProcessCityCapturedUseCase.execute("other", "Rome", "one", objectives, owners, listOf("CaptureCity"), false))
        assertFalse(ProcessCityCapturedUseCase.execute("a", "Rome", "two", objectives, owners, listOf("CaptureCity"), false))
    }

    /** Захват одной цели не компенсирует потерю другой или ещё не завершённую смену владельца. */
    @Test fun incompleteOwnershipSnapshotDoesNotWin() {
        val objectives = listOf(CaptureCityObjective("a", arrayListOf("one")), CaptureCityObjective("b", arrayListOf("one")))
        assertFalse(ProcessCityCapturedUseCase.execute("a", "Rome", "one", objectives, mapOf("a" to "Rome", "b" to "Greece"), listOf("CaptureCity"), false))
        assertFalse(ProcessCityCapturedUseCase.execute("a", "Rome", "one", objectives, mapOf("a" to null, "b" to "Rome"), listOf("CaptureCity"), false))
    }
}
