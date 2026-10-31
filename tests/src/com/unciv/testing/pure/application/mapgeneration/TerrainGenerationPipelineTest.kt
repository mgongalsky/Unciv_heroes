package com.unciv.testing.pure.application.mapgeneration

import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.LatitudeRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import org.junit.Assert.*
import org.junit.Test

/** Контракты эксперимента: явная случайность, копирование и ранняя валидация. */
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

    /** Нулевой seed не подменяется временем; повтор воспроизводит все слои. */
    @Test
    fun zeroSeedReproducesAllLayers() {
        val source = input()
        val first = TerrainGenerationPipeline.generate(source)
        assertEquals(0L, first.input.settings.seed)
        assertEquals(first, TerrainGenerationPipeline.generate(source))
        assertEquals(input(), source)
    }

    /** Изменяемые коллекции источника не разделяются с сохранённым результатом. */
    @Test
    fun resultOwnsNestedSnapshots() {
        val features = mutableListOf<String>()
        val source = input().let { it.copy(cells = it.cells.map { cell -> cell.copy(features = features) }) }
        val result = TerrainGenerationPipeline.generate(source)
        features.add("Forest")
        assertTrue(result.input.cells.all { it.features.isEmpty() })
        assertEquals(result, TerrainGenerationPipeline.generate(result.input))
    }

    /** Частичная растительность сохраняет основания и климат. */
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

    /** Ошибочный масштаб отклоняется до частичного результата. */
    @Test
    fun invalidScaleRejectsWholeRequest() {
        val source = input().let { it.copy(settings = it.settings.copy(elevationScale = Double.NaN)) }
        try {
            TerrainGenerationPipeline.generate(source)
            fail("Invalid scale accepted")
        } catch (_: IllegalArgumentException) { }
        assertTrue(source.cells.all { it.features.isEmpty() && it.temperature == null })
    }

    /** Некорректные ссылки и асимметричные графы отклоняются. */
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

    /** Широты меняют температурный фон, сохраняя влажность, исходные клетки и повторяемость. */
    @Test
    fun latitudeRangeChangesOnlyClimateBackground() {
        val source = input().let { it.copy(settings = it.settings.copy(mode = TerrainGenerationMode.CLIMATE)) }
        fun run(top: Double, bottom: Double) = TerrainGenerationPipeline.generate(source.copy(
            settings = source.settings.copy(latitudeRange = LatitudeRange(top, bottom))))
        val equator = run(0.0, 0.0)
        val polar = run(90.0, 90.0)
        assertEquals(equator.cells.map { it.humidity }, polar.cells.map { it.humidity })
        assertTrue(equator.cells.zip(polar.cells).all { (warm, cold) -> warm.temperature!! > cold.temperature!! })
        assertEquals(source.cells, equator.input.cells)
        assertEquals(equator, TerrainGenerationPipeline.generate(equator.input))
        assertEquals(TerrainGenerationPipeline.LATITUDE_ALGORITHM_VERSION, equator.algorithmVersion)
        val north = run(60.0, 20.0)
        assertTrue(north.cells[1].temperature!! < north.cells[0].temperature!!)
        assertEquals(TerrainGenerationPipeline.ALGORITHM_VERSION, TerrainGenerationPipeline.generate(source).algorithmVersion)
    }

    /** Одна строка допустима с диапазоном; Flat Earth не смешивается с географической моделью. */
    @Test
    fun latitudeRangeSupportsSingleRowAndRejectsFlatEarth() {
        val original = input()
        val source = original.copy(
            cells = TerrainGridFactory.rectangular(7, 1, "Flat"),
            settings = original.settings.copy(
                mode = TerrainGenerationMode.CLIMATE,
                latitudeRange = LatitudeRange(60.0, 20.0)
            )
        )
        val result = TerrainGenerationPipeline.generate(source)
        assertTrue(result.cells.all { it.temperature?.isFinite() == true })
        assertEquals(source.cells, result.input.cells)
        try {
            TerrainGenerationPipeline.generate(source.copy(settings = source.settings.copy(flatEarth = true)))
            fail("Mixed latitude models accepted")
        } catch (_: IllegalArgumentException) {
        }
    }
}
