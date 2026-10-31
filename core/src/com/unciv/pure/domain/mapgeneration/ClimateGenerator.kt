package com.unciv.pure.domain.mapgeneration

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.ulp

/** Упорядоченное условие террейна: нижняя граница открыта, верхняя закрыта. */
data class ClimateTerrainRange(
    val terrain: String,
    val temperatureFrom: Float,
    val temperatureTo: Float,
    val humidityFrom: Float,
    val humidityTo: Float
) {
    fun matches(temperature: Double, humidity: Double): Boolean =
        temperatureFrom < temperature && temperature <= temperatureTo &&
            humidityFrom < humidity && humidity <= humidityTo

    companion object {
        /** Минимумы -1 и 0 включаются тем же сдвигом Float.ulp, что и в историческом генераторе. */
        fun inclusiveMinimums(terrain: String, t0: Float, t1: Float, h0: Float, h1: Float) =
            ClimateTerrainRange(terrain, if (t0 == -1f) t0 - t0.ulp else t0, t1,
                if (h0 == 0f) h0 - h0.ulp else h0, h1)
    }
}

/** Числовые настройки климата. Нулевые геометрические максимумы сохраняют историческую арифметику. */
data class ClimateSettings(
    val scale: Double = 6.0,
    val extremeness: Float = 0.6f,
    val shift: Float = 0f,
    val flatEarth: Boolean = false,
    val maxLatitude: Float,
    val maxLongitude: Float
) {
    init {
        require(scale.isFinite() && scale > 0.0) { "Climate scale must be positive and finite" }
        require(extremeness.isFinite() && shift.isFinite()) { "Climate parameters must be finite" }
    }
}

/** Снимок клетки; координаты шума уже округлены адаптером до исторической точности Float. */
data class ClimateCell(
    val x: Double,
    val y: Double,
    val latitude: Float,
    val longitude: Float,
    val baseTerrain: String,
    val excluded: Boolean = false,
    val temperature: Double? = null,
    val humidity: Double? = null
)

/** Результат с признаком обработки: исключённые клетки адаптер не переписывает. */
data class ClimateResult(
    val baseTerrain: String,
    val temperature: Double?,
    val humidity: Double?,
    val processed: Boolean
)

/** Общие климатические формулы игры и экспериментов; входные снимки не изменяются. */
object ClimateGenerator {
    fun generate(
        cells: List<ClimateCell>,
        settings: ClimateSettings,
        ranges: List<ClimateTerrainRange>,
        availableTerrains: Set<String>,
        firstLandTerrain: String,
        humiditySeed: Double,
        temperatureSeed: Double
    ): List<ClimateResult> {
        require(firstLandTerrain in availableTerrains) { "Missing fallback terrain: $firstLandTerrain" }
        require(ranges.all { it.terrain in availableTerrains }) { "Unknown terrain in climate rules" }
        val humidityShift = if (settings.shift > 0) -settings.shift / 2 else 0f
        return cells.map { cell ->
            if (cell.excluded) return@map ClimateResult(cell.baseTerrain, cell.temperature, cell.humidity, false)
            val humidityRandom = PerlinNoise.noise3d(cell.x, cell.y, humiditySeed, nOctaves = 1, scale = settings.scale)
            val humidity = ((humidityRandom + 1.0) / 2.0 + humidityShift).coerceIn(0.0..1.0)
            val expectedTemperature = if (settings.flatEarth) {
                val latitudeRatio = abs(cell.latitude) / settings.maxLatitude
                val longitudeRatio = abs(cell.longitude) / settings.maxLongitude
                val radius = sqrt(latitudeRatio.pow(2) + longitudeRatio.pow(2))
                temperatureAtRadius(radius)
            } else 1.0 - 2.0 * abs(cell.latitude) / settings.maxLatitude
            val randomTemperature = PerlinNoise.noise3d(cell.x, cell.y, temperatureSeed, nOctaves = 1, scale = settings.scale)
            var temperature = (5.0 * expectedTemperature + randomTemperature) / 6.0
            temperature = abs(temperature).pow(1.0 - settings.extremeness) * temperature.sign
            temperature = (temperature + settings.shift).coerceIn(-1.0..1.0)
            if (ranges.isEmpty()) {
                // Исторический путь не публикует вычисленные климатические поля.
                val terrain = when {
                    temperature < -0.4 -> if (humidity < 0.5) "Snow" else "Tundra"
                    temperature < 0.8 -> if (humidity < 0.5) "Plains" else "Grassland"
                    temperature <= 1.0 -> if (humidity < 0.7) "Desert" else "Plains"
                    else -> firstLandTerrain
                }
                ClimateResult(if (terrain in availableTerrains) terrain else cell.baseTerrain,
                    cell.temperature, cell.humidity, true)
            } else ClimateResult(selectTerrain(temperature, humidity, ranges, firstLandTerrain), temperature, humidity, true)
        }
    }

    /** Порядок правил значим: пересечения разрешаются первым совпадением. */
    fun selectTerrain(temperature: Double, humidity: Double, ranges: List<ClimateTerrainRange>, fallback: String): String =
        ranges.firstOrNull { it.matches(temperature, humidity) }?.terrain ?: fallback

    /** Радиальная модель Flat Earth сохраняет смещения ледяных границ 0.05 и 0.95. */
    fun temperatureAtRadius(radius: Float): Double = when {
        radius < 0.5 -> scaleToRange(0.05, 0.5, -1.0, 1.0, radius)
        radius > 0.5 -> scaleToRange(0.5, 0.95, 1.0, -1.0, radius)
        else -> 1.0
    }

    private fun scaleToRange(x1: Double, x2: Double, y1: Double, y2: Double, value: Float): Double {
        val gain = (y2 - y1) / (x2 - x1)
        val offset = y2 - gain * x2
        return gain * value + offset
    }
}
