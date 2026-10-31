package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.ElevationCell
import com.unciv.pure.domain.mapgeneration.ElevationGenerator
import com.unciv.pure.domain.mapgeneration.ElevationRules
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Правила рельефа на маленьких графах без движка и внешних данных. */
class ElevationGeneratorTest {
    private val rules = ElevationRules("Flat", "Mountain", "Hill", setOf("Mountain"), setOf("Water"), "up", "down")

    /** При отсутствии плоского террейна карта сохраняется и RNG не расходуется. */
    @Test
    fun missingFlatTerrainPreservesInputWithoutRandomDraw() {
        val random = Random(42)
        val cells = listOf(ElevationCell(0.0, 0.0, "Mountain", listOf("Forest"), false, emptyList()))
        val result = ElevationGenerator.generate(cells, rules.copy(flat = null), 0.7f, random)
        assertEquals("Mountain", result.single().baseTerrain)
        assertEquals(listOf("Forest"), result.single().features)
        assertEquals(Random(42).nextInt(), random.nextInt())
    }

    /** Водная клетка сохраняет основание и особенности даже при наличии соседних гор. */
    @Test
    fun waterPreservesItsLayers() {
        val cells = listOf(
            ElevationCell(0.0, 0.0, "Water", listOf("Ice"), true, listOf(1)),
            ElevationCell(1.0, 1.0, "Mountain", emptyList(), false, listOf(0))
        )
        val result = ElevationGenerator.generate(cells, rules, 0.7f, Random(42))
        assertEquals("Water", result[0].baseTerrain)
        assertEquals(listOf("Ice"), result[0].features)
    }

    /** Некорректное соседство отклоняется до обращения к RNG и до любых изменений списков. */
    @Test
    fun invalidNeighborDoesNotConsumeRandomness() {
        val random = Random(42)
        val features = mutableListOf("Forest")
        val cells = listOf(ElevationCell(0.0, 0.0, "Flat", features, false, listOf(1)))
        try {
            ElevationGenerator.generate(cells, rules, 0.7f, random)
            fail("Invalid neighbor accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(Random(42).nextInt(), random.nextInt())
        assertEquals(listOf("Forest"), features)
    }

    /** Неположительные и нечисловые масштабы запрещены до расхода RNG. */
    @Test
    fun invalidScalesAreRejected() {
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val random = Random(1)
            try {
                ElevationGenerator.generate(emptyList(), rules, 0.7f, random, scale)
                fail("Accepted scale: $scale")
            } catch (_: IllegalArgumentException) { }
            assertEquals(Random(1).nextInt(), random.nextInt())
        }
    }

    /** Вычисление не меняет входные особенности; результат не разделяет их изменяемое хранилище. */
    @Test
    fun resultsAreRepeatableAndDoNotShareInputFeatures() {
        val features = mutableListOf("Forest")
        val cells = listOf(ElevationCell(0.0, 0.0, "Flat", features, false, emptyList()))
        val result = ElevationGenerator.generate(cells, rules, 0.7f, Random(42))
        assertEquals(listOf("Forest"), features)
        assertEquals(result, ElevationGenerator.generate(cells, rules, 0.7f, Random(42)))
        features.add("Other")
        assertFalse("Other" in result.single().features)
    }
}
