package com.unciv.pure.application.mapgeneration

import com.unciv.pure.domain.mapgeneration.*
import kotlin.math.abs
import kotlin.random.Random

/** Геометрия и слои клетки; индексы соседей относятся к списку cells. */
data class TerrainSurfaceCell(
    val column: Int, val row: Int,
    val x: Double, val y: Double,
    val latitude: Float, val longitude: Float,
    val neighbors: List<Int>,
    val baseTerrain: String,
    val features: List<String> = emptyList(),
    val temperature: Double? = null,
    val humidity: Double? = null
)

/** Снимок правил; порядок климатических условий и кандидатов растительности значим. */
data class TerrainRuleSnapshot(
    val terrains: Set<String>,
    val waterTerrains: Set<String>,
    val impassableTerrains: Set<String>,
    val firstLandTerrain: String,
    val flatTerrain: String?,
    val mountainTerrain: String?,
    val hillTerrain: String?,
    val climateExcludedTerrains: Set<String>,
    val climateRanges: List<ClimateTerrainRange>,
    val vegetation: List<VegetationRule>,
    val risingMarker: String = "terrain-rising",
    val loweringMarker: String = "terrain-lowering"
)

/** Отдельный этап не имитирует расход RNG предыдущих этапов. */
enum class TerrainGenerationMode { ALL, ELEVATION, CLIMATE, VEGETATION }

/** Явный seed; null диапазона сохраняет историческую климатическую модель. */
data class TerrainExperimentSettings(
    val seed: Long = 0L,
    val climateScale: Double = 6.0,
    val vegetationScale: Double = 3.0,
    val elevationScale: Double = 2.0,
    val elevationExponent: Float = 0.7f,
    val temperatureExtremeness: Float = 0.6f,
    val temperatureShift: Float = 0f,
    val vegetationRichness: Float = 0.4f,
    val flatEarth: Boolean = false,
    val mode: TerrainGenerationMode = TerrainGenerationMode.ALL,
    val latitudeRange: LatitudeRange? = null
)

/** Вход сохраняет всю геометрию и исходную маску воды. */
data class TerrainExperimentInput(
    val cells: List<TerrainSurfaceCell>,
    val rules: TerrainRuleSnapshot,
    val settings: TerrainExperimentSettings
) {
    /** Копирует вложенные коллекции до передачи снимка в рабочий поток. */
    fun snapshot() = copy(
        cells = cells.map { it.copy(neighbors = it.neighbors.toList(), features = it.features.toList()) },
        rules = rules.copy(
            terrains = rules.terrains.toSet(), waterTerrains = rules.waterTerrains.toSet(),
            impassableTerrains = rules.impassableTerrains.toSet(),
            climateExcludedTerrains = rules.climateExcludedTerrains.toSet(),
            climateRanges = rules.climateRanges.toList(),
            vegetation = rules.vegetation.map { it.copy(occursOn = it.occursOn.toList()) }
        )
    )
}

/** Результат содержит собственный исходный снимок для точного повторения. */
data class TerrainExperimentResult(
    val input: TerrainExperimentInput,
    val cells: List<TerrainSurfaceCell>,
    val algorithmVersion: String = TerrainGenerationPipeline.ALGORITHM_VERSION
)

/** Координирует общие доменные генераторы без движка, файлов и глобального состояния. */
object TerrainGenerationPipeline {
    const val ALGORITHM_VERSION = "homogenous-terrain-1"
    /** Новый режим имеет отдельную версию; старые отчёты сохраняют прежнюю. */
    const val LATITUDE_ALGORITHM_VERSION = "homogenous-terrain-latitude-1"

