package com.unciv.logic.map.mapgenerator

import com.unciv.Constants
import com.unciv.logic.HexMath
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.pure.domain.mapgeneration.ClimateCell
import com.unciv.pure.domain.mapgeneration.ClimateGenerator
import com.unciv.pure.domain.mapgeneration.ClimateSettings
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import kotlin.random.Random

/** Граница legacy: снимает значения правил и карты, вызывает чистое ядро и применяет завершённый результат. */
internal object TerrainGenerationAdapter {
    fun applyClimate(map: TileMap, ruleset: Ruleset, random: Random) {
        val parameters = map.mapParameters
        // Проверяем масштаб до RNG и любых изменений карты.
        require(parameters.tilesPerBiomeArea > 0) { "Climate scale must be positive" }
        require(parameters.temperatureExtremeness.isFinite() && parameters.temperatureShift.isFinite()) {
            "Climate parameters must be finite"
        }
        val humiditySeed = random.nextInt().toDouble()
        val temperatureSeed = random.nextInt().toDouble()
        map.setTransients(ruleset)
        val settings = ClimateSettings(
            parameters.tilesPerBiomeArea.toDouble(), parameters.temperatureExtremeness,
            parameters.temperatureShift, parameters.shape === MapShape.flatEarth,
            map.maxLatitude, map.maxLongitude
        )
        val ranges = ruleset.terrains.values.filter { it.type == TerrainType.Land }.flatMap { terrain ->
            terrain.getMatchingUniques(UniqueType.TileGenerationConditions).map { unique ->
                ClimateTerrainRange.inclusiveMinimums(terrain.name,
                    unique.params[0].toFloat(), unique.params[1].toFloat(),
                    unique.params[2].toFloat(), unique.params[3].toFloat())
            }.toList()
        }
        val elevations = ruleset.terrains.values.filter { it.hasUnique(UniqueType.OccursInChains) }
            .mapTo(mutableSetOf()) { it.name }
        if (elevations.isEmpty()) elevations.add(Constants.mountain)
        val tiles = map.values.toList()
        val cells = tiles.map { tile ->
            val world = HexMath.hex2WorldCoords(tile.position)
            ClimateCell(world.x.toDouble(), world.y.toDouble(), tile.latitude, tile.longitude,
                tile.baseTerrain, tile.isWater || tile.baseTerrain in elevations, tile.temperature, tile.humidity)
        }
        val results = ClimateGenerator.generate(cells, settings, ranges, ruleset.terrains.keys.toSet(),
            ruleset.terrains.values.first { it.type == TerrainType.Land }.name, humiditySeed, temperatureSeed)
        // Проверка всего результата предшествует публикации базовых террейнов.
        require(results.size == tiles.size && results.all { it.baseTerrain in ruleset.terrains })
        for ((index, result) in results.withIndex()) {
            if (!result.processed) continue
            val tile = tiles[index]
            tile.baseTerrain = result.baseTerrain
            tile.temperature = result.temperature
            tile.humidity = result.humidity
            tile.setTerrainTransients()
        }
    }

    /** Сохраняет исторический порядок HashSet кандидатов; чистое ядро не сортирует и не пересоздаёт его. */
    fun applyVegetation(map: TileMap, ruleset: Ruleset, random: Random) {
        val parameters = map.mapParameters
        require(parameters.vegetationScale.isFinite() && parameters.vegetationScale > 0.0) {
            "Vegetation scale must be positive and finite"
        }
        val terrains = (Constants.vegetation.mapNotNull { ruleset.terrains[it] }
                + ruleset.terrains.values.filter { it.hasUnique(UniqueType.Vegetation) }).toHashSet()
        val rules = terrains.map {
            com.unciv.pure.domain.mapgeneration.VegetationRule(it.name, it.occursOn.toList())
        }
        val tiles = map.values.toList()
        val cells = tiles.map { tile ->
            val world = HexMath.hex2WorldCoords(tile.position)
            com.unciv.pure.domain.mapgeneration.VegetationCell(
                world.x.toDouble(), world.y.toDouble(), tile.baseTerrain, tile.getLastTerrain().name
            )
        }
        val additions = com.unciv.pure.domain.mapgeneration.VegetationGenerator.generate(
            cells, rules, parameters.vegetationRichness, random, parameters.vegetationScale
        )
        require(additions.size == tiles.size && additions.all { it == null || it in ruleset.terrains })
        additions.forEachIndexed { index, terrain ->
            if (terrain != null) tiles[index].addTerrainFeature(terrain)
        }
    }

    /** Снимает соседство и свойства террейнов, затем применяет полностью вычисленный рельеф. */
    fun applyElevation(map: TileMap, ruleset: Ruleset, random: Random) {
        val parameters = map.mapParameters
        require(parameters.elevationScale.isFinite() && parameters.elevationScale > 0.0) {
            "Elevation scale must be positive and finite"
        }
        require(parameters.elevationExponent.isFinite()) { "Elevation exponent must be finite" }
        val mountain =
                ruleset.terrains.values.firstOrNull { it.hasUnique(UniqueType.OccursInChains) }?.name
        val hill =
                ruleset.terrains.values.firstOrNull { it.hasUnique(UniqueType.OccursInGroups) }?.name
        val flat = ruleset.terrains.values.firstOrNull {
            !it.impassable && it.type == TerrainType.Land && !it.hasUnique(UniqueType.RoughTerrain)
        }?.name ?: return
        map.setTransients(ruleset)
        val tiles = map.values.toList()
        val indices = tiles.withIndex().associate { it.value to it.index }
        val cells = tiles.map { tile ->
            val world = HexMath.hex2WorldCoords(tile.position)
            com.unciv.pure.domain.mapgeneration.ElevationCell(
                world.x.toDouble(),
                world.y.toDouble(),
                tile.baseTerrain,
                tile.terrainFeatures.toList(),
                tile.isWater,
                tile.neighbors.map { indices.getValue(it) }.toList()
            )
        }
        val rules = com.unciv.pure.domain.mapgeneration.ElevationRules(
            flat, mountain, hill,
            ruleset.terrains.values.filter { it.impassable }.mapTo(mutableSetOf()) { it.name },
            ruleset.terrains.values.filter { it.type == TerrainType.Water }
                .mapTo(mutableSetOf()) { it.name },
            Constants.rising, Constants.lowering
        )
        val results = com.unciv.pure.domain.mapgeneration.ElevationGenerator.generate(
            cells, rules, parameters.elevationExponent, random, parameters.elevationScale
        )
        require(results.size == tiles.size && results.all { it.baseTerrain in ruleset.terrains })
        for ((index, result) in results.withIndex()) {
            if (cells[index].water) continue
            val tile = tiles[index]
            tile.baseTerrain = result.baseTerrain
            tile.setTerrainFeatures(result.features)
            tile.setTerrainTransients()
        }
    }
}
