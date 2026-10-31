package com.unciv.testing.pure.application.mapgeneration

import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import org.junit.Assert.*
import org.junit.Test

/** Контракты экспериментального запуска: явная случайность, копирование и ранняя валидация. */
class TerrainGenerationPipelineTest {
    private fun input() = TerrainExperimentInput(
        listOf(
            TerrainSurfaceCell(0, 0, 0.0, 0.0, 0f, 0f, listOf(1), "Flat"),
            TerrainSurfaceCell(1, 0, 1.5, 0.5, 1f, 1f, listOf(0), "Flat")
        ),
        TerrainRuleSnapshot(setOf("Flat", "Forest", "Water"), setOf("Water"), emptySet(),
            "Flat", "Flat", null, null, emptySet(),
            listOf(ClimateTerrainRange.inclusiveMinimums("Flat", -1f, 1f, 0f, 1f)),
            listOf(VegetationRule("Forest", listOf("Flat")))),
        TerrainExperimentSettings(seed = 0L)
    )

    /** Нулевой seed не подменяется временем, а повторный запуск воспроизводит все слои. */
    @Test
    fun zeroSeedReproducesAllLayers() {
        val source = input()
        val first = TerrainGenerationPipeline.generate(source)
        assertEquals(0L, first.input.settings.seed)
        assertEquals(first, TerrainGenerationPipeline.generate(source))
        assertEquals(input(), source)
    }

    /** Исходные изменяемые коллекции не разделяются с сохранённым входом и выходом эксперимента. */
    @Test
    fun resultOwnsNestedSnapshots() {
        val features = mutableListOf<String>()
        val source = input().let { it.copy(cells = it.cells.map { cell -> cell.copy(features = features) }) }
        val result = TerrainGenerationPipeline.generate(source)
        features.add("Forest")
        assertTrue(result.input.cells.all { it.features.isEmpty() })
        assertEquals(result, TerrainGenerationPipeline.generate(result.input))
    }

    /** Частичная растительность сохраняет базовый слой и исходный климат, не вызывая другие этапы. */
    @Test
    fun partialVegetationPreservesOtherLayers() {
        val source = input().let {
            it.copy(settings = it.settings.copy(mode = TerrainGenerationMode.VEGETATION, vegetationRichness = 1f),
                cells = it.cells.map { cell -> cell.copy(temperature = 0.25, humidity = 0.75) })
        }
        val result = TerrainGenerationPipeline.generate(source)
        assertTrue(result.cells.all { it.baseTerrain == "Flat" && it.features == listOf("Forest") })
        assertTrue(result.cells.all { it.temperature == 0.25 && it.humidity == 0.75 })
    }

    /** Ошибочный масштаб любого слоя отклоняется до частичного результата, даже для другого режима. */
    @Test
    fun invalidScaleRejectsWholeRequest() {
        val source = input().let { it.copy(settings = it.settings.copy(elevationScale = Double.NaN)) }
        try {
            TerrainGenerationPipeline.generate(source)
            fail("Invalid scale accepted")
        } catch (_: IllegalArgumentException) { }
        assertTrue(source.cells.all { it.features.isEmpty() && it.temperature == null })
    }

    /** Некорректные ссылки соседства и асимметричные графы не допускаются в метрики и генерацию. */
    @Test
    fun invalidNeighborhoodIsRejected() {
        for (neighbors in listOf(listOf(7), listOf(0), listOf(1, 1), emptyList())) {
            val source = input().let { it.copy(cells = listOf(it.cells[0].copy(neighbors = neighbors), it.cells[1])) }
            try {
                TerrainGenerationPipeline.generate(source)
                fail("Invalid neighborhood accepted: $neighbors")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
