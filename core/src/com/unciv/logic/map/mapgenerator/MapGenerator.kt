package com.unciv.logic.map.mapgenerator

import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.HexMath
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.ClimateParameters
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapType
import com.unciv.logic.map.Perlin
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.models.Counter
import com.unciv.models.metadata.GameParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.ResourceType
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.ui.mapeditor.MapGeneratorSteps
import com.unciv.utils.Log
import com.unciv.utils.debug
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.ulp
import kotlin.random.Random

class MapGenerator(val ruleset: Ruleset) {
    companion object {
        private const val consoleTimings = false
    }

    private val landTerrainName =
            MapLandmassGenerator.getInitializationTerrain(ruleset, TerrainType.Land)
    private val waterTerrainName: String = try {
        MapLandmassGenerator.getInitializationTerrain(ruleset, TerrainType.Water)
    } catch (_: Exception) {
        landTerrainName
    }

    private var randomness = MapGenerationRandomness()
    private val firstLandTerrain = ruleset.terrains.values.first { it.type==TerrainType.Land }

    /** Associates [terrain] with a range of temperatures and a range of humidities (both open to closed) */
    private class TerrainOccursRange(
        val terrain: Terrain,
        val tempFrom: Float, val tempTo: Float,
        val humidFrom: Float, val humidTo: Float
    ) {
        /** builds a [TerrainOccursRange] for [terrain] from a [unique] (type [UniqueType.TileGenerationConditions]) */
        constructor(terrain: Terrain, unique: Unique)
                : this(terrain,
            unique.params[0].toFloatMakeInclusive(-1f), unique.params[1].toFloat(),
            unique.params[2].toFloatMakeInclusive(0f), unique.params[3].toFloat())

        /** Checks if both [temperature] and [humidity] satisfy their ranges (>From, <=To)
         *  Note the lowest allowed limit has been made inclusive (temp -1 nudged down by 1 [Float.ulp], humidity at 0)
         */

        // Yes this does implicit conversions Float/Double
        fun matches(temperature: Double, humidity: Double) =
            tempFrom < temperature && temperature <= tempTo &&
            humidFrom < humidity && humidity <= humidTo

        companion object {
            /** A [toFloat] that also nudges the value slightly down if it matches [limit] to make the resulting range inclusive on the lower end */
            private fun String.toFloatMakeInclusive(limit: Float): Float {
                val result = toFloat()
                if (result != limit) return result
                return result - result.ulp
            }
        }
    }
    private fun Terrain.getGenerationConditions() =
        getMatchingUniques(UniqueType.TileGenerationConditions)
            .map { unique -> TerrainOccursRange(this, unique) }

