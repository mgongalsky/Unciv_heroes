package com.unciv.testing.mapgeneration

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.unciv.UncivGame
import com.unciv.logic.HexMath
import com.unciv.logic.MapSaver
import com.unciv.logic.map.*
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSettings
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.mapgeneration.*
import com.unciv.ui.mapeditor.MapGeneratorSteps
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Проверяет реальный игровой путь отдельно от независимых тестов топологии. */
class MountainZoneGenerationTest {
    /** Поддержанные обычные геометрии сохраняют стены и одноклеточные проходы после полного запуска. */
    @Test
    fun completeGenerationKeepsTopologyAcrossSupportedShapes() = withRules { rules ->
        for (type in listOf(MapType.mountainCross, MapType.mountainZones)) {
            for (shape in listOf(MapShape.hexagonal, MapShape.rectangular)) {
                verify(MapGenerator(rules).generateMap(parameters(type, shape)))
            }
        }
    }

    /** Климат, рельеф, чудеса и прочие Partial сохраняют тот же физический граф проходов. */
    @Test
    fun partialStepsCannotBreakWallsOrBlockPassages() = withRules { rules ->
        for (type in listOf(MapType.mountainCross, MapType.mountainZones)) {
            val map = MapGenerator(rules).generateMap(parameters(type))
            for (step in listOf(MapGeneratorSteps.Elevation, MapGeneratorSteps.HumidityAndTemperature,
                MapGeneratorSteps.Vegetation, MapGeneratorSteps.RareFeatures, MapGeneratorSteps.NaturalWonders,
                MapGeneratorSteps.LakesAndCoast, MapGeneratorSteps.Rivers, MapGeneratorSteps.Resources)) {
                MapGenerator(rules).generateSingleStep(map, step)
                verify(map)
            }
        }
    }

    /** Seed воспроизводит готовую карту; сохранение, clone и старый JSON сохраняют контракт числа зон. */
    @Test
    fun repeatsAndSurvivesSavedMapRoundTrip() = withRules { rules ->
        for (type in listOf(MapType.mountainCross, MapType.mountainZones)) {
            val parameters = parameters(type).apply { mountainZoneCount = 8 }
            assertEquals(8, parameters.clone().mountainZoneCount)
            val first = MapGenerator(rules).generateMap(parameters.clone())
            val second = MapGenerator(rules).generateMap(parameters.clone())
            assertEquals(snapshot(first), snapshot(second))
            val restored = MapSaver.mapFromSavedString(MapSaver.mapToSavedString(first))
            restored.setTransients(rules, false)
            assertEquals(type, restored.mapParameters.type)
            assertEquals(8, restored.mapParameters.mountainZoneCount)
            assertEquals(snapshot(first), snapshot(restored))
            verify(restored)
        }
        val old = Json().fromJson(MapParameters::class.java, "{\"type\":\"Pangaea\",\"seed\":42}")
        assertEquals(5, old.mountainZoneCount)
        assertFalse(old.isMountainZoneType())
        old.mountainZoneCount = 0
        old.validateMountainZones() // Неиспользуемое поле не меняет контракт старых типов.
    }

    /** Неподдержанные запросы, включая legacy wrap, отклоняются до seed, метаданных и мутации карты. */
    @Test
    fun rejectsInvalidRequestsBeforeMutation() = withRules { rules ->
        for (invalid in listOf(
            parameters(MapType.mountainCross).apply { mountainZoneCount = 13 },
            parameters(MapType.mountainZones).apply { mapSize = MapSizeNew(4, 4) },
            parameters(MapType.mountainCross).apply { shape = MapShape.flatEarth },
            parameters(MapType.mountainCross).apply { worldWrap = true }
        )) {
            invalid.seed = 0L
            expectInvalid { MapGenerator(rules).generateMap(invalid) }
            assertEquals(0L, invalid.seed)
            assertEquals("", invalid.createdWithVersion)
        }
        val map = MapGenerator(rules).generateMap(parameters(MapType.mountainCross))
        map.mapParameters.mountainZoneCount = 2
        map.mapParameters.seed = 0L
        val before = snapshot(map)
        expectInvalid { MapGenerator(rules).generateSingleStep(map, MapGeneratorSteps.Elevation) }
        assertEquals(before, snapshot(map))
        assertEquals(0L, map.mapParameters.seed)
    }