    fun generate(source: TerrainExperimentInput): TerrainExperimentResult {
        val input = source.snapshot()
        validate(input)
        val settings = input.settings
        val rules = input.rules
        val random = Random(settings.seed)
        var cells = input.cells
        if (settings.mode == TerrainGenerationMode.ALL || settings.mode == TerrainGenerationMode.ELEVATION) {
            val elevation = ElevationGenerator.generate(cells.map {
                ElevationCell(it.x, it.y, it.baseTerrain, it.features, it.baseTerrain in rules.waterTerrains, it.neighbors)
            }, ElevationRules(rules.flatTerrain, rules.mountainTerrain, rules.hillTerrain,
                rules.impassableTerrains, rules.waterTerrains, rules.risingMarker, rules.loweringMarker),
                settings.elevationExponent, random, settings.elevationScale)
            cells = cells.mapIndexed { index, cell ->
                cell.copy(baseTerrain = elevation[index].baseTerrain, features = elevation[index].features)
            }
        }
        if (settings.mode == TerrainGenerationMode.ALL || settings.mode == TerrainGenerationMode.CLIMATE) {
            val humiditySeed = random.nextInt().toDouble()
            val temperatureSeed = random.nextInt().toDouble()
            val range = settings.latitudeRange
            val minY = cells.minOf { it.y }
            val maxY = cells.maxOf { it.y }
            val climate = ClimateGenerator.generate(cells.map {
                // Проекция меняет только фон температуры, не геометрию и координаты шума.
                val latitude = range?.at(it.y, minY, maxY)?.toFloat() ?: it.latitude
                ClimateCell(it.x, it.y, latitude, it.longitude, it.baseTerrain,
                    it.baseTerrain in rules.waterTerrains || it.baseTerrain in rules.climateExcludedTerrains,
                    it.temperature, it.humidity)
            }, ClimateSettings(settings.climateScale, settings.temperatureExtremeness, settings.temperatureShift,
                settings.flatEarth, if (range == null) cells.maxOf { abs(it.latitude) } else 90f,
                cells.maxOf { abs(it.longitude) }),
                rules.climateRanges, rules.terrains, rules.firstLandTerrain, humiditySeed, temperatureSeed)
            cells = cells.mapIndexed { index, cell ->
                val result = climate[index]
                cell.copy(baseTerrain = result.baseTerrain, temperature = result.temperature, humidity = result.humidity)
            }
        }
        if (settings.mode == TerrainGenerationMode.ALL || settings.mode == TerrainGenerationMode.VEGETATION) {
            val vegetation = VegetationGenerator.generate(cells.map {
                VegetationCell(it.x, it.y, it.baseTerrain, it.features.lastOrNull() ?: it.baseTerrain)
            }, rules.vegetation, settings.vegetationRichness, random, settings.vegetationScale)
            cells = cells.mapIndexed { index, cell ->
                val addition = vegetation[index]
                if (addition == null) cell else cell.copy(features = cell.features + addition)
            }
        }
        return TerrainExperimentResult(input,
            cells.map { it.copy(features = it.features.toList(), neighbors = it.neighbors.toList()) },
            if (settings.latitudeRange == null) ALGORITHM_VERSION else LATITUDE_ALGORITHM_VERSION)
    }

    /** Проверяет весь снимок до первого вычисления и расхода RNG. */
    fun validate(input: TerrainExperimentInput) {
        val cells = input.cells
        val rules = input.rules
        val settings = input.settings
        require(cells.isNotEmpty()) { "Terrain snapshot must contain cells" }
        require(!(settings.flatEarth && settings.latitudeRange != null)) {
            "Latitude range cannot be combined with radial Flat Earth climate"
        }
        require(listOf(settings.climateScale, settings.vegetationScale, settings.elevationScale)
            .all { it.isFinite() && it > 0.0 }) { "All terrain scales must be positive and finite" }
        require(listOf(settings.elevationExponent, settings.temperatureExtremeness,
            settings.temperatureShift, settings.vegetationRichness).all { it.isFinite() }) { "Terrain parameters must be finite" }
        require(cells.map { it.column to it.row }.toSet().size == cells.size) { "Duplicate cell coordinates" }
        require(cells.all { it.x.isFinite() && it.y.isFinite() && it.latitude.isFinite() && it.longitude.isFinite() }) {
            "Non-finite terrain coordinates"
        }
        require(cells.all { it.temperature == null || it.temperature.isFinite() }) { "Non-finite snapshot temperature" }
        require(cells.all { it.humidity == null || it.humidity.isFinite() }) { "Non-finite snapshot humidity" }
        require(cells.all { it.baseTerrain in rules.terrains && it.features.all { feature -> feature in rules.terrains } }) {
            "Unknown snapshot terrain"
        }
        require(rules.firstLandTerrain in rules.terrains && rules.firstLandTerrain !in rules.waterTerrains) { "Invalid land fallback" }
        require(listOfNotNull(rules.flatTerrain, rules.mountainTerrain, rules.hillTerrain).all { it in rules.terrains }) { "Unknown elevation terrain" }
        require((rules.waterTerrains + rules.impassableTerrains).all { it in rules.terrains }) { "Unknown terrain mask entry" }
        require(rules.climateRanges.all { it.terrain in rules.terrains }) { "Unknown climate terrain" }
        require(rules.vegetation.all { it.terrain in rules.terrains && it.occursOn.all { base -> base in rules.terrains } }) {
            "Unknown vegetation terrain"
        }
        for ((index, cell) in cells.withIndex()) {
            require(cell.neighbors.distinct().size == cell.neighbors.size && index !in cell.neighbors) { "Duplicate or self neighbor" }
            require(cell.neighbors.all { it in cells.indices }) { "Neighbor outside snapshot" }
            require(cell.neighbors.all { index in cells[it].neighbors }) { "Asymmetric terrain neighborhood" }
        }
        // Явный диапазон допускает одну строку; legacy сохраняет прежние ограничения.
        if ((settings.mode == TerrainGenerationMode.ALL || settings.mode == TerrainGenerationMode.CLIMATE)
            && settings.latitudeRange == null) {
            require(cells.maxOf { abs(it.latitude) } > 0f) { "Climate requires a nonzero latitude extent" }
            if (settings.flatEarth) require(cells.maxOf { abs(it.longitude) } > 0f) { "Flat Earth requires a nonzero longitude extent" }
        }
    }
}
