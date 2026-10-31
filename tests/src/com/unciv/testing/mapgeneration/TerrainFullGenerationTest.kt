package com.unciv.testing.mapgeneration

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.unciv.UncivGame
import com.unciv.logic.map.*
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSettings
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Интеграция полного генератора с оставшимися этапами; это не исторический эталон всего generateMap. */
class TerrainFullGenerationTest {
    @Test
    fun completeGeneratorRepeatsWithSameRulesAndSeedAcrossShapes() = withRules { rules ->
        for ((shape, wrap) in listOf(MapShape.hexagonal to false, MapShape.flatEarth to false,
            MapShape.rectangular to false, MapShape.rectangular to true)) {
            val parameters = parameters(shape, wrap)
            val first = MapGenerator(rules).generateMap(parameters.clone())
            val second = MapGenerator(rules).generateMap(parameters.clone())
            assertEquals("Full generation differs for $shape wrap=$wrap", snapshot(first), snapshot(second))
            assertEquals(parameters.getArea(), first.values.size)
            assertTrue(first.values.any { it.isLand })
            assertTrue(first.values.any { it.isWater })
            assertTrue(first.continentSizes.isNotEmpty())
            assertTrue(first.values.all { it.baseTerrain in rules.terrains })
            assertTrue(first.values.all { tile -> tile.terrainFeatures.all { it in rules.terrains } })
            assertTrue(first.values.all { it.resource == null || it.resource in rules.tileResources })
            assertTrue(first.values.all { it.naturalWonder == null || it.naturalWonder in rules.terrains })
            assertTrue(first.values.all { it.temperature == null || it.temperature!!.isFinite() })
            assertTrue(first.values.all { it.humidity == null || it.humidity!!.isFinite() })
        }
    }

    @Test
    fun oldParameterJsonMatchesExplicitHistoricalScalesInFullGenerator() = withRules { rules ->
        val old = Json().fromJson(MapParameters::class.java,
            """{"seed":42,"shape":"Hexagonal","type":"Pangaea","mapSize":{"radius":10,"width":23,"height":15,"name":"Tiny"},"noRuins":true}""")
        val explicit = old.clone().apply {
            tilesPerBiomeArea = 6
            vegetationScale = 3.0
            elevationScale = 2.0
        }
        assertEquals(snapshot(MapGenerator(rules).generateMap(explicit)), snapshot(MapGenerator(rules).generateMap(old)))
    }

    private fun parameters(shape: String, wrap: Boolean) = MapParameters().apply {
        seed = 42L
        this.shape = shape
        worldWrap = wrap
        mapSize = if (shape == MapShape.rectangular) MapSizeNew(32, 20) else MapSizeNew(MapSize.Tiny)
        noRuins = true // Поселения этого форка имеют отдельные игровые зависимости.
    }

    private fun snapshot(map: TileMap) = map.values.map {
        listOf(it.position.x, it.position.y, it.baseTerrain, it.terrainFeatures.toList(), it.temperature, it.humidity,
            it.resource, it.resourceAmount, it.naturalWonder, it.improvement, it.getContinent(),
            it.hasBottomRiver, it.hasBottomLeftRiver, it.hasBottomRightRiver)
    }

    /** Восстанавливает глобальные ссылки даже при сбое загрузки; пользовательские настройки не сохраняются. */
    private fun withRules(test: (Ruleset) -> Unit) {
        val cached = HashMap(RulesetCache)
        val currentField = UncivGame::class.java.getDeclaredField("Current").apply { isAccessible = true }
        val previous = currentField.get(null)
        try {
            UncivGame.Current = UncivGame().apply { settings = GameSettings() }
            val assets = listOf(File("."), File("android/assets"), File("../android/assets"))
                .firstOrNull { File(it, "jsons/Civ V - Gods & Kings/Terrains.json").isFile }
                ?: error("Cannot locate game rules")
            RulesetCache.clear()
            for (base in BaseRuleset.values()) {
                RulesetCache[base.fullName] = Ruleset().apply {
                    load(FileHandle(File(assets, "jsons/${base.fullName}")))
                    name = base.fullName
                }
            }
            test(RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName))
        } finally {
            RulesetCache.clear()
            RulesetCache.putAll(cached)
            currentField.set(null, previous)
        }
    }
}