    fun generateMap(mapParameters: MapParameters, gameParameters: GameParameters = GameParameters(), civilizations: List<CivilizationInfo> = emptyList()): TileMap {
        val mapSize = mapParameters.mapSize
        val mapType = mapParameters.type

        if (mapParameters.seed == 0L)
            mapParameters.seed = System.currentTimeMillis()

        randomness.seedRNG(mapParameters.seed)

        val map: TileMap = if (mapParameters.shape == MapShape.rectangular)
            TileMap(mapSize.width, mapSize.height, ruleset, mapParameters.worldWrap)
        else
            TileMap(mapSize.radius, ruleset, mapParameters.worldWrap)

        mapParameters.createdWithVersion = UncivGame.VERSION.toNiceString()
        map.mapParameters = mapParameters

        if (mapType == MapType.empty) {
            for (tile in map.values) {
                tile.baseTerrain = Constants.ocean
                tile.setTerrainTransients()
            }

            return map
        }

        if (consoleTimings) debug("\nMapGenerator run with parameters %s", mapParameters)
        runAndMeasure("MapLandmassGenerator") {
            MapLandmassGenerator(ruleset, randomness).generateLand(map)
        }
        runAndMeasure("raiseMountainsAndHills") {
            raiseMountainsAndHills(map)
        }
        runAndMeasure("applyHumidityAndTemperature") {
            applyHumidityAndTemperature(map)
        }
        runAndMeasure("spawnLakesAndCoasts") {
            spawnLakesAndCoasts(map)
        }
        runAndMeasure("spawnVegetation") {
            spawnVegetation(map)
        }
        runAndMeasure("spawnRareFeatures") {
            spawnRareFeatures(map)
        }
        runAndMeasure("spawnIce") {
            spawnIce(map)
        }
        runAndMeasure("assignContinents") {
            map.assignContinents(TileMap.AssignContinentsMode.Assign)
        }
        runAndMeasure("RiverGenerator") {
            RiverGenerator(map, randomness, ruleset).spawnRivers()
        }
        convertTerrains(map, ruleset)

        // Region based map generation - not used when generating maps in map editor
        if (civilizations.isNotEmpty()) {
            val regions = MapRegions(ruleset)
            runAndMeasure("generateRegions") {
                regions.generateRegions(map, civilizations.count { ruleset.nations[it.civName]!!.isMajorCiv() })
            }
            runAndMeasure("assignRegions") {
                regions.assignRegions(map, civilizations.filter { ruleset.nations[it.civName]!!.isMajorCiv() }, gameParameters)
            }
            // Natural wonders need to go before most resources since there is a minimum distance
            runAndMeasure("NaturalWonderGenerator") {
                NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
            }
            runAndMeasure("placeResourcesAndMinorCivs") {
                regions.placeResourcesAndMinorCivs(map, civilizations.filter { ruleset.nations[it.civName]!!.isCityState() })
            }
        } else {
            runAndMeasure("NaturalWonderGenerator") {
                NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
            }
            // Fallback spread resources function - used when generating maps in map editor
            runAndMeasure("spreadResources") {
                spreadResources(map)
            }
        }
        runAndMeasure("spreadAncientRuins") {
            spreadAncientRuins(map)
        }
        return map
    }

    /**
     * Generates a deterministic battlefield map with a horizontal gradient.
     * The left side uses the attacker's climate parameters and the right side uses the defender's.
     *
     * For each tile:
     *   - A normalized parameter t (0..1) is computed based on its x coordinate.
     *   - Climate parameters (elevation, temperature, humidity) are linearly interpolated between the attacker and defender.
     *   - Based on final elevation, temperature, and humidity, a terrain type is chosen.
     *     If elevation is above a hill threshold, the "Hill" feature is added.
     *
     * @param width Number of tiles horizontally.
     * @param height Number of tiles vertically.
     * @param attackerTile Base tile for the attacker (left side).
     * @param defenderTile Base tile for the defender (right side).
     * @return The generated TileMap.
     */
    // Старый публичный API — делегирует новому. Не трогаем вызывающий код.
    fun generateBattlefield(
        width: Int,
        height: Int,
        attackerTile: TileInfo,
        defenderTile: TileInfo
    ): TileMap {
        val attackerParams = attackerTile.getClimateParameters()
            ?: throw Exception("Attacker tile has no climate parameters")
        val defenderParams = defenderTile.getClimateParameters()
            ?: throw Exception("Defender tile has no climate parameters")
        println("Attacker params: ${attackerTile.baseTerrain} $attackerParams")
        println("Defender params: ${defenderTile.baseTerrain} $defenderParams")
        return generateBattlefield(width, height, attackerParams = attackerParams, defenderParams = defenderParams)
    }

