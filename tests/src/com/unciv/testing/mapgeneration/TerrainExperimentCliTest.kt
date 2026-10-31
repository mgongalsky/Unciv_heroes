package com.unciv.testing.mapgeneration

import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import com.unciv.testing.simulations.terrain.TerrainExperimentCli
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Запускает настоящую CLI-точку входа с временными файлами без графического контекста. */
class TerrainExperimentCliTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun inputFile(): File {
        val rules = TerrainRuleSnapshot(setOf("Flat", "Forest"), emptySet(), emptySet(), "Flat", "Flat",
            null, null, emptySet(), listOf(ClimateTerrainRange.inclusiveMinimums("Flat", -1f, 1f, 0f, 1f)),
            listOf(VegetationRule("Forest", listOf("Flat"))))
        val input = TerrainExperimentInput(TerrainGridFactory.hexagonal(2, "Flat"), rules,
            TerrainExperimentSettings(seed = 0, vegetationRichness = 1f))
        return temporary.newFile("input.json").also { TerrainExperimentFiles.writeInput(it, input) }
    }

    @Test
    fun cartesianBatchPreservesInputsAndReplaysEachConfiguration() {
        val file = inputFile()
        val originalBytes = file.readBytes()
        val original = TerrainExperimentFiles.readInput(file)
        val output = File(temporary.root, "batch")
        TerrainExperimentCli.main(arrayOf("--input=$file", "--output=$output", "--seeds=0,-17",
            "--vegetation-scales=3,6", "--mode=VEGETATION"))
        val reports = output.listFiles()!!.filter { it.isDirectory }.sortedBy { it.name }
        assertEquals(4, reports.size)
        val restored = reports.map { TerrainExperimentFiles.readResult(File(it, "result.json")) }
        assertEquals(listOf(0L, 0L, -17L, -17L), restored.map { it.input.settings.seed })
        assertEquals(listOf(3.0, 6.0, 3.0, 6.0), restored.map { it.input.settings.vegetationScale })
        for (result in restored) {
            assertEquals(original.cells, result.input.cells)
            assertEquals(TerrainGenerationPipeline.generate(result.input), result)
            assertTrue(result.cells.all { it.features == listOf("Forest") })
        }
        assertEquals(5, File(output, "index.csv").readLines().size)
        assertArrayEquals(originalBytes, file.readBytes())
    }

    @Test
    fun invalidLaterCombinationCreatesNoOutput() {
        val input = inputFile()
        val output = File(temporary.root, "invalid")
        try {
            TerrainExperimentCli.main(arrayOf("--input=$input", "--output=$output", "--climate-scales=6,0"))
            fail("Invalid scale accepted")
        } catch (expected: IllegalArgumentException) {
            assertFalse(output.exists())
        }
    }

    @Test
    fun storedReportCanBeRepeatedWithoutChangingItsSeed() {
        val input = inputFile()
        val report = temporary.newFile("report.json")
        val result = TerrainGenerationPipeline.generate(TerrainExperimentFiles.readInput(input))
        TerrainExperimentFiles.writeResult(report, result)
        val output = File(temporary.root, "repeat")
        TerrainExperimentCli.main(arrayOf("--input=$report", "--output=$output"))
        assertEquals(result, TerrainExperimentFiles.readResult(File(output, "run-0000/result.json")))
    }

    /** Новый запуск из правил сохраняет геометрию и настройки, пригодные для последующего открытия в UI. */
    @Test
    fun terrainFileCreatesNewRectangularExperiment() {
        val terrains = temporary.newFile("terrains.json")
        terrains.writeText("""[{"name":"Grassland","type":"Land"}]""")
        val output = File(temporary.root, "from-rules")
        TerrainExperimentCli.main(
            arrayOf(
                "--terrains=$terrains", "--output=$output", "--shape=rectangular",
                "--width=7", "--height=5", "--seeds=0", "--elevation-scales=4"
            )
        )
        val result = TerrainExperimentFiles.readResult(File(output, "run-0000/result.json"))
        assertEquals(35, result.input.cells.size)
        assertEquals(0L, result.input.settings.seed)
        assertEquals(4.0, result.input.settings.elevationScale, 0.0)
        assertTrue(result.input.cells.all { it.baseTerrain == "Grassland" })
    }

    /** Диапазон сохраняется в отчёте, повторяется и явно отключается без изменения исходного файла. */
    @Test
    fun latitudeRangeCanBeReplayedAndDisabled() {
        val input = inputFile()
        val original = input.readBytes()
        val output = File(temporary.root, "latitude")
        TerrainExperimentCli.main(
            arrayOf(
                "--input=$input",
                "--output=$output",
                "--top-latitude=60",
                "--bottom-latitude=20"
            )
        )
        val report = File(output, "run-0000/result.json")
        val result = TerrainExperimentFiles.readResult(report)
        assertEquals(
            com.unciv.pure.domain.mapgeneration.LatitudeRange(60.0, 20.0),
            result.input.settings.latitudeRange
        )
        val replay = File(temporary.root, "latitude-repeat")
        TerrainExperimentCli.main(arrayOf("--input=$report", "--output=$replay"))
        assertEquals(
            result,
            TerrainExperimentFiles.readResult(File(replay, "run-0000/result.json"))
        )
        val legacy = File(temporary.root, "legacy-repeat")
        TerrainExperimentCli.main(
            arrayOf(
                "--input=$report",
                "--output=$legacy",
                "--latitude-mode=legacy"
            )
        )
        assertEquals(
            TerrainGenerationPipeline.generate(TerrainExperimentFiles.readInput(input)),
            TerrainExperimentFiles.readResult(File(legacy, "run-0000/result.json"))
        )
        assertArrayEquals(original, input.readBytes())
    }

    /** Неполные, перевёрнутые и конфликтующие широты отклоняются до записи. */
    @Test
    fun invalidLatitudeOptionsCreateNoOutput() {
        val input = inputFile()
        val variants = listOf(
            arrayOf("--top-latitude=60"),
            arrayOf("--top-latitude=20", "--bottom-latitude=60"),
            arrayOf("--top-latitude=NaN", "--bottom-latitude=20"),
            arrayOf("--latitude-mode=legacy", "--top-latitude=60", "--bottom-latitude=20"),
            arrayOf("--latitude-mode=range")
        )
        for ((index, options) in variants.withIndex()) {
            val output = File(temporary.root, "bad-latitude-$index")
            try {
                TerrainExperimentCli.main(arrayOf("--input=$input", "--output=$output") + options)
                fail("Invalid latitude options accepted")
            } catch (_: IllegalArgumentException) {
                assertFalse(output.exists())
            }
        }
    }

    /** Диапазон передаётся до валидации новой сетки, поэтому одна строка допустима. */
    @Test
    fun latitudeRangeAllowsNewSingleRowGrid() {
        val terrains = temporary.newFile("single-row-terrains.json")
        terrains.writeText("""[{"name":"Grassland","type":"Land"}]""")
        val output = File(temporary.root, "single-row")
        TerrainExperimentCli.main(
            arrayOf(
                "--terrains=$terrains", "--output=$output",
                "--shape=rectangular", "--width=7", "--height=1",
                "--top-latitude=60", "--bottom-latitude=20"
            )
        )
        val result = TerrainExperimentFiles.readResult(File(output, "run-0000/result.json"))
        assertEquals(7, result.cells.size)
        assertEquals(
            com.unciv.pure.domain.mapgeneration.LatitudeRange(60.0, 20.0),
            result.input.settings.latitudeRange
        )
        assertEquals(TerrainGenerationPipeline.LATITUDE_ALGORITHM_VERSION, result.algorithmVersion)
    }
}