    private fun parameters(type: String, shape: String = MapShape.rectangular, wrap: Boolean = false) = MapParameters().apply {
        seed = 42L
        this.type = type
        this.shape = shape
        worldWrap = wrap
        mapSize = if (shape == MapShape.rectangular) MapSizeNew(36, 28) else MapSizeNew(MapSize.Tiny)
        noRuins = true // Поселения форка требуют отдельного игрового окружения.
    }

    private fun verify(map: TileMap) {
        val tiles = map.values.sortedWith(compareBy({ it.position.x }, { it.position.y }))
        val indices = tiles.withIndex().associate { it.value to it.index }
        val actualEdges = tiles.map { tile -> tile.neighbors.map { indices.getValue(it) }.toList() }
        val closedEdges = List(tiles.size) { sortedSetOf<Int>() }
        val blocked = BooleanArray(tiles.size)
        actualEdges.forEachIndexed { i, neighbors ->
            neighbors.forEach { j ->
                if (i != j) {
                    closedEdges[i].add(j)
                    closedEdges[j].add(i)
                    if (i !in actualEdges[j]) {
                        blocked[i] = true; blocked[j] = true
                    }
                }
            }
        }
        val cells = tiles.mapIndexed { i, tile ->
            val world = HexMath.hex2WorldCoords(tile.position)
            MountainZoneCell(
                world.x.toDouble(),
                world.y.toDouble(),
                closedEdges[i].toList(),
                blocked[i]
            )
        }
        val layout = MountainZoneGenerator.generate(
            cells, map.mapParameters.mountainZoneCount,
            if (map.mapParameters.type == MapType.mountainCross) MountainZoneMode.CROSS else MountainZoneMode.NETWORK,
            map.mapParameters.seed
        )
        for (i in tiles.indices) {
            if (layout.zoneByCell[i] < 0) assertTrue(
                "Wall became passable at ${tiles[i].position}",
                tiles[i].isImpassible()
            )
            else assertTrue(
                "Room or passage blocked at ${tiles[i].position}",
                tiles[i].isLand && !tiles[i].isImpassible()
            )
        }
        // Настоящие направленные переходы должны соединять все зоны в обе стороны.
        fun reachable(start: Int, removed: Set<Int>): Set<Int> {
            val seen = mutableSetOf(start)
            val queue = ArrayDeque<Int>()
            queue.addLast(start)
            while (queue.isNotEmpty()) {
                for (j in actualEdges[queue.removeFirst()]) if (j !in removed && !tiles[j].isImpassible() && seen.add(
                            j
                        )
                ) queue.addLast(j)
            }
            return seen
        }
        for (start in layout.centers) assertTrue(
            "Zone cannot reach other zones",
            reachable(start, emptySet()).containsAll(layout.centers)
        )
        for (gate in layout.passages) assertFalse(
            "Guard can be bypassed",
            reachable(layout.centers.first(), setOf(gate.guard)).containsAll(layout.centers)
        )
    }

    private fun snapshot(map: TileMap) = map.values.sortedWith(compareBy({ it.position.x }, { it.position.y })).map {
        listOf(it.position.cpy(), it.baseTerrain, it.terrainFeatures.toList(), it.naturalWonder,
            it.resource, it.resourceAmount, it.improvement, it.hasBottomRiver, it.hasBottomLeftRiver, it.hasBottomRightRiver)
    }

    private fun expectInvalid(action: () -> Unit) {
        try { action(); fail("Invalid mountain zone request accepted") } catch (_: IllegalArgumentException) { }
    }

    /** Пользовательские настройки и кеши восстанавливаются даже при падении теста. */
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
            for (base in BaseRuleset.values()) RulesetCache[base.fullName] = Ruleset().apply {
                load(FileHandle(File(assets, "jsons/${base.fullName}")))
                name = base.fullName
            }
            test(RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName))
        } finally {
            RulesetCache.clear()
            RulesetCache.putAll(cached)
            currentField.set(null, previous)
        }
    }
}