    // Shared terrain rules; this adapter only builds the graphical map representation.
    fun generateBattlefield(
        width: Int,
        height: Int,
        attackerParams: ClimateParameters,
        defenderParams: ClimateParameters
    ): TileMap {
        val battlefield = TileMap(width, height, ruleset, worldWrap = false)
        val temperatureSeed = randomness.RNG.nextDouble()
        for (tile in battlefield.values) {
            val terrain = com.unciv.pure.application.battle.BattlefieldTerrain.at(
                tile.position, width, attackerParams, defenderParams, temperatureSeed
            )
            tile.baseTerrain = terrain.baseTerrain
            tile.temperature = terrain.temperature
            tile.humidity = terrain.humidity
            tile.setTerrainFeatures(terrain.features)
            tile.setTerrainTransients()
        }
        return battlefield
    }
    fun generateSingleStep(map: TileMap, step: MapGeneratorSteps) {
        if (map.mapParameters.seed == 0L)
            map.mapParameters.seed = System.currentTimeMillis()

        randomness.seedRNG(map.mapParameters.seed)

        when(step) {
            MapGeneratorSteps.None -> return
            MapGeneratorSteps.All -> throw IllegalArgumentException("MapGeneratorSteps.All cannot be used in generateSingleStep")
            MapGeneratorSteps.Landmass -> MapLandmassGenerator(ruleset, randomness).generateLand(map)
            MapGeneratorSteps.Elevation -> raiseMountainsAndHills(map)
            MapGeneratorSteps.HumidityAndTemperature -> applyHumidityAndTemperature(map)
            MapGeneratorSteps.LakesAndCoast -> spawnLakesAndCoasts(map)
            MapGeneratorSteps.Vegetation -> spawnVegetation(map)
            MapGeneratorSteps.RareFeatures -> spawnRareFeatures(map)
            MapGeneratorSteps.Ice -> spawnIce(map)
            MapGeneratorSteps.Continents -> map.assignContinents(TileMap.AssignContinentsMode.Reassign)
            MapGeneratorSteps.NaturalWonders -> NaturalWonderGenerator(ruleset, randomness).spawnNaturalWonders(map)
            MapGeneratorSteps.Rivers -> {
                RiverGenerator(map, randomness, ruleset).spawnRivers()
                convertTerrains(map, ruleset)
            }
            MapGeneratorSteps.Resources -> spreadResources(map)
            MapGeneratorSteps.AncientRuins -> spreadAncientRuins(map)
        }
    }

    private fun runAndMeasure(text: String, action: ()->Unit) {
        if (!consoleTimings) return action()
        val startNanos = System.nanoTime()
        action()
        val delta = System.nanoTime() - startNanos
        debug("MapGenerator.%s took %s.%sms", text, delta/1000000L, (delta/10000L).rem(100))
    }

    private fun convertTerrains(map: TileMap, ruleset: Ruleset) {
        for (tile in map.values) {
            val conversionUnique =
                tile.getBaseTerrain().getMatchingUniques(UniqueType.ChangesTerrain)
                    .firstOrNull { tile.isAdjacentTo(it.params[1]) }
                    ?: continue
            val terrain = ruleset.terrains[conversionUnique.params[0]] ?: continue
            if (!terrain.occursOn.contains(tile.getLastTerrain().name)) continue

            if (terrain.type == TerrainType.TerrainFeature)
                tile.addTerrainFeature(terrain.name)
            else tile.baseTerrain = terrain.name
            tile.setTerrainTransients()
        }
    }

    private fun spawnLakesAndCoasts(map: TileMap) {

        if (ruleset.terrains.containsKey(Constants.lakes)) {
            //define lakes
            val waterTiles = map.values.filter { it.isWater }.toMutableList()

            val tilesInArea = ArrayList<TileInfo>()
            val tilesToCheck = ArrayList<TileInfo>()

            val maxLakeSize = ruleset.modOptions.constants.maxLakeSize

            while (waterTiles.isNotEmpty()) {
                val initialWaterTile = waterTiles.removeFirst()
                tilesInArea += initialWaterTile
                tilesToCheck += initialWaterTile

                // Floodfill to cluster water tiles
                while (tilesToCheck.isNotEmpty()) {
                    val tileWeAreChecking = tilesToCheck.removeFirst()
                    for (vector in tileWeAreChecking.neighbors
                        .filter { !tilesInArea.contains(it) and waterTiles.contains(it) }) {
                        tilesInArea += vector
                        tilesToCheck += vector
                        waterTiles -= vector
                    }
                }

                if (tilesInArea.size <= maxLakeSize) {
                    for (tile in tilesInArea) {
                        tile.baseTerrain = Constants.lakes
                        tile.setTransients()
                    }
                }
                tilesInArea.clear()
            }
        }

        //Coasts
        if (ruleset.terrains.containsKey(Constants.coast)) {
            for (tile in map.values.filter { it.baseTerrain == Constants.ocean }) {
                val coastLength =
                    max(1, randomness.RNG.nextInt(max(1, map.mapParameters.maxCoastExtension)))
                if (tile.getTilesInDistance(coastLength).any { it.isLand }) {
                    tile.baseTerrain = Constants.coast
                    tile.setTransients()
                }
            }
        }
    }

