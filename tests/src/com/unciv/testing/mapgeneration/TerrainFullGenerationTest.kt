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

    /** Лёд следует мировому диапазону, а не холодным краям локальной геометрии. */
    @Test
    fun iceUsesSelectedWorldLatitude() = withRules { rules ->
        fun waterMap(latitude: Double): TileMap {
            val map = TileMap(12, 8, rules, false)
            map.mapParameters = MapParameters().apply {
                seed = 42L
                shape = MapShape.rectangular
                mapSize = MapSizeNew(12, 8)
                useLatitudeRange = true
                topLatitude = latitude
                bottomLatitude = latitude
            }
            for (tile in map.values) {
                tile.baseTerrain = "Ocean"
                tile.setTerrainFeatures(emptyList())
                tile.setTerrainTransients()
            }
            return map
        }

        val equator = waterMap(0.0)
        val polar = waterMap(90.0)
        MapGenerator(rules).generateSingleStep(
            equator,
            com.unciv.ui.mapeditor.MapGeneratorSteps.Ice
        )
        MapGenerator(rules).generateSingleStep(polar, com.unciv.ui.mapeditor.MapGeneratorSteps.Ice)
        assertTrue(
            "Equatorial water unexpectedly froze",
            equator.values.all { "Ice" !in it.terrainFeatures })
        assertTrue("Polar water has no ice", polar.values.any { "Ice" in it.terrainFeatures })
        assertTrue(equator.values.all { it.baseTerrain == "Ocean" })
        assertTrue(polar.values.all { it.baseTerrain == "Ocean" })
    }

    /** Ошибка диапазона обнаруживается до reseed и любых изменений клеток полного или частичного запуска. */
    @Test
    fun invalidLatitudeRejectsGenerationBeforeMutation() = withRules { rules ->
        val invalid = parameters(MapShape.rectangular, false).apply {
            seed = 0L
            useLatitudeRange = true
            topLatitude = 10.0
            bottomLatitude = 60.0
        }
        try {
            MapGenerator(rules).generateMap(invalid)
            fail("Invalid range accepted by full generation")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(0L, invalid.seed)
        assertEquals("", invalid.createdWithVersion)
        val map = MapGenerator(rules).generateMap(parameters(MapShape.rectangular, false))
        map.mapParameters = invalid.clone()
        val before = snapshot(map)
        for (step in listOf(
            com.unciv.ui.mapeditor.MapGeneratorSteps.HumidityAndTemperature,
            com.unciv.ui.mapeditor.MapGeneratorSteps.Ice,
            com.unciv.ui.mapeditor.MapGeneratorSteps.NaturalWonders
        )) {
            try {
                MapGenerator(rules).generateSingleStep(map, step)
                fail("Invalid range accepted by $step")
            } catch (_: IllegalArgumentException) {
            }
            assertEquals(before, snapshot(map))
            assertEquals(0L, map.mapParameters.seed)
        }
    }

    /** Полный запуск воспроизводит климат; сохранение удерживает параметры и постоянные данные клеток. */
    @Test
    fun latitudeRangeRepeatsAndSurvivesMapSave() = withRules { rules ->
        for ((shape, wrap) in listOf(
            MapShape.hexagonal to false,
            MapShape.rectangular to false, MapShape.rectangular to true
        )) {
            val requested = parameters(shape, wrap).apply {
                useLatitudeRange = true
                topLatitude = 60.0
                bottomLatitude = 20.0
            }
            val first = MapGenerator(rules).generateMap(requested.clone())
            val second = MapGenerator(rules).generateMap(requested.clone())
            assertEquals(snapshot(first), snapshot(second))
            val saved = com.unciv.logic.MapSaver.mapToSavedString(first)
            val restored = com.unciv.logic.MapSaver.mapFromSavedString(saved)
            restored.setTransients(rules, false)
            assertEquals(
                requested.validatedLatitudeRange(),
                restored.mapParameters.validatedLatitudeRange()
            )
            // temperature и humidity намеренно @Transient: они не являются частью формата карты.
            fun persistedCells(map: TileMap) = snapshot(map).map { values ->
                values.filterIndexed { index, _ -> index != 4 && index != 5 }
            }
            assertEquals(persistedCells(first), persistedCells(restored))
            assertTrue(restored.values.all { it.temperature == null && it.humidity == null })
            val preview = com.unciv.logic.MapSaver.mapParametersFromSavedString(saved)
            assertEquals(requested.validatedLatitudeRange(), preview.validatedLatitudeRange())
        }
    }

    /** Одиночная особенность переживает JSON, а изменяемый входной список не разделяется с клеткой. */
    @Test
    fun terrainFeatureListsAreOwnedAndSerializable() = withRules { rules ->
        val map = TileMap(3, rules, false)
        val tile = map.values.first()
        tile.baseTerrain = "Grassland"
        tile.setTerrainTransients()
        val source = mutableListOf("Forest")
        tile.setTerrainFeatures(source)
        source.clear()
        assertEquals(listOf("Forest"), tile.terrainFeatures)
        for (features in listOf(emptyList(), listOf("Forest"), listOf("Hill", "Forest"))) {
            tile.setTerrainFeatures(features)
            val json = Json()
            val restored = json.fromJson(TileInfo::class.java, json.toJson(tile))
            assertEquals(features, restored.terrainFeatures)
        }
    }

    /** Игровая проекция совпадает с доменной и трактует проценты чудес относительно полюса. */
    @Test
    fun latitudeAdapterUsesWorldCoordinatesAndPolarPercentages() = withRules { rules ->
        val adapterClass = Class.forName("com.unciv.logic.map.mapgenerator.MapLatitudeAdapter")
        val constructor = adapterClass.getDeclaredConstructor(TileMap::class.java)
            .apply { isAccessible = true }
        val latitude = adapterClass.getDeclaredMethod("latitude", TileInfo::class.java)
            .apply { isAccessible = true }
        val matches = adapterClass.getDeclaredMethod(
            "matchesPercentRange", TileInfo::class.java,
            Int::class.javaPrimitiveType, Int::class.javaPrimitiveType
        ).apply { isAccessible = true }
        for (shape in listOf(MapShape.hexagonal, MapShape.rectangular)) {
            val map = if (shape == MapShape.hexagonal) TileMap(4, rules, false) else TileMap(
                7,
                5,
                rules,
                false
            )
            map.mapParameters = MapParameters().apply {
                this.shape = shape
                useLatitudeRange = true
                topLatitude = 60.0
                bottomLatitude = 20.0
            }
            val positions = map.values.map { it.position.cpy() }
            val y =
                    map.values.map { com.unciv.logic.HexMath.hex2WorldCoords(it.position).y.toDouble() }
            val range = com.unciv.pure.domain.mapgeneration.LatitudeRange(60.0, 20.0)
            val adapter = constructor.newInstance(map)
            for ((index, tile) in map.values.withIndex()) {
                val expected = range.at(y[index], y.minOrNull()!!, y.maxOrNull()!!).toFloat()
                assertEquals(expected, latitude.invoke(adapter, tile) as Float, 0f)
                // 30% от 90° = 27°; это не 30% от высоты выбранного участка карты.
                assertEquals(expected <= 27f, matches.invoke(adapter, tile, 0, 30))
            }
            assertEquals(positions, map.values.map { it.position })
            map.mapParameters.useLatitudeRange = false
            val legacy = constructor.newInstance(map)
            for (tile in map.values) {
                assertEquals(tile.latitude, latitude.invoke(legacy, tile) as Float, 0f)
                assertEquals(
                    kotlin.math.abs(tile.latitude) in 0f..(map.maxLatitude * 30 * 0.01f),
                    matches.invoke(legacy, tile, 0, 30)
                )
            }
        }
    }

    /** Полный игровой путь сохраняет воду; повышение уровня увеличивает водную область при том же seed. */
    @Test
    fun fullGenerationKeepsWaterAndHonoursWaterLevel() = withRules { rules ->
        fun generate(type: String, level: Float = 0f): TileMap = MapGenerator(rules).generateMap(
            parameters(MapShape.rectangular, false).apply {
                this.type = type
                waterThreshold = level
                noNaturalWonders = true
            }
        )
        for (type in listOf(MapType.pangaea, MapType.archipelago, MapType.innerSea)) {
            val map = generate(type)
            assertTrue("No water after full generation: $type", map.values.any { it.isWater })
            assertTrue("No land after full generation: $type", map.values.any { it.isLand })
        }
        val lower = generate(MapType.default, -0.1f)
        val higher = generate(MapType.default, 0.1f)
        val lowerWater = lower.values.filter { it.isWater }.map { it.position.cpy() }.toSet()
        val higherWater = higher.values.filter { it.isWater }.map { it.position.cpy() }.toSet()
        assertTrue(
            "Increasing Water level removed existing water",
            higherWater.containsAll(lowerWater)
        )
        assertTrue("Water level did not increase water area", higherWater.size > lowerWater.size)
    }
}
