package com.unciv.app.desktop.terrain

import com.badlogic.gdx.utils.Json
import com.unciv.logic.map.mapgenerator.TerrainRuleSnapshotAdapter
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.pure.application.mapgeneration.*
import java.io.File

/** Загружает один полный список террейнов; не изменяет глобальный кеш и не объединяет моды неявно. */
object TerrainExperimentSource {
    fun loadRules(file: File): TerrainRuleSnapshot {
        require(file.isFile) { "Terrain rules file not found: $file" }
        val json = Json().apply { setIgnoreUnknownFields(true) }
        val terrains = json.fromJson(Array<Terrain>::class.java, file.readText(Charsets.UTF_8))
        require(terrains.isNotEmpty()) { "Terrain rules file is empty" }
        require(terrains.all { it.name.isNotBlank() }) { "Terrain name must not be blank" }
        require(terrains.map { it.name }.distinct().size == terrains.size) { "Duplicate terrain name" }
        val ruleset = Ruleset()
        for (terrain in terrains) ruleset.terrains[terrain.name] = terrain
        require(terrains.any { it.type == com.unciv.models.ruleset.tile.TerrainType.Land }) {
            "Terrain rules require at least one land terrain"
        }
        return TerrainRuleSnapshotAdapter.snapshot(ruleset)
    }

    /** Новая маска состоит из суши; исходные водные маски загружаются через полный снимок эксперимента. */
    fun create(
        rules: TerrainRuleSnapshot,
        shape: String = "hexagonal",
        radius: Int = 12,
        width: Int = 30,
        height: Int = 20,
        settings: TerrainExperimentSettings = TerrainExperimentSettings()
    ): TerrainExperimentInput {
        val terrain = rules.flatTerrain ?: rules.firstLandTerrain
        val cells = when (shape) {
            "hexagonal" -> TerrainGridFactory.hexagonal(radius, terrain)
            "rectangular" -> TerrainGridFactory.rectangular(width, height, terrain)
            else -> throw IllegalArgumentException("Unknown experiment shape: $shape")
        }
        return TerrainExperimentInput(cells, rules, settings).also { TerrainGenerationPipeline.validate(it) }
    }
}