    /** We rewrote this function for generation of Small villages */
    private fun spreadAncientRuins(map: TileMap) {
        val ruinsEquivalents = ruleset.tileImprovements.filter { it.value.isAncientRuinsEquivalent() }
        if (map.mapParameters.noRuins || ruinsEquivalents.isEmpty() )
            return
        val suitableTiles = map.values.filter { it.isLand && !it.isImpassible() }
        val locations = randomness.chooseSpreadOutLocations(
                (suitableTiles.size * ruleset.modOptions.constants.ancientRuinCountMultiplier * 5).roundToInt(),
                suitableTiles,
                map.mapParameters.mapSize.radius)
        for (tile in locations)
            tile.changeImprovement("Small village")
            //tile.changeImprovement(ruinsEquivalents.keys.random())
    }

    private fun spreadResources(tileMap: TileMap) {
        val mapRadius = tileMap.mapParameters.mapSize.radius
        for (tile in tileMap.values)
            tile.resource = null

        spreadStrategicResources(tileMap, mapRadius)
        spreadResources(tileMap, mapRadius, ResourceType.Luxury)
        spreadResources(tileMap, mapRadius, ResourceType.Bonus)
    }

    // Here, we need each specific resource to be spread over the map - it matters less if specific resources are near each other
    private fun spreadStrategicResources(tileMap: TileMap, mapRadius: Int) {
        val strategicResources = ruleset.tileResources.values.filter { it.resourceType == ResourceType.Strategic }
        // passable land tiles (no mountains, no wonders) without resources yet
        // can't be next to NW
        val candidateTiles = tileMap.values.filter { it.resource == null && !it.isImpassible()
                && it.neighbors.none { neighbor -> neighbor.isNaturalWonder() }}
        val totalNumberOfResources = candidateTiles.count { it.isLand } * tileMap.mapParameters.resourceRichness
        val resourcesPerType = (totalNumberOfResources/strategicResources.size).toInt()
        for (resource in strategicResources) {
            // remove the tiles where previous resources have been placed
            val suitableTiles = candidateTiles
                    .filterNot { it.baseTerrain == Constants.snow && it.isHill() }
                    .filter { it.resource == null
                            && resource.terrainsCanBeFoundOn.contains(it.getLastTerrain().name) }

            val locations = randomness.chooseSpreadOutLocations(resourcesPerType, suitableTiles, mapRadius)

            for (location in locations) location.setTileResource(resource, rng = randomness.RNG)
        }
    }

