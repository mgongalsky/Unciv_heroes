package com.unciv.testing.mapgeneration

import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.pure.application.mapgeneration.TerrainGenerationPipeline
import com.unciv.testing.simulations.terrain.TerrainExperimentCli
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Сохраняет небольшую проверенную серию для последующей ручной приёмки; defaults не подбираются. */
class TerrainAcceptanceSeriesTest {
    @Test
    fun exportsIndependentScaleSeriesForManualAcceptance() {
        val projectRoot = File(System.getProperty("golden.projectRoot") ?: "../..").canonicalFile
        val terrains = File(projectRoot, "android/assets/jsons/Civ V - Gods & Kings/Terrains.json")
        assertTrue("Base terrain rules are missing", terrains.isFile)
        val parent = File(projectRoot, "tests/build/terrain-acceptance")
        check(parent.isDirectory || parent.mkdirs())
        val directory = Files.createTempDirectory(parent.toPath(), "series-").toFile()
        val baseline = File(directory, "baseline")
        TerrainExperimentCli.main(arrayOf(
            "--terrains=${terrains.absolutePath}", "--shape=hexagonal", "--radius=8",
            "--seeds=0,42", "--output=${baseline.absolutePath}"))
        val sourceFile = File(baseline, "input.json")
        val source = TerrainExperimentFiles.readInput(sourceFile)
        val variants = listOf(
            Triple("climate", "--climate-scales=12", listOf(12.0, 3.0, 2.0)),
            Triple("vegetation", "--vegetation-scales=6", listOf(6.0, 6.0, 2.0)),
            Triple("elevation", "--elevation-scales=4", listOf(6.0, 3.0, 4.0)))
        for ((name, option, _) in variants) {
            TerrainExperimentCli.main(arrayOf("--input=${sourceFile.absolutePath}", "--seeds=0,42",
                option, "--output=${File(directory, name).absolutePath}"))
        }
        val expectedScales = mapOf("baseline" to listOf(6.0, 3.0, 2.0)) +
            variants.associate { it.first to it.third }
        for ((name, scales) in expectedScales) {
            val batch = File(directory, name)
            assertEquals(3, File(batch, "index.csv").readLines().size)
            for ((index, seed) in listOf(0L, 42L).withIndex()) {
                val run = File(batch, "run-${index.toString().padStart(4, '0')}")
                val result = TerrainExperimentFiles.readResult(File(run, "result.json"))
                val settings = result.input.settings
                assertEquals(seed, settings.seed)
                assertEquals(scales, listOf(settings.climateScale, settings.vegetationScale, settings.elevationScale))
                assertEquals(source.cells, result.input.cells)
                assertEquals(source.rules, result.input.rules)
                assertEquals(result, TerrainGenerationPipeline.generate(result.input))
                assertEquals(5, run.listFiles()!!.count { it.extension == "png" })
                assertTrue(File(run, "metrics.csv").length() > 0)
                assertTrue(File(run, "legend.csv").length() > 0)
            }
        }
        // Указатель обновляется только после успешной проверки всех отчётов; прежние серии сохраняются.
        File(parent, "latest.txt").writeText(directory.absolutePath + "\n", Charsets.UTF_8)
        println("ACCEPTANCE SERIES: ${directory.absolutePath}")
        println("OPEN IN PLAYGROUND: ${File(baseline, "run-0001/result.json").absolutePath}")
    }
}
