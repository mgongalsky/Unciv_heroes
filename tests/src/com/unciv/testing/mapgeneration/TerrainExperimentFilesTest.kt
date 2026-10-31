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

    /** Новый диапазон сохраняется с отдельной версией и защищён checksum. */
    @Test
    fun latitudeReportRoundTripAndTampering() {
        val original = input()
        val source = original.copy(
            settings = original.settings.copy(
                latitudeRange = com.unciv.pure.domain.mapgeneration.LatitudeRange(60.0, 20.0)
            )
        )
        val result = TerrainGenerationPipeline.generate(source)
        val file = temporary.newFile("latitude.json")
        TerrainExperimentFiles.writeResult(file, result)
        assertEquals(result, TerrainExperimentFiles.readResult(file))
        assertEquals(source, TerrainExperimentFiles.readInput(file))
        val text = file.readText(Charsets.UTF_8)
        val changed = text.replace("\"top\":60.0", "\"top\":50.0")
        assertNotEquals(text, changed)
        file.writeText(changed, Charsets.UTF_8)
        try {
            TerrainExperimentFiles.readResult(file)
            fail("Modified latitude accepted")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("checksum"))
        }
    }

    /** Legacy-версия не записывает новых полей и не принимает замаскированный новый режим. */
    @Test
    fun legacyFormatOmitsLatitudeAndRejectsMixedVersions() {
        val file = temporary.newFile("legacy.json")
        TerrainExperimentFiles.writeInput(file, input())
        val text = file.readText(Charsets.UTF_8)
        assertFalse(text.contains("latitudeRange"))
        assertTrue(text.contains("\"algorithmVersion\":\"homogenous-terrain-1\""))
        assertNull(TerrainExperimentFiles.readInput(file).settings.latitudeRange)
        file.writeText(
            text.replace(
                "\"mode\":\"ALL\"",
                "\"mode\":\"ALL\",\"latitudeRange\":{\"top\":60.0,\"bottom\":20.0}"
            ), Charsets.UTF_8
        )
        try {
            TerrainExperimentFiles.readInput(file)
            fail("Mixed algorithm versions accepted")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("Legacy"))
        }
    }

    /** Фиксированная схема legacy проверяет канонические байты и checksum независимо от нового writer. */
    @Test
    fun frozenLegacyJsonPreservesCanonicalBytesAndReportChecksum() {
        val cells =
                """[{"column":0,"row":0,"x":0.0,"y":0.0,"latitude":0.0,"longitude":0.0,"neighbors":[],"baseTerrain":"Flat","features":[],"temperature":null,"humidity":null}]"""
        val legacy =
                """{"formatVersion":1,"algorithmVersion":"homogenous-terrain-1","settings":{"seed":0,"climateScale":6.0,"vegetationScale":3.0,"elevationScale":2.0,"elevationExponent":0.0,"temperatureExtremeness":0.0,"temperatureShift":0.0,"vegetationRichness":0.0,"flatEarth":false,"mode":"VEGETATION"},"rules":{"terrains":["Flat"],"waterTerrains":[],"impassableTerrains":[],"climateExcludedTerrains":[],"firstLandTerrain":"Flat","flatTerrain":"Flat","mountainTerrain":null,"hillTerrain":null,"risingMarker":"terrain-rising","loweringMarker":"terrain-lowering","climateRanges":[],"vegetation":[]},"cells":$cells}"""
        val inputFile = temporary.newFile("frozen-legacy-input.json")
            .apply { writeText(legacy, Charsets.UTF_8) }
        val input = TerrainExperimentFiles.readInput(inputFile)
        assertNull(input.settings.latitudeRange)
        val rewritten = temporary.newFile("rewritten-legacy.json")
        TerrainExperimentFiles.writeInput(rewritten, input)
        assertEquals(legacy, rewritten.readText(Charsets.UTF_8))
        val checksum = java.security.MessageDigest.getInstance("SHA-256")
            .digest(legacy.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val report = temporary.newFile("frozen-legacy-report.json")
        report.writeText(
            """{"formatVersion":1,"algorithmVersion":"homogenous-terrain-1","inputSha256":"$checksum","input":$legacy,"output":$cells}""",
            Charsets.UTF_8
        )
        val restored = TerrainExperimentFiles.readResult(report)
        assertEquals(input, restored.input)
        assertEquals(input.cells, restored.cells)
        assertEquals("homogenous-terrain-1", restored.algorithmVersion)
    }
}