    /**
     * Spreads resources of type [resourceType] picking locations at a minimum distance from each other,
     * which is determined from [mapRadius] and then tuned down until the desired number fits.
     * [MapParameters.resourceRichness] used to control how many resources to spawn.
     */
    private fun spreadResources(tileMap: TileMap, mapRadius: Int, resourceType: ResourceType) {
        val resourcesOfType = ruleset.tileResources.values.filter { it.resourceType == resourceType }

        val suitableTiles = tileMap.values
                .filterNot { it.baseTerrain == Constants.snow && it.isHill() }
                .filter { it.resource == null && it.neighbors.none { neighbor -> neighbor.isNaturalWonder() }
                        && resourcesOfType.any { r -> r.terrainsCanBeFoundOn.contains(it.getLastTerrain().name) } }
        val numberOfResources = tileMap.values.count { it.isLand && !it.isImpassible() } *
                tileMap.mapParameters.resourceRichness
        val locations = randomness.chooseSpreadOutLocations(numberOfResources.toInt(), suitableTiles, mapRadius)

        val resourceToNumber = Counter<String>()

        for (tile in locations) {
            val possibleResources = resourcesOfType
                    .filter { it.terrainsCanBeFoundOn.contains(tile.getLastTerrain().name) }
            if (possibleResources.isEmpty()) continue
            val resourceWithLeastAssignments = possibleResources.minByOrNull { resourceToNumber[it.name]!! }!!
            resourceToNumber.add(resourceWithLeastAssignments.name, 1)
            tile.setTileResource(resourceWithLeastAssignments, rng = randomness.RNG)
        }
    }


    /** Общие правила шума и клеточного рельефа; масштаб не меняет показатель преобразования высоты. */
    private fun raiseMountainsAndHills(tileMap: TileMap) {
        TerrainGenerationAdapter.applyElevation(tileMap, ruleset, randomness.RNG)
    }

    /** Применяет общее чистое климатическое ядро с историческим потоком случайности генератора. */
    private fun applyHumidityAndTemperature(tileMap: TileMap) {
        TerrainGenerationAdapter.applyClimate(tileMap, ruleset, randomness.RNG)
    }

    /** Применяет общее правило растительности с историческим порядком кандидатов и RNG. */
    private fun spawnVegetation(tileMap: TileMap) {
        TerrainGenerationAdapter.applyVegetation(tileMap, ruleset, randomness.RNG)
    }

    /**
     * [MapParameters.rareFeaturesRichness] is the probability of spawning a rare feature
     */
    private fun spawnRareFeatures(tileMap: TileMap) {
        val rareFeatures = ruleset.terrains.values.filter {
            it.type == TerrainType.TerrainFeature && it.hasUnique(UniqueType.RareFeature)
        }
        for (tile in tileMap.values.asSequence().filter { it.terrainFeatures.isEmpty() }) {
            if (randomness.RNG.nextDouble() <= tileMap.mapParameters.rareFeaturesRichness) {
                val possibleFeatures = rareFeatures.filter { it.occursOn.contains(tile.baseTerrain)
                        && (!tile.isHill() || it.occursOn.contains(Constants.hill)) }
                if (possibleFeatures.any())
                    tile.addTerrainFeature(possibleFeatures.random(randomness.RNG).name)
            }
        }
    }

    /**
     * [MapParameters.temperatureExtremeness] as in [applyHumidityAndTemperature]
     */
    private fun spawnIce(tileMap: TileMap) {
        val waterTerrain: Set<String> =
            ruleset.terrains.values.asSequence()
            .filter { it.type == TerrainType.Water }
            .map { it.name }.toSet()
        val iceEquivalents: List<TerrainOccursRange> =
            ruleset.terrains.values.asSequence()
            .filter { terrain ->
                terrain.type == TerrainType.TerrainFeature &&
                terrain.impassable &&
                terrain.occursOn.all { it in waterTerrain }
            }.flatMap { terrain ->
                val conditions = terrain.getGenerationConditions()
                if (conditions.any()) conditions
                else sequenceOf(TerrainOccursRange(terrain,
                    -1f, ruleset.modOptions.constants.spawnIceBelowTemperature,
                    0f, 1f))
            }.toList()

        if (tileMap.mapParameters.shape === MapShape.flatEarth) {
            spawnFlatEarthIceWalls(tileMap, iceEquivalents)
        }

        if (iceEquivalents.isEmpty()) return

        tileMap.setTransients(ruleset)
        val temperatureSeed = randomness.RNG.nextInt().toDouble()
        for (tile in tileMap.values) {
            if (tile.baseTerrain !in waterTerrain || tile.terrainFeatures.isNotEmpty())
                continue

            val randomTemperature = randomness.getPerlinNoise(tile, temperatureSeed, scale = tileMap.mapParameters.tilesPerBiomeArea.toDouble(), nOctaves = 1)
            val latitudeTemperature = 1.0 - 2.0 * abs(tile.latitude) / tileMap.maxLatitude
            var temperature = ((latitudeTemperature + randomTemperature) / 2.0)
            temperature = abs(temperature).pow(1.0 - tileMap.mapParameters.temperatureExtremeness) * temperature.sign
            temperature = (temperature + tileMap.mapParameters.temperatureShift).coerceIn(-1.0..1.0)

            val candidates = iceEquivalents
                .filter {
                    it.matches(temperature, 1.0) &&
                    tile.getLastTerrain().name in it.terrain.occursOn
                }.map { it.terrain.name }
            when (candidates.size) {
                1 -> tile.addTerrainFeature(candidates.first())
                !in 0..1 -> tile.addTerrainFeature(candidates.random(randomness.RNG))
            }
        }
    }

