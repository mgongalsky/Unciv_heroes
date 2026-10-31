package com.unciv.testing.mapgeneration

import com.unciv.Constants
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSizeNew
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerationRandomness
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.ui.mapeditor.MapGeneratorSteps
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

/** Характеристика настоящего генератора на правилах в памяти; это интеграционные, не pure-тесты. */
class TerrainGenerationCharacterizationTest {
    /** Вода и горы исключены из климатического этапа вместе с их прежними климатическими полями. */
    @Test
    fun climatePreservesWaterAndMountains() {
        val rules = rules()
        val map = map(rules, 42L)
        val excluded = map.values.take(2)
        excluded[0].baseTerrain = Constants.ocean
        excluded[1].baseTerrain = Constants.mountain
        excluded.forEach {
            it.temperature = 0.123
            it.humidity = 0.456
            it.setTerrainTransients()
        }
        MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.HumidityAndTemperature)
        assertEquals(listOf(Constants.ocean, Constants.mountain), excluded.map { it.baseTerrain })
        excluded.forEach {
            assertEquals(0.123, it.temperature!!, 0.0)
            assertEquals(0.456, it.humidity!!, 0.0)
        }
        assertTrue(map.values.drop(2).all { it.temperature != null && it.humidity != null })
    }

    /** При пересечении диапазонов побеждает первый террейн в порядке ruleset. */
    @Test
    fun overlappingClimateRulesUseFirstMatch() {
        val rules = rules(climate = false)
        rules.terrains.getValue(Constants.plains).uniques.add(condition("-1", "1", "0", "1"))
        rules.terrains.getValue(Constants.desert).uniques.add(condition("-1", "1", "0", "1"))
        val map = map(rules, 42L)
        MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.HumidityAndTemperature)
        assertTrue(map.values.all { it.baseTerrain == Constants.plains })
    }

    /** Непокрытая таблицей температура возвращает первый сухопутный террейн и записывает климат. */
    @Test
    fun missingClimateMatchUsesFirstLandTerrain() {
        val rules = rules(climate = false)
        rules.terrains.getValue(Constants.desert).uniques.add(condition("2", "3", "0", "1"))
        val map = map(rules, 42L)
        MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.HumidityAndTemperature)
        assertTrue(map.values.all { it.baseTerrain == Constants.grassland })
        assertTrue(map.values.all { it.temperature != null && it.humidity != null })
    }

    /** Старый путь без условий сохраняет прежние значения transient-полей даже при смене террейна. */
    @Test
    fun climateWithoutRulesPreservesPreviousClimateFields() {
        val rules = rules(climate = false)
        val map = map(rules, 42L)
        map.values.forEach {
            it.temperature = 0.123
            it.humidity = 0.456
        }
        MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.HumidityAndTemperature)
        map.values.forEach {
            assertEquals(0.123, it.temperature!!, 0.0)
            assertEquals(0.456, it.humidity!!, 0.0)
        }
    }

    /** Неподходящее основание запрещает лес даже при допустимой верхней особенности Hill. */
    @Test
    fun vegetationRejectsDesertHillAndPreservesExistingForest() {
        val rules = rules()
        val map = map(rules, 42L)
        map.mapParameters.vegetationRichness = 1f
        val tiles = map.values.toList()
        tiles[0].baseTerrain = Constants.desert
        tiles[0].setTerrainTransients()
        tiles[0].addTerrainFeature(Constants.hill)
        tiles[1].addTerrainFeature(Constants.forest)
        MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.Vegetation)
        assertEquals(listOf(Constants.hill), tiles[0].terrainFeatures)
        assertEquals(listOf(Constants.forest), tiles[1].terrainFeatures)
        assertTrue(tiles.drop(2).all { it.terrainFeatures == listOf(Constants.forest) })
    }

    /** Сравнивает 36 результатов и расход RNG с наблюдёнными эталонами неизменённого legacy-кода. */
    @Test
    fun observeLegacyLayerSnapshots() {
        fun verify(shape: String, seed: Long, step: String, map: TileMap, generator: MapGenerator) {
            val key = "$shape|$seed|$step"
            val expected = legacySnapshots.getValue(key)
            assertEquals("Слои: $key", expected.first, digest(snapshot(map)))
            assertEquals("RNG: $key", expected.second, randomness(generator).RNG.nextInt())
        }
        assertEquals(36, legacySnapshots.size)
        for (shape in listOf(MapShape.hexagonal, MapShape.rectangular, MapShape.flatEarth)) {
            for (seed in listOf(1L, 42L, -17L)) {
                val rules = rules()
                for (step in listOf(
                    MapGeneratorSteps.Elevation,
                    MapGeneratorSteps.HumidityAndTemperature,
                    MapGeneratorSteps.Vegetation
                )) {
                    val first = map(rules, seed, shape)
                    val second = map(rules, seed, shape)
                    val generator = MapGenerator(rules)
                    generator.generateSingleStep(first, step)
                    MapGenerator(rules).generateSingleStep(second, step)
                    assertEquals(snapshot(first), snapshot(second))
                    verify(shape, seed, step.name, first, generator)
                }
                // Это последовательность выделяемых слоёв, а не полный конвейер с сушей, берегами и ресурсами.
                val sequential = map(rules, seed, shape)
                val generator = MapGenerator(rules)
                randomness(generator).seedRNG(seed)
                for (method in listOf(
                    "raiseMountainsAndHills",
                    "applyHumidityAndTemperature",
                    "spawnVegetation"
                )) {
                    MapGenerator::class.java.getDeclaredMethod(method, TileMap::class.java).apply {
                        isAccessible = true
                    }.invoke(generator, sequential)
                }
                verify(shape, seed, "Sequential", sequential, generator)
            }
        }
    }

    /** Собственная карта каждого запуска исключает накопление особенностей между экспериментами. */
    private fun map(rules: Ruleset, seed: Long, shape: String = MapShape.hexagonal): TileMap {
        val map = if (shape == MapShape.rectangular) TileMap(7, 5, rules) else TileMap(3, rules)
        map.mapParameters = MapParameters().apply {
            this.seed = seed
            this.shape = shape
            mapSize = if (shape == MapShape.rectangular) MapSizeNew(7, 5) else MapSizeNew(3)
        }
        return map
    }

    /** Минимальные реальные правила без файлов и глобального RulesetCache; порядок задан явно. */
    private fun rules(climate: Boolean = true): Ruleset = Ruleset().apply {
        fun add(name: String, type: TerrainType, vararg uniques: String): Terrain {
            val terrain = Terrain().apply {
                this.name = name
                this.type = type
                this.uniques.addAll(uniques)
            }
            terrains[name] = terrain
            return terrain
        }
        add(Constants.grassland, TerrainType.Land)
        add(Constants.plains, TerrainType.Land)
        add(Constants.desert, TerrainType.Land)
        add(Constants.snow, TerrainType.Land)
        add(Constants.tundra, TerrainType.Land)
        add(Constants.ocean, TerrainType.Water)
        add(Constants.mountain, TerrainType.Land, "Rough terrain", "Occurs in chains at high elevations").impassable = true
        add(Constants.hill, TerrainType.TerrainFeature, "Rough terrain", "Occurs in groups around high elevations")
            .occursOn.addAll(listOf(Constants.grassland, Constants.plains, Constants.desert, Constants.snow, Constants.tundra))
        // Один кандидат отделяет характеристику RNG от identity hash объектов Terrain в историческом HashSet.
        add(Constants.forest, TerrainType.TerrainFeature, "Vegetation")
            .occursOn.addAll(listOf(Constants.grassland, Constants.plains, Constants.tundra, Constants.hill))
        if (climate) {
            terrains.getValue(Constants.snow).uniques.add(condition("-1", "-0.4", "0", "1"))
            terrains.getValue(Constants.plains).uniques.add(condition("-0.4", "0.7", "0", "0.5"))
            terrains.getValue(Constants.grassland).uniques.add(condition("-0.4", "1", "0.5", "1"))
            terrains.getValue(Constants.desert).uniques.add(condition("0.7", "1", "0", "0.5"))
        }
    }

    private fun condition(t0: String, t1: String, h0: String, h1: String) =
        "Occurs at temperature between [$t0] and [$t1] and humidity between [$h0] and [$h1]"

    /** Включает порядок клеток, точные Float/Double и порядок особенностей, а не только доли типов. */
    private fun snapshot(map: TileMap): String = map.values.joinToString("\n") {
        "${it.position.x},${it.position.y}|${it.baseTerrain}|${it.terrainFeatures.joinToString(",")}|${it.temperature}|${it.humidity}"
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    /** Доступ только из теста: наблюдение расхода RNG не требует изменения производственного API. */
    private fun randomness(generator: MapGenerator): MapGenerationRandomness =
        MapGenerator::class.java.getDeclaredField("randomness").apply { isAccessible = true }
            .get(generator) as MapGenerationRandomness

    /** Эталоны наблюдённого legacy-запуска до извлечения: форма, seed, этап, SHA-256 и следующий RNG.nextInt. */
    private val legacySnapshots = """
Flat Earth Hexagonal|1|Elevation|454ef24ac2f4f43ab37427c8473e9995bba915d802805afe538e4ef24d6fe668|767537789
Flat Earth Hexagonal|1|HumidityAndTemperature|6c07492d57287b36fdebb0bb37301b2e96992828ccde314333f47ce3327c4015|-527218591
Flat Earth Hexagonal|1|Sequential|f171d29064aedb00c128bb89983cada52cdf089cdb59e5cf637b52e3aeb4115e|1045410217
Flat Earth Hexagonal|1|Vegetation|26f885959917ab8061d223ad3beaf9e226ac193a292d2089957a6f0abdf10cf2|1329509210
Flat Earth Hexagonal|-17|Elevation|153f8e59158f87ebb8c73bb79dd37c685cc9c98943c151eb0a2e7f8b6f5e2f73|-269585518
Flat Earth Hexagonal|-17|HumidityAndTemperature|d856b39f911312cdcee7d1bd3c97a17c941645c24fc59cdfc211ceb4bde88078|2006176460
Flat Earth Hexagonal|-17|Sequential|41f154324ac357d2947a6e4120f44bf18f00ac81c1274a9898c9b1b22368e0f1|2117836223
Flat Earth Hexagonal|-17|Vegetation|855385749fe9bdd797a98076f1191985993402da0e758ffe7fe2b55933d5589c|-2102322988
Flat Earth Hexagonal|42|Elevation|d3ede26259bc24e5d4c46431287a83cbc0dc18a12626d59fc1840f1587b68bf8|-1191040091
Flat Earth Hexagonal|42|HumidityAndTemperature|d7b69f69b3d822492bd14cafe3837fcf867c211e5bea9c8af72b21a2fc1a6311|-408207414
Flat Earth Hexagonal|42|Sequential|ac8dd2c2a65f2128a8716f32739248b6b28339772b8792cbe30ce97f60942239|-1364882158
Flat Earth Hexagonal|42|Vegetation|d184f311fd32dfed3c668b2458c1205c5e713ef8b7176ebee9a9b47e853f1fcb|593343317
Hexagonal|1|Elevation|454ef24ac2f4f43ab37427c8473e9995bba915d802805afe538e4ef24d6fe668|767537789
Hexagonal|1|HumidityAndTemperature|98c94e181646745b66689398e4f293365ae0e16e145b44bc217ec4a780cb37b2|-527218591
Hexagonal|1|Sequential|3cfc425962e9ce0686a0bf9bfd1db598518ed962ed3b497d0068ed425cf50224|-1254667599
Hexagonal|1|Vegetation|26f885959917ab8061d223ad3beaf9e226ac193a292d2089957a6f0abdf10cf2|1329509210
Hexagonal|-17|Elevation|153f8e59158f87ebb8c73bb79dd37c685cc9c98943c151eb0a2e7f8b6f5e2f73|-269585518
Hexagonal|-17|HumidityAndTemperature|f8c530c965461b6e083415f76e6794d842e25290f744722c3875b38cde339d0f|2006176460
Hexagonal|-17|Sequential|5a80e62bb78059b812dcdde595bdc5f6e03d6ff8d3cb068a02709f77f8308694|952446032
Hexagonal|-17|Vegetation|855385749fe9bdd797a98076f1191985993402da0e758ffe7fe2b55933d5589c|-2102322988
Hexagonal|42|Elevation|d3ede26259bc24e5d4c46431287a83cbc0dc18a12626d59fc1840f1587b68bf8|-1191040091
Hexagonal|42|HumidityAndTemperature|c0bc56710de220a60bc98a5a83697590e053b6e1ee5f96fd49727aa4a4c50230|-408207414
Hexagonal|42|Sequential|d51853786fb687b6c8dd5f12572041c382dc91cf18fc83af259c050a0ed90636|-972290542
Hexagonal|42|Vegetation|d184f311fd32dfed3c668b2458c1205c5e713ef8b7176ebee9a9b47e853f1fcb|593343317
Rectangular|1|Elevation|6c76e95cd3fe6741021a6ac5fbcdb3d2e993f5b0c1d07e1f28c0092af9a58648|322919607
Rectangular|1|HumidityAndTemperature|33517ad33a02abbdb208a65f419485edecd806626233d5b61ada391e4cb77c94|-527218591
Rectangular|1|Sequential|94ef4911ec203f4812cc42402c155eaf2f6dcd675d7e00428d4bc4df05771709|-1246546199
Rectangular|1|Vegetation|962c3e1d3f323de870196c0bc9198daf6f5a54c52b34635418a7a2d796d95ff8|-2022884062
Rectangular|-17|Elevation|80f193be6246fc613240372bf9c0535a5991298299496923d710e4e46bd7c0ea|1724584083
Rectangular|-17|HumidityAndTemperature|e2448b953b3edc418ace7764ae273f33f1f2815d8d5f9c1326a87bcf56593372|2006176460
Rectangular|-17|Sequential|573c3431ec50d418c1ce11e8e792e2ee93959245a0b79ab581327a8dd559347e|-194801646
Rectangular|-17|Vegetation|4f7c057aae6b0d220b88b5b068d17dbda1af2f6ccfbdc8bb3ad7eb36fedd8469|1324433380
Rectangular|42|Elevation|82ab8db33cf47ea08c240702c492bfc3042f4e15b6c236e7373553d9ef7ae00b|627104895
Rectangular|42|HumidityAndTemperature|44ebd0ee047b596ced5ae30d8f925826be7f130d3e7bfac9db0a8e24214a807b|-408207414
Rectangular|42|Sequential|c1edde4f92c962985d2cddc88c5f7a40ab1b655a12bdc7c19b9ff12bcea451f4|403120289
Rectangular|42|Vegetation|31801d3461628ef0e936f3f6773fa26ebe1faba174f9d888073e833c05477fdc|-1164069089
""".trimIndent().lines().associate { line ->
        val parts = line.split('|')
        parts.take(3).joinToString("|") to (parts[3] to parts[4].toInt())
    }

    /** Чистая сетка совпадает с TileMap по точным координатам шума, широте и порядку соседей. */
    @Test
    fun experimentGeometryMatchesTileMap() {
        val rules = rules()
        for (shape in listOf(MapShape.hexagonal, MapShape.rectangular)) {
            val map = map(rules, 42L, shape)
            val tiles = map.values.toList()
            val indices = tiles.withIndex().associate { it.value to it.index }
            val cells = if (shape == MapShape.rectangular)
                com.unciv.pure.application.mapgeneration.TerrainGridFactory.rectangular(
                    7,
                    5,
                    Constants.grassland
                )
            else com.unciv.pure.application.mapgeneration.TerrainGridFactory.hexagonal(
                3,
                Constants.grassland
            )
            assertEquals(tiles.size, cells.size)
            tiles.forEachIndexed { index, tile ->
                val cell = cells[index]
                val world = com.unciv.logic.HexMath.hex2WorldCoords(tile.position)
                assertEquals(tile.position.x.toInt(), cell.column)
                assertEquals(tile.position.y.toInt(), cell.row)
                assertEquals(world.x.toDouble(), cell.x, 0.0)
                assertEquals(world.y.toDouble(), cell.y, 0.0)
                assertEquals(tile.latitude, cell.latitude, 0f)
                assertEquals(tile.longitude, cell.longitude, 0f)
                assertEquals(tile.neighbors.map { indices.getValue(it) }.toList(), cell.neighbors)
            }
        }
    }

    /** Общий эксперимент воспроизводит зафиксированные legacy-слои при одинаковом входе и RNG-контексте. */
    @Test
    fun experimentPipelineMatchesLegacySnapshots() {
        for (shape in listOf(MapShape.hexagonal, MapShape.rectangular, MapShape.flatEarth)) {
            for (seed in listOf(1L, 42L, -17L)) {
                val rules = rules()
                val cells = if (shape == MapShape.rectangular)
                    com.unciv.pure.application.mapgeneration.TerrainGridFactory.rectangular(
                        7,
                        5,
                        Constants.grassland
                    )
                else com.unciv.pure.application.mapgeneration.TerrainGridFactory.hexagonal(
                    3,
                    Constants.grassland
                )
                val ruleSnapshot =
                        com.unciv.logic.map.mapgenerator.TerrainRuleSnapshotAdapter.snapshot(rules)
                for ((mode, legacyStep) in listOf(
                    com.unciv.pure.application.mapgeneration.TerrainGenerationMode.ALL to "Sequential",
                    com.unciv.pure.application.mapgeneration.TerrainGenerationMode.CLIMATE to "HumidityAndTemperature",
                    com.unciv.pure.application.mapgeneration.TerrainGenerationMode.ELEVATION to "Elevation",
                    com.unciv.pure.application.mapgeneration.TerrainGenerationMode.VEGETATION to "Vegetation"
                )) {
                    val input = com.unciv.pure.application.mapgeneration.TerrainExperimentInput(
                        cells, ruleSnapshot,
                        com.unciv.pure.application.mapgeneration.TerrainExperimentSettings(
                            seed = seed, flatEarth = shape == MapShape.flatEarth, mode = mode
                        )
                    )
                    val result =
                            com.unciv.pure.application.mapgeneration.TerrainGenerationPipeline.generate(
                                input
                            )
                    val serialized = result.cells.joinToString("\n") {
                        "${it.column.toFloat()},${it.row.toFloat()}|${it.baseTerrain}|${
                            it.features.joinToString(
                                ","
                            )
                        }|${it.temperature}|${it.humidity}"
                    }
                    val key = "$shape|$seed|$legacyStep"
                    assertEquals(key, legacySnapshots.getValue(key).first, digest(serialized))
                }
            }
        }
    }
}
