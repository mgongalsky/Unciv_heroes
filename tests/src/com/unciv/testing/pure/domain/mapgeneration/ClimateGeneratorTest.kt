package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.ClimateCell
import com.unciv.pure.domain.mapgeneration.ClimateGenerator
import com.unciv.pure.domain.mapgeneration.ClimateSettings
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import org.junit.Assert.*
import org.junit.Test

/** Границы климатических правил и владение снимками проверяются без движка и ruleset. */
class ClimateGeneratorTest {
    /** Только специальные минимумы включены; обычная нижняя граница остаётся открытой. */
    @Test
    fun rangePreservesFloatBoundariesAndInclusiveMinimums() {
        val full = ClimateTerrainRange.inclusiveMinimums("A", -1f, 1f, 0f, 1f)
        assertTrue(full.matches(-1.0, 0.0))
        assertTrue(full.matches(1.0, 1.0))
        val range = ClimateTerrainRange.inclusiveMinimums("A", -0.4f, 0.2f, 0.2f, 0.6f)
        assertFalse(range.matches((-0.4f).toDouble(), 0.4))
        assertFalse(range.matches(0.0, 0.2f.toDouble()))
        assertTrue(range.matches(0.2f.toDouble(), 0.6f.toDouble()))
        assertFalse(range.matches(Math.nextUp(0.2f.toDouble()), 0.4))
        assertFalse(range.matches(0.0, Math.nextUp(0.6f.toDouble())))
    }

    /** Перестановка пересекающихся правил меняет победителя; пробел возвращает fallback. */
    @Test
    fun selectionUsesRuleOrderAndFallback() {
        val a = ClimateTerrainRange.inclusiveMinimums("A", -1f, 1f, 0f, 1f)
        val b = a.copy(terrain = "B")
        assertEquals("A", ClimateGenerator.selectTerrain(0.0, 0.5, listOf(a, b), "F"))
        assertEquals("B", ClimateGenerator.selectTerrain(0.0, 0.5, listOf(b, a), "F"))
        assertEquals("F", ClimateGenerator.selectTerrain(2.0, 0.5, listOf(a), "F"))
    }

    /** Ошибочные масштабы отклоняются при создании настроек до запуска вычислений. */
    @Test
    fun invalidScalesAreRejected() {
        for (scale in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            try {
                ClimateSettings(scale = scale, maxLatitude = 3f, maxLongitude = 3f)
                fail("Accepted scale: $scale")
            } catch (_: IllegalArgumentException) { }
        }
    }

    /** Исключённые клетки и входные значения сохраняются, повторный запуск не накапливает результат. */
    @Test
    fun generationPreservesInputsAndExcludedCells() {
        val cells = listOf(
            ClimateCell(0.0, 0.0, 0f, 0f, "A"),
            ClimateCell(1.0, 1.0, 1f, 1f, "Water", true, 0.125, 0.75)
        )
        val before = cells.toList()
        val settings = ClimateSettings(maxLatitude = 3f, maxLongitude = 3f)
        val ranges = listOf(ClimateTerrainRange.inclusiveMinimums("B", -1f, 1f, 0f, 1f))
        fun generate() = ClimateGenerator.generate(cells, settings, ranges, setOf("A", "B", "Water"), "A", 0.0, 0.0)
        val result = generate()
        assertEquals(before, cells)
        assertEquals(result, generate())
        assertEquals("B", result[0].baseTerrain)
        assertEquals(0.5, result[0].humidity!!, 0.0)
        assertEquals("Water", result[1].baseTerrain)
        assertEquals(0.125, result[1].temperature!!, 0.0)
        assertFalse(result[1].processed)
    }

    /** Без таблицы отсутствующий исторический террейн не заменяет основание и не публикует климат. */
    @Test
    fun legacyBranchPreservesFieldsAndUnavailableTerrain() {
        val cell = ClimateCell(0.0, 0.0, 0f, 0f, "Custom", temperature = 0.125, humidity = 0.75)
        val result = ClimateGenerator.generate(listOf(cell), ClimateSettings(maxLatitude = 3f, maxLongitude = 3f),
            emptyList(), setOf("Custom"), "Custom", 0.0, 0.0).single()
        assertEquals("Custom", result.baseTerrain)
        assertEquals(cell.temperature, result.temperature)
        assertEquals(cell.humidity, result.humidity)
        assertTrue(result.processed)
    }

    /** Неизвестный идентификатор правила отклоняется без изменения входных клеток. */
    @Test
    fun unknownRuleTerrainIsRejectedWithoutMutation() {
        val cell = ClimateCell(0.0, 0.0, 0f, 0f, "A")
        val input = mutableListOf(cell)
        try {
            ClimateGenerator.generate(input, ClimateSettings(maxLatitude = 3f, maxLongitude = 3f),
                listOf(ClimateTerrainRange.inclusiveMinimums("Missing", -1f, 1f, 0f, 1f)), setOf("A"), "A", 0.0, 0.0)
            fail("Unknown terrain accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(listOf(cell), input)
    }
}