    private fun spawnFlatEarthIceWalls(tileMap: TileMap, iceEquivalents: List<TerrainOccursRange>) {
        val iceCandidates = iceEquivalents.filter {
            it.matches(-1.0, 1.0)
        }.map {
            it.terrain.name
        }
        val iceTerrainName =
                when (iceCandidates.size) {
                    1 -> iceCandidates.first()
                    !in 0..1 -> iceCandidates.random(randomness.RNG)
                    else -> null
                }

        val snowCandidates = ruleset.terrains.values.asSequence().filter {
            it.type == TerrainType.Land
        }.flatMap { terrain ->
            val conditions = terrain.getGenerationConditions()
            if (conditions.any()) conditions
            else sequenceOf(
                TerrainOccursRange(
                    terrain,
                    -1f, ruleset.modOptions.constants.spawnIceBelowTemperature,
                    0f, 1f
                )
            )
        }.toList().filter {
            it.matches(-1.0, 1.0)
        }.map {
            it.terrain.name
        }
        val snowTerrainName =
                when (snowCandidates.size) {
                    1 -> snowCandidates.first()
                    !in 0..1 -> snowCandidates.random(randomness.RNG)
                    else -> null
                }

        val mountainTerrainName =
                ruleset.terrains.values.firstOrNull { it.hasUnique(UniqueType.OccursInChains) }?.name

        val bestArcticTileName = when {
            iceTerrainName != null -> iceTerrainName
            snowTerrainName != null -> snowTerrainName
            mountainTerrainName != null -> mountainTerrainName
            else -> null
        }

        val arcticTileNameList =
                arrayOf(iceTerrainName, snowTerrainName, mountainTerrainName).filterNotNull()

        // Skip the tile loop if nothing can be done in it
        if (bestArcticTileName == null && arcticTileNameList.isEmpty()) return

        // Flat Earth needs a 1 tile wide perimeter of ice/mountain/snow and a 2 radius cluster of ice in the center.
        for (tile in tileMap.values) {
            val isCenterTile = tile.latitude == 0f && tile.longitude == 0f
            val isEdgeTile = tile.neighbors.count() < 6

            // Make center tiles ice or snow or mountain depending on availability
            if (isCenterTile && bestArcticTileName != null) {
                spawnFlatEarthCenterIceWall(tile, bestArcticTileName, iceTerrainName, mountainTerrainName)
            }

            // Make edge tiles randomly ice or snow or mountain if available
            if (isEdgeTile && arcticTileNameList.isNotEmpty()) {
                spawnFlatEarthEdgeIceWall(tile, arcticTileNameList, iceTerrainName, mountainTerrainName)
            }
        }
    }

