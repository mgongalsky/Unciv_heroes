package com.unciv.logic.map

import com.unciv.logic.HexMath.getEquivalentHexagonalRadius
import com.unciv.logic.HexMath.getEquivalentRectangularSize
import com.unciv.logic.HexMath.getNumberOfTilesInHexagon
import com.unciv.logic.IsPartOfGameInfoSerialization
import com.unciv.models.metadata.BaseRuleset

enum class MapSize(val radius: Int, val width: Int, val height: Int) {
    Tiny(10, 23, 15),
    Small(15, 33, 21),
    Medium(20, 44, 29),
    Large(30, 66, 43),
    Huge(40, 87, 57);

    companion object {
        const val custom = "Custom"
    }
}

class MapSizeNew : IsPartOfGameInfoSerialization {
    var radius = 0
    var width = 0
    var height = 0
    var name = ""

    @Suppress("unused")
    constructor()

    private fun fromPredefined(predefined: MapSize) {
        name = predefined.name
        radius = predefined.radius
        width = predefined.width
        height = predefined.height
    }

    constructor(size: MapSize) {
        fromPredefined(size)
    }

    constructor(name: String) {
        try {
            fromPredefined(MapSize.valueOf(name))
        } catch (_: Exception) {
            fromPredefined(MapSize.Tiny)
        }
    }

    constructor(radius: Int) {
        name = MapSize.custom
        setNewRadius(radius)
    }

    constructor(width: Int, height: Int) {
        name = MapSize.custom
        this.width = width
        this.height = height
        radius = getEquivalentHexagonalRadius(width, height)
    }

    fun clone() = MapSizeNew().also {
        it.name = name
        it.radius = radius
        it.width = width
        it.height = height
    }

    fun fixUndesiredSizes(worldWrap: Boolean): String? {
        if (name != MapSize.custom) return null
        if (worldWrap && width % 2 != 0) width--
        val message = when {
            worldWrap && width < 32 -> "World wrap requires a minimum width of 32 tiles"
            width < 3 || height < 3 || radius < 2 -> "The provided map dimensions were too small"
            radius > 500 -> "The provided map dimensions were too big"
            height * 16 < width || width * 16 < height -> "The provided map dimensions had an unacceptable aspect ratio"
            else -> null
        } ?: return null
        setNewRadius(
            when {
                radius < 2 -> 2
                radius > 500 -> 500
                worldWrap && radius < 15 -> 15
                else -> radius
            }
        )
        return message
    }

    private fun setNewRadius(radius: Int) {
        this.radius = radius
        val size = getEquivalentRectangularSize(radius)
        width = size.x.toInt()
        height = size.y.toInt()
    }

    override fun toString() = if (name == MapSize.custom) "${width}x$height" else name
}

object MapShape : IsPartOfGameInfoSerialization {
    const val hexagonal = "Hexagonal"
    const val flatEarth = "Flat Earth Hexagonal"
    const val rectangular = "Rectangular"
}

object MapType : IsPartOfGameInfoSerialization {
    const val default = "Default"
    const val pangaea = "Pangaea"
    const val continentAndIslands = "Continent and Islands"
    const val twoContinents = "Two Continents"
    const val threeContinents = "Three Continents"
    const val fourCorners = "Four Corners"
    const val archipelago = "Archipelago"
    const val innerSea = "Inner Sea"
    const val smoothedRandom = "Smoothed Random"
    const val custom = "Custom"
    const val empty = "Empty"
}

object MapResources {
    const val sparse = "Sparse"
    const val default = "Default"
    const val abundant = "Abundant"
    const val strategicBalance = "Strategic Balance"
    const val legendaryStart = "Legendary Start"
}

class MapParameters : IsPartOfGameInfoSerialization {
    /** Serialization contract version, independent from the game release version. */
    var mapFormatVersion = 0
    var name = ""
    var type = MapType.pangaea
    var shape = MapShape.hexagonal
    var mapSize = MapSizeNew(MapSize.Medium)
    var mapResources = MapResources.default
    var noRuins = false
    var noNaturalWonders = false
    var worldWrap = false
    var mods = LinkedHashSet<String>()
    var baseRuleset = BaseRuleset.Civ_V_GnK.fullName
    var createdWithVersion = ""
    var seed: Long = System.currentTimeMillis()
    var tilesPerBiomeArea = 6
    var maxCoastExtension = 2
    var elevationExponent = 0.7f
    var temperatureExtremeness = 0.6f
    var vegetationRichness = 0.4f
    var rareFeaturesRichness = 0.05f
    var resourceRichness = 0.1f
    var waterThreshold = 0.0f
    var temperatureShift = 0f

