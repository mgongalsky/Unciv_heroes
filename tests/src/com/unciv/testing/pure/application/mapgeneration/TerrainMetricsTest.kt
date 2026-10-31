package com.unciv.testing.pure.application.mapgeneration

import com.unciv.pure.application.mapgeneration.TerrainMetrics
import org.junit.Assert.*
import org.junit.Test

/** Метрики проверяются на графах с вручную определяемыми площадями и границами. */
class TerrainMetricsTest {
    /** Цепочка A-A-B содержит две области, одну границу и одну одиночную клетку. */
    @Test
    fun measuresKnownChain() {
        val result = TerrainMetrics.measure(listOf("A", "A", "B"), listOf(listOf(1), listOf(0, 2), listOf(1)), 1)
        assertEquals(mapOf("A" to 2, "B" to 1), result.counts)
        assertEquals(mapOf("A" to listOf(2), "B" to listOf(1)), result.componentAreas)
        assertEquals(1, result.singletonCells)
        assertEquals(1.0 / 3.0, result.smallComponentCellShare, 0.0)
        assertEquals(0.5, result.boundaryShare, 0.0)
    }

    /** Замыкающее ребро объединяет крайние A, но сохраняет две межтеррейновые границы. */
    @Test
    fun wrapEdgeJoinsComponentsAcrossBoundary() {
        val result = TerrainMetrics.measure(listOf("A", "B", "A"), listOf(listOf(1, 2), listOf(0, 2), listOf(0, 1)))
        assertEquals(listOf(2), result.componentAreas["A"])
        assertEquals(3, result.edges)
        assertEquals(2, result.boundaryEdges)
    }

    /** Исключённая вода разрывает область и удаляет прилегающие рёбра из знаменателя. */
    @Test
    fun excludedCellsDoNotConnectRegionsOrCountAsEdges() {
        val result = TerrainMetrics.measure(listOf("A", null, "A"), listOf(listOf(1), listOf(0, 2), listOf(1)))
        assertEquals(2, result.includedCells)
        assertEquals(listOf(1, 1), result.componentAreas["A"])
        assertEquals(0, result.edges)
        assertEquals(0.0, result.boundaryShare, 0.0)
    }

    /** Полностью исключённая карта даёт нулевые показатели без NaN и деления на ноль. */
    @Test
    fun emptyMaskHasFiniteZeroMetrics() {
        val result = TerrainMetrics.measure(listOf(null), listOf(emptyList()))
        assertEquals(0, result.includedCells)
        assertTrue(result.shares.isEmpty())
        assertEquals(0.0, result.smallComponentCellShare, 0.0)
    }
}