    private fun spawnFlatEarthCenterIceWall(tile: TileInfo, bestArcticTileName: String, iceTerrainName: String?, mountainTerrainName: String?) {
        // Spawn ice on center tile
        if (bestArcticTileName == iceTerrainName) {
            tile.baseTerrain = waterTerrainName
            tile.setTerrainFeatures(listOf(iceTerrainName))
        } else if (iceTerrainName != null && bestArcticTileName != mountainTerrainName) {
            tile.baseTerrain = bestArcticTileName // snow (or a mod's equivalent)
            tile.setTerrainFeatures(listOf(iceTerrainName))
        } else {
            tile.baseTerrain = bestArcticTileName // mountain (or a mod's equivalent)
            tile.removeTerrainFeatures()
        }

        // Spawn circle of ice around center tile
        for (neighbor in tile.neighbors) {
            if (bestArcticTileName == iceTerrainName) {
                neighbor.baseTerrain = waterTerrainName
                neighbor.setTerrainFeatures(listOf(iceTerrainName))
            } else if (iceTerrainName != null && bestArcticTileName != mountainTerrainName) {
                neighbor.baseTerrain = bestArcticTileName // snow (or a mod's equivalent)
                neighbor.setTerrainFeatures(listOf(iceTerrainName))
            } else {
                neighbor.baseTerrain = bestArcticTileName // mountain (or a mod's equivalent)
                neighbor.removeTerrainFeatures()
            }

            // Spawn partial circle of ice around circle of ice
            for (neighbor2 in neighbor.neighbors) {
                if (randomness.RNG.nextDouble() < 0.75) {
                    // Do nothing most of the time at random.
                } else if (bestArcticTileName == iceTerrainName) {
                    neighbor2.baseTerrain = waterTerrainName
                    neighbor2.setTerrainFeatures(listOf(iceTerrainName))
                } else if (iceTerrainName != null && bestArcticTileName != mountainTerrainName) {
                    neighbor2.baseTerrain = bestArcticTileName // snow (or a mod's equivalent)
                    neighbor2.setTerrainFeatures(listOf(iceTerrainName))
                } else {
                    neighbor2.baseTerrain = bestArcticTileName // mountain (or a mod's equivalent)
                    neighbor2.removeTerrainFeatures()
                }
            }
        }
    }

    private fun spawnFlatEarthEdgeIceWall(tile: TileInfo, arcticTileNameList: List<String>, iceTerrainName: String?, mountainTerrainName: String?) {
        // Select one of the arctic tiles at random
        val arcticTileName = when (arcticTileNameList.size) {
            1 -> arcticTileNameList.first()
            else -> arcticTileNameList.random(randomness.RNG)
        }

        // Spawn arctic tiles on edge tile
        if (arcticTileName == iceTerrainName) {
            tile.baseTerrain = waterTerrainName
            tile.setTerrainFeatures(listOf(iceTerrainName))
        } else if (iceTerrainName != null && arcticTileName != mountainTerrainName) {
            tile.baseTerrain = arcticTileName // snow (or a mod's equivalent)
            tile.setTerrainFeatures(listOf(iceTerrainName))
        } else {
            tile.baseTerrain = arcticTileName // mountain (or a mod's equivalent)
            tile.removeTerrainFeatures()
        }

        // Spawn partial circle of arctic tiles next to the edge
        for (neighbor in tile.neighbors) {
            val neighborIsEdgeTile = neighbor.neighbors.count() < 6
            if (neighborIsEdgeTile) {
                // Do not redo edge tile. It is already done.
            } else if (randomness.RNG.nextDouble() < 0.75) {
                // Do nothing most of the time at random.
            } else if (arcticTileName == iceTerrainName) {
                neighbor.baseTerrain = waterTerrainName
                neighbor.setTerrainFeatures(listOf(iceTerrainName))
            } else if (iceTerrainName != null && arcticTileName != mountainTerrainName) {
                neighbor.baseTerrain = arcticTileName // snow (or a mod's equivalent)
                neighbor.setTerrainFeatures(listOf(iceTerrainName))
            } else {
                neighbor.baseTerrain = arcticTileName // mountain (or a mod's equivalent)
                neighbor.removeTerrainFeatures()
            }
        }
    }
}

