package com.unciv.pure.application.mapgeneration

import com.unciv.pure.domain.mapgeneration.*
import kotlin.math.abs
import kotlin.random.Random

/** Геометрия и слои одной клетки экспериментального снимка. Индексы соседей относятся к списку cells. */
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

/** Снимок фактических правил; порядок климатических условий и кандидатов растительности значим. */
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

/** Режим определяет входной RNG-контекст: отдельный этап не имитирует расход предыдущих этапов. */
enum class TerrainGenerationMode { ALL, ELEVATION, CLIMATE, VEGETATION }

/** Настройки запуска не читают часы; seed 0 — обычный воспроизводимый seed. */
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
    val mode: TerrainGenerationMode = TerrainGenerationMode.ALL
)

/** Вход сохраняет всю геометрию и маску воды, а не только название шаблона карты. */
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

/** Результат содержит собственный исходный снимок для точного повторения эксперимента. */
data class TerrainExperimentResult(
    val input: TerrainExperimentInput,
    val cells: List<TerrainSurfaceCell>,
    val algorithmVersion: String = TerrainGenerationPipeline.ALGORITHM_VERSION
)

/** Координирует общие доменные генераторы без TileMap, Gdx, файлов или глобального состояния. */
object TerrainGenerationPipeline {
    const val ALGORITHM_VERSION = "homogenous-terrain-1"

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
            val climate = ClimateGenerator.generate(cells.map {
                ClimateCell(it.x, it.y, it.latitude, it.longitude, it.baseTerrain,
                    it.baseTerrain in rules.waterTerrains || it.baseTerrain in rules.climateExcludedTerrains,
                    it.temperature, it.humidity)
            }, ClimateSettings(settings.climateScale, settings.temperatureExtremeness, settings.temperatureShift,
                settings.flatEarth, cells.maxOf { abs(it.latitude) }, cells.maxOf { abs(it.longitude) }),
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
        return TerrainExperimentResult(input, cells.map { it.copy(features = it.features.toList(), neighbors = it.neighbors.toList()) })
    }

    /** Согласованность всего снимка проверяется до первого вычисления и расхода RNG. */
    fun validate(input: TerrainExperimentInput) {
        val cells = input.cells
        val rules = input.rules
        val settings = input.settings
        require(cells.isNotEmpty()) { "Terrain snapshot must contain cells" }
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
        // В экспериментальном режиме вырожденная геометрия отклоняется явно; игровой legacy-путь не меняется.
        if (settings.mode == TerrainGenerationMode.ALL || settings.mode == TerrainGenerationMode.CLIMATE) {
            require(cells.maxOf { abs(it.latitude) } > 0f) { "Climate requires a nonzero latitude extent" }
            if (settings.flatEarth) require(cells.maxOf { abs(it.longitude) } > 0f) { "Flat Earth requires a nonzero longitude extent" }
        }
    }
}
