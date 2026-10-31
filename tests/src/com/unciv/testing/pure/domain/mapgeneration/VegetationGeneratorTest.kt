package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.VegetationCell
import com.unciv.pure.domain.mapgeneration.VegetationGenerator
import com.unciv.pure.domain.mapgeneration.VegetationRule
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

/** Правила допустимости растительности и расход случайности без игрового состояния. */
class VegetationGeneratorTest {
    /** Проверяются одновременно основание и верхний слой, включая уже существующий лес. */
    @Test
    fun bothBaseAndLastTerrainMustBeEligible() {
        val cells = listOf(
            VegetationCell(0.0, 0.0, "Desert", "Hill"),
            VegetationCell(0.0, 0.0, "Grassland", "Forest"),
            VegetationCell(0.0, 0.0, "Grassland", "Hill")
        )
        val rules = listOf(VegetationRule("Forest", listOf("Grassland", "Hill")))
        assertEquals(listOf(null, null, "Forest"), VegetationGenerator.generate(cells, rules, 1f, Random(42)))
    }

    /** Порядок кандидатов и один bounded-вызов на выбранную клетку совпадают с историческим выбором. */
    @Test
    fun candidateOrderAndRandomConsumptionArePreserved() {
        val cells = List(12) { VegetationCell(it.toDouble(), 0.0, "Grassland", "Grassland") }
        val rules = listOf(VegetationRule("Jungle", listOf("Grassland")), VegetationRule("Forest", listOf("Grassland")))
        val expectedRandom = Random(17)
        expectedRandom.nextInt()
        val expected = cells.map { rules.random(expectedRandom).terrain }
        val actualRandom = Random(17)
        assertEquals(expected, VegetationGenerator.generate(cells, rules, 1f, actualRandom))
        assertEquals(expectedRandom.nextInt(), actualRandom.nextInt())
    }

    /** Даже пустой этап потребляет один seed, но больше не обращается к случайности. */
    @Test
    fun emptyRulesStillConsumeSeed() {
        val expected = Random(0)
        expected.nextInt()
        val actual = Random(0)
        assertEquals(listOf(null), VegetationGenerator.generate(
            listOf(VegetationCell(0.0, 0.0, "Grassland", "Grassland")), emptyList(), 1f, actual))
        assertEquals(expected.nextInt(), actual.nextInt())
    }

    /** Невалидный масштаб отклоняется до расхода RNG и не изменяет входной список. */
    @Test
    fun invalidScaleDoesNotConsumeRandomnessOrMutateInput() {
        val cells = mutableListOf(VegetationCell(0.0, 0.0, "Grassland", "Grassland"))
        val before = cells.toList()
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val random = Random(42)
            try {
                VegetationGenerator.generate(cells, emptyList(), 1f, random, scale)
                fail("Accepted scale: $scale")
            } catch (_: IllegalArgumentException) { }
            assertEquals(Random(42).nextInt(), random.nextInt())
            assertEquals(before, cells)
        }
    }

    /** Возврат к исходному масштабу и seed восстанавливает результат без накопления особенностей. */
    @Test
    fun repeatedSnapshotIsIndependentOfOtherRuns() {
        val cells = List(50) { VegetationCell(it * 0.5, it * 0.25, "Grassland", "Grassland") }
        val rules = listOf(VegetationRule("Forest", listOf("Grassland")))
        val original = VegetationGenerator.generate(cells, rules, 0.4f, Random(0), 3.0)
        VegetationGenerator.generate(cells, rules, 0.4f, Random(0), 12.0)
        assertEquals(original, VegetationGenerator.generate(cells, rules, 0.4f, Random(0), 3.0))
    }
}
