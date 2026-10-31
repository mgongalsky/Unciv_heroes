package com.unciv.testing.mapgeneration

import com.unciv.app.desktop.terrain.TerrainExperimentSource
import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.application.mapgeneration.TerrainGenerationPipeline
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Загрузка реальных JSON не требует Gdx-приложения и не изменяет RulesetCache. */
class TerrainExperimentSourceTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun baseTerrainRulesGenerateReplayableSnapshotWithoutChangingCache() {
        val relative = "jsons/Civ V - Gods & Kings/Terrains.json"
        val source = listOf(File(relative), File("android/assets", relative), File("../android/assets", relative))
            .firstOrNull { it.isFile } ?: error("Cannot locate base Terrains.json")
        val cached = HashMap(RulesetCache)
        val rules = TerrainExperimentSource.loadRules(source)
        assertTrue(rules.climateRanges.isNotEmpty())
        assertTrue(rules.vegetation.isNotEmpty())
        assertNotNull(rules.mountainTerrain)
        assertNotNull(rules.hillTerrain)
        val input = TerrainExperimentSource.create(rules, radius = 3)
        val result = TerrainGenerationPipeline.generate(input)
        val report = temporary.newFile("base-result.json")
        TerrainExperimentFiles.writeResult(report, result)
        assertEquals(result, TerrainExperimentFiles.readResult(report))
        assertEquals(cached.keys, RulesetCache.keys)
        for ((name, previous) in cached) assertSame(previous, RulesetCache[name])
    }

    @Test
    fun duplicateNamesAreRejectedBeforeBuildingSnapshot() {
        val file = temporary.newFile("duplicate.json")
        file.writeText("""[{"name":"Flat","type":"Land"},{"name":"Flat","type":"Land"}]""")
        try {
            TerrainExperimentSource.loadRules(file)
            fail("Duplicate terrain names accepted")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("Duplicate"))
        }
    }

    @Test
    fun rectangularSourceHasRequestedSizeAndRejectsUnknownShape() {
        val file = temporary.newFile("terrains.json")
        file.writeText("""[{"name":"Grassland","type":"Land"}]""")
        val rules = TerrainExperimentSource.loadRules(file)
        assertEquals(35, TerrainExperimentSource.create(rules, "rectangular", width = 7, height = 5).cells.size)
        try {
            TerrainExperimentSource.create(rules, "unknown")
            fail("Unknown shape accepted")
        } catch (failure: IllegalArgumentException) {
            assertTrue(failure.message.orEmpty().contains("shape"))
        }
    }
}