class MapGenerationRandomness {
    var RNG = Random(42)

    fun seedRNG(seed: Long = 42) {
        RNG = Random(seed)
    }

    /**
     * Generates a perlin noise channel combining multiple octaves
     * Default settings generate mostly within [-0.55, 0.55], but clustered around 0.0
     * About 28% are < -0.1 and 28% are > 0.1
     *
     * @param tile Source for x / x coordinates.
     * @param seed Misnomer: actually the z value the Perlin cloud is 'cut' on.
     * @param nOctaves is the number of octaves.
     * @param persistence is the scaling factor of octave amplitudes.
     * @param lacunarity is the scaling factor of octave frequencies.
     * @param scale is the distance the noise is observed from.
     */
    fun getPerlinNoise(
        tile: TileInfo,
        seed: Double,
        nOctaves: Int = 6,
        persistence: Double = 0.5,
        lacunarity: Double = 2.0,
        scale: Double = 10.0
    ): Double {
        val worldCoords = HexMath.hex2WorldCoords(tile.position)
        return Perlin.noise3d(worldCoords.x.toDouble(), worldCoords.y.toDouble(), seed, nOctaves, persistence, lacunarity, scale)
    }


    fun chooseSpreadOutLocations(number: Int, suitableTiles: List<TileInfo>, mapRadius: Int): ArrayList<TileInfo> {
        if (number <= 0) return ArrayList(0)

        // Determine sensible initial distance from number of desired placements and mapRadius
        // empiric formula comes very close to eliminating retries for distance.
        // The `if` means if we need to fill 60% or more of the available tiles, no sense starting with minimum distance 2.
        val sparsityFactor = (HexMath.getHexagonalRadiusForArea(suitableTiles.size) / mapRadius).pow(0.333f)
        val initialDistance = if (number == 1 || number * 5 >= suitableTiles.size * 3) 1
            else max(1, (mapRadius * 0.666f / HexMath.getHexagonalRadiusForArea(number).pow(0.9f) * sparsityFactor + 0.5).toInt())

        // If possible, we want to equalize the base terrains upon which
        //  the resources are found, so we save how many have been
        //  found for each base terrain and try to get one from the lowest
        val baseTerrainsToChosenTiles = HashMap<String, Int>()
        for (tileInfo in suitableTiles){
            if (tileInfo.baseTerrain !in baseTerrainsToChosenTiles)
                baseTerrainsToChosenTiles[tileInfo.baseTerrain] = 0
        }

        for (distanceBetweenResources in initialDistance downTo 1) {
            var availableTiles = suitableTiles
            val chosenTiles = ArrayList<TileInfo>(number)

            for (terrain in baseTerrainsToChosenTiles.keys)
                baseTerrainsToChosenTiles[terrain] = 0

            for (i in 1..number) {
                if (availableTiles.isEmpty()) break
                val orderedKeys = baseTerrainsToChosenTiles.entries
                        .sortedBy { it.value }.map { it.key }
                val firstKeyWithTilesLeft = orderedKeys
                        .first { availableTiles.any { tile -> tile.baseTerrain == it} }
                val chosenTile = availableTiles.filter { it.baseTerrain == firstKeyWithTilesLeft }.random(RNG)
                availableTiles = availableTiles.filter { it.aerialDistanceTo(chosenTile) > distanceBetweenResources }
                chosenTiles.add(chosenTile)
                baseTerrainsToChosenTiles[firstKeyWithTilesLeft] = baseTerrainsToChosenTiles[firstKeyWithTilesLeft]!! + 1
            }
            if (chosenTiles.size == number || distanceBetweenResources == 1) {
                // Either we got them all, or we're not going to get anything better
                if (Log.shouldLog() && distanceBetweenResources < initialDistance)
                    debug("chooseSpreadOutLocations: distance $distanceBetweenResources < initial $initialDistance")
                return chosenTiles
            }
        }
        // unreachable due to last loop iteration always returning and initialDistance >= 1
        throw Exception()
    }
}