    fun clone() = MapParameters().also {
        it.mapFormatVersion = mapFormatVersion
        it.name = name
        it.type = type
        it.shape = shape
        it.mapSize = mapSize.clone()
        it.mapResources = mapResources
        it.noRuins = noRuins
        it.noNaturalWonders = noNaturalWonders
        it.worldWrap = worldWrap
        it.mods = LinkedHashSet(mods)
        it.baseRuleset = baseRuleset
        it.seed = seed
        it.tilesPerBiomeArea = tilesPerBiomeArea
        it.maxCoastExtension = maxCoastExtension
        it.elevationExponent = elevationExponent
        it.temperatureExtremeness = temperatureExtremeness
        it.temperatureShift = temperatureShift
        it.vegetationRichness = vegetationRichness
        it.rareFeaturesRichness = rareFeaturesRichness
        it.resourceRichness = resourceRichness
        it.waterThreshold = waterThreshold
        it.createdWithVersion = createdWithVersion
    }

    fun reseed() {
        seed = System.currentTimeMillis()
    }

    fun resetAdvancedSettings() {
        reseed()
        tilesPerBiomeArea = 6
        maxCoastExtension = 2
        elevationExponent = 0.7f
        temperatureExtremeness = 0.6f
        temperatureShift = 0f
        vegetationRichness = 0.4f
        rareFeaturesRichness = 0.05f
        resourceRichness = 0.1f
        waterThreshold = if (type == MapType.smoothedRandom) -0.05f else 0f
    }

    fun getArea() = when {
        shape == MapShape.hexagonal || shape == MapShape.flatEarth -> getNumberOfTilesInHexagon(
            mapSize.radius
        )

        worldWrap && mapSize.width % 2 != 0 -> (mapSize.width - 1) * mapSize.height
        else -> mapSize.width * mapSize.height
    }

    fun displayMapDimensions() = mapSize.run {
        (if (shape == MapShape.hexagonal || shape == MapShape.flatEarth) "R$radius" else "${width}x$height") +
                (if (worldWrap) "w" else "")
    }

    private fun Float.niceToString(maxPrecision: Int) =
            "%.${maxPrecision}f".format(this).trimEnd('0').trimEnd('.')

    override fun toString() = sequence {
        if (name.isNotEmpty()) yield("\"$name\" ")
        yield("(")
        if (mapSize.name != MapSize.custom) yield("{${mapSize.name}} ")
        if (worldWrap) yield("{World Wrap} ")
        yield("{$shape")
        yield(" " + displayMapDimensions() + ")")
        if (mapResources != MapResources.default) yield(" {Resource Setting}: {$mapResources")
        if (name.isEmpty()) return@sequence
        yield("\n")
        if (type != MapType.custom && type != MapType.empty) yield("{Map Generation Type}: {$type}, ")
        yield("{RNG Seed} $seed")
        yield(", {Map Elevation}=" + elevationExponent.niceToString(2))
        yield(", {Temperature extremeness}=" + temperatureExtremeness.niceToString(2))
        yield(", {Resource richness}=" + resourceRichness.niceToString(3))
        yield(", {Vegetation richness}=" + vegetationRichness.niceToString(2))
        yield(", {Rare features richness}=" + rareFeaturesRichness.niceToString(3))
        yield(", {Max Coast extension}=$maxCoastExtension")
        yield(", {Biome areas extension}=$tilesPerBiomeArea")
        yield(", {Water level}=" + waterThreshold.niceToString(2))
    }.joinToString("")

    fun numberOfTiles() = if (shape == MapShape.hexagonal || shape == MapShape.flatEarth)
        1 + 3 * mapSize.radius * (mapSize.radius - 1)
    else mapSize.width * mapSize.height
}
