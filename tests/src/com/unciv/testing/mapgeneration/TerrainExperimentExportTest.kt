package com.unciv.testing.mapgeneration

import com.unciv.app.desktop.terrain.TerrainExperimentExport
import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.imageio.ImageIO

/** Проверяет реальные файлы экспорта без запуска игрового приложения. */
class TerrainExperimentExportTest {
    @get:Rule val temporary = TemporaryFolder()

    private fun result(): TerrainExperimentResult {
        val terrain = "Flat,\"quoted\""
        val rules = TerrainRuleSnapshot(setOf(terrain), emptySet(), emptySet(), terrain, terrain,
            null, null, emptySet(), listOf(ClimateTerrainRange.inclusiveMinimums(terrain, -1f, 1f, 0f, 1f)), emptyList())
        return TerrainGenerationPipeline.generate(TerrainExperimentInput(
            TerrainGridFactory.hexagonal(2, terrain), rules, TerrainExperimentSettings(seed = 0L)))
    }

    @Test
    fun exportProducesReadableReproducibleReportImagesAndEscapedCsv() {
        val result = result()
        val first = File(temporary.root, "first")
        val second = File(temporary.root, "second")
        TerrainExperimentExport.write(first, result)
        TerrainExperimentExport.write(second, result)
        assertEquals(result, TerrainExperimentFiles.readResult(File(first, "result.json")))
        assertEquals(8, first.listFiles()!!.size)
        for (layer in TerrainExperimentExport.Layer.values()) {
            val filename = "${layer.name.lowercase()}.png"
            val image = ImageIO.read(File(first, filename))
            assertNotNull(image)
            assertEquals(1000, image.width)
            assertEquals(800, image.height)
            assertNotEquals(image.getRGB(0, 0), image.getRGB(500, 400))
        }
        for (file in first.listFiles()!!) {
            assertArrayEquals(file.name, file.readBytes(), File(second, file.name).readBytes())
        }
        assertTrue(File(first, "metrics.csv").readText().contains("\"Flat,\"\"quoted\"\"\""))
    }

    @Test
    fun existingExportIsNotOverwritten() {
        val directory = temporary.newFolder("existing")
        val marker = File(directory, "keep.txt").apply { writeText("keep") }
        try {
            TerrainExperimentExport.write(directory, result())
            fail("Existing directory accepted")
        } catch (expected: IllegalArgumentException) {
            assertEquals("keep", marker.readText())
            assertEquals(listOf("keep.txt"), directory.list()!!.toList())
        }
    }
}
