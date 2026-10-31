package com.unciv.logic.map.mapgenerator

import com.unciv.logic.HexMath
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapType
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.domain.mapgeneration.MountainZoneCell
import com.unciv.pure.domain.mapgeneration.MountainZoneGenerator
import com.unciv.pure.domain.mapgeneration.MountainZoneLayout
import com.unciv.pure.domain.mapgeneration.MountainZoneMode

/** Единственная граница между чистой топологией горных зон и изменяемой игровой картой. */
internal object MountainZoneGenerationAdapter {
    fun validate(parameters: MapParameters, ruleset: Ruleset) {
        parameters.validateMountainZones()
        if (!parameters.isMountainZoneType()) return
        mountain(ruleset)
        flat(ruleset)
    }

    /** Вычисляет весь результат на реальном соседстве до изменения клеток. */
    fun prepare(map: TileMap): MountainZoneLayout? {
        if (!map.mapParameters.isMountainZoneType()) return null
        validate(map.mapParameters, map.ruleset!!)
        val tiles = tiles(map)
        val indices = tiles.withIndex().associate { it.value to it.index }
        val cells = tiles.map { tile ->
            val world = HexMath.hex2WorldCoords(tile.position)
            MountainZoneCell(
                world.x.toDouble(), world.y.toDouble(),
                tile.neighbors.map { indices.getValue(it) }.distinct().sorted().toList()
            )
        }
        return MountainZoneGenerator.generate(
            cells, map.mapParameters.mountainZoneCount,
            if (map.mapParameters.type == MapType.mountainCross) MountainZoneMode.CROSS else MountainZoneMode.NETWORK,
            map.mapParameters.seed
        )
    }

    /** Стены и проходы имеют приоритет над случайными горами, водой и блокирующими чудесами. */
    fun apply(map: TileMap, layout: MountainZoneLayout?) {
        if (layout == null) return
        val rules = map.ruleset!!
        val mountain = mountain(rules)
        val flat = flat(rules)
        val tiles = tiles(map)
        require(tiles.size == layout.zoneByCell.size)
        for ((index, tile) in tiles.withIndex()) {
            tile.setTerrainTransients()
            if (layout.zoneByCell[index] < 0) {
                tile.baseTerrain = mountain
                tile.setTerrainFeatures(emptyList())
                tile.naturalWonder = null
                tile.resource = null
                tile.resourceAmount = 0
                tile.improvement = null
            } else {
                if (tile.isWater || tile.getBaseTerrain().impassable) tile.baseTerrain = flat
                tile.setTerrainFeatures(tile.terrainFeatures.filter { rules.terrains[it]?.impassable != true })
                if (tile.naturalWonder?.let { rules.terrains[it]?.impassable == true || rules.terrains[it]?.type == TerrainType.Water } == true)
                    tile.naturalWonder = null
            }
            tile.setTerrainTransients()
        }
    }

    /** Сортировка отделяет seed-топологию от порядка хранения клеток и сериализации. */
    private fun tiles(map: TileMap) = map.values.sortedWith(compareBy({ it.position.x }, { it.position.y }))

    private fun mountain(rules: Ruleset): String = rules.terrains.values.firstOrNull {
        it.type == TerrainType.Land && it.impassable && it.hasUnique(UniqueType.OccursInChains)
    }?.name ?: rules.terrains.values.firstOrNull {
        it.name == "Mountain" && it.type == TerrainType.Land && it.impassable
    }?.name ?: throw IllegalArgumentException("Mountain zones require an impassable mountain terrain in the selected ruleset")

    private fun flat(rules: Ruleset): String = rules.terrains.values.firstOrNull {
        it.type == TerrainType.Land && !it.impassable && !it.hasUnique(UniqueType.RoughTerrain)
    }?.name ?: throw IllegalArgumentException("Mountain zones require passable land in the selected ruleset")
}
