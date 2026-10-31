package com.unciv.testing.mapgeneration

import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Интеграция формата отчёта с файловой системой без Gdx-приложения и графического контекста. */
class TerrainExperimentFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun input() = TerrainExperimentInput(
        TerrainGridFactory.hexagonal(2, "Flat"),
        TerrainRuleSnapshot(setOf("Flat", "Forest", "Water"), setOf("Water"), emptySet(),
            "Flat", "Flat", null, null, emptySet(),
            listOf(ClimateTerrainRange.inclusiveMinimums("Flat", -1f, 1f, 0f, 1f)),
            listOf(VegetationRule("Forest", listOf("Flat")))),
        TerrainExperimentSettings(seed = 0L)
    )

    /** Полный снимок сохраняет нулевой seed, порядок правил, Float.ulp и явную геометрию. */
    @Test
    fun inputRoundTripPreservesExactValues() {
        val source = input()
        val file = temporary.newFile("input.json")
        TerrainExperimentFiles.writeInput(file, source)
        assertEquals(source, TerrainExperimentFiles.readInput(file))
    }

    /** Отчёт читается обратно и подтверждается повторным выполнением тех же вычислений. */
    @Test
    fun resultRoundTripReproducesFields() {
        val result = TerrainGenerationPipeline.generate(input())
        val file = temporary.newFile("result.json")
        TerrainExperimentFiles.writeResult(file, result)
        assertEquals(result, TerrainExperimentFiles.readResult(file))
        assertEquals(result.input, TerrainExperimentFiles.readInput(file))
    }

    /** Изменённый seed при прежней контрольной сумме отклоняется. */
    @Test
    fun modifiedInputChecksumIsRejected() {
        val file = temporary.newFile("result.json")
        TerrainExperimentFiles.writeResult(file, TerrainGenerationPipeline.generate(input()))
        val text = file.readText(Charsets.UTF_8)
        val changed = text.replace("\"seed\":0", "\"seed\":1")
        assertNotEquals(text, changed)
        file.writeText(changed, Charsets.UTF_8)
        try {
            TerrainExperimentFiles.readResult(file)
            fail("Modified input accepted")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("checksum"))
        }
    }

    /** Знак нуля сохраняется во всех вещественных полях; сравнение снимков остаётся точным. */
    @Test
    fun negativeZeroSurvivesInCoordinatesClimateAndSettings() {
        val source = input().let { original ->
            original.copy(
                cells = original.cells.map {
                    it.copy(
                        x = -0.0, y = -0.0, latitude = -0.0f,
                        longitude = -0.0f, temperature = -0.0, humidity = -0.0
                    )
                },
                settings = original.settings.copy(
                    mode = TerrainGenerationMode.VEGETATION,
                    temperatureShift = -0.0f, elevationExponent = -0.0f
                )
            )
        }
        val file = temporary.newFile("negative-zero.json")
        TerrainExperimentFiles.writeInput(file, source)
        val restored = TerrainExperimentFiles.readInput(file)
        assertEquals(source, restored)
        assertEquals((-0.0).toBits(), restored.cells.first().x.toBits())
        assertEquals((-0.0f).toBits(), restored.settings.temperatureShift.toBits())
    }
}
