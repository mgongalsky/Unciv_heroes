package com.unciv.app.desktop.terrain

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import java.io.File
import java.security.MessageDigest

/** Общий файловый адаптер CLI и UI. JSON содержит полный снимок и не требует загрузки игрового мира. */
object TerrainExperimentFiles {
    fun writeInput(file: File, input: TerrainExperimentInput) {
        val snapshot = input.snapshot()
        TerrainGenerationPipeline.validate(snapshot)
        write(file, inputJson(snapshot))
    }

    /** Читает конфигурацию или вход из отчёта; отчёт дополнительно проверяется повторным вычислением. */
    fun readInput(file: File): TerrainExperimentInput {
        val root = JsonReader().parse(file.readText(Charsets.UTF_8))
        return if (root.has("output")) readResult(file).input else decodeInput(root).also { TerrainGenerationPipeline.validate(it) }
    }

    fun writeResult(file: File, result: TerrainExperimentResult) {
        require(result.algorithmVersion == TerrainGenerationPipeline.ALGORITHM_VERSION) { "Unsupported terrain algorithm" }
        val snapshot = result.input.snapshot()
        TerrainGenerationPipeline.validate(snapshot)
        val input = inputJson(snapshot)
        val root = obj()
        root.addChild("formatVersion", number(1))
        root.addChild("algorithmVersion", string(result.algorithmVersion))
        root.addChild("inputSha256", string(sha256(text(input))))
        root.addChild("input", input)
        root.addChild("output", cellsJson(result.cells))
        write(file, root)
    }

    /** Несовпадение версии, контрольной суммы или результата не маскируется новым успешным запуском. */
    fun readResult(file: File): TerrainExperimentResult {
        val root = JsonReader().parse(file.readText(Charsets.UTF_8))
        require(root.getInt("formatVersion") == 1) { "Unsupported terrain report format" }
        require(root.getString("algorithmVersion") == TerrainGenerationPipeline.ALGORITHM_VERSION) { "Unsupported terrain algorithm" }
        val input = decodeInput(root.get("input"))
        require(root.getString("inputSha256") == sha256(text(inputJson(input)))) { "Terrain input checksum mismatch" }
        val output = decodeCells(root.get("output"))
        val regenerated = TerrainGenerationPipeline.generate(input)
        require(regenerated.cells == output) { "Terrain report does not reproduce its stored output" }
        return regenerated
    }

    private fun inputJson(input: TerrainExperimentInput): JsonValue {
        val root = obj()
        root.addChild("formatVersion", number(1))
        root.addChild("algorithmVersion", string(TerrainGenerationPipeline.ALGORITHM_VERSION))
        val settings = input.settings
        val config = obj()
        config.addChild("seed", JsonValue(settings.seed))
        config.addChild("climateScale", JsonValue(settings.climateScale))
        config.addChild("vegetationScale", JsonValue(settings.vegetationScale))
        config.addChild("elevationScale", JsonValue(settings.elevationScale))
        config.addChild("elevationExponent", JsonValue(settings.elevationExponent.toDouble()))
        config.addChild("temperatureExtremeness", JsonValue(settings.temperatureExtremeness.toDouble()))
        config.addChild("temperatureShift", JsonValue(settings.temperatureShift.toDouble()))
        config.addChild("vegetationRichness", JsonValue(settings.vegetationRichness.toDouble()))
        config.addChild("flatEarth", JsonValue(settings.flatEarth))
        config.addChild("mode", string(settings.mode.name))
        root.addChild("settings", config)
        val rules = input.rules
        val ruleJson = obj()
        ruleJson.addChild("terrains", strings(rules.terrains.sorted()))
        ruleJson.addChild("waterTerrains", strings(rules.waterTerrains.sorted()))
        ruleJson.addChild("impassableTerrains", strings(rules.impassableTerrains.sorted()))
        ruleJson.addChild("climateExcludedTerrains", strings(rules.climateExcludedTerrains.sorted()))
        ruleJson.addChild("firstLandTerrain", string(rules.firstLandTerrain))
        ruleJson.addChild("flatTerrain", string(rules.flatTerrain))
        ruleJson.addChild("mountainTerrain", string(rules.mountainTerrain))
        ruleJson.addChild("hillTerrain", string(rules.hillTerrain))
        ruleJson.addChild("risingMarker", string(rules.risingMarker))
        ruleJson.addChild("loweringMarker", string(rules.loweringMarker))
        ruleJson.addChild("climateRanges", array(rules.climateRanges.map {
            obj().apply {
                addChild("terrain", string(it.terrain))
                addChild("temperatureFrom", JsonValue(it.temperatureFrom.toDouble()))
                addChild("temperatureTo", JsonValue(it.temperatureTo.toDouble()))
                addChild("humidityFrom", JsonValue(it.humidityFrom.toDouble()))
                addChild("humidityTo", JsonValue(it.humidityTo.toDouble()))
            }
        }))
        ruleJson.addChild("vegetation", array(rules.vegetation.map {
            obj().apply { addChild("terrain", string(it.terrain)); addChild("occursOn", strings(it.occursOn)) }
        }))
        root.addChild("rules", ruleJson)
        root.addChild("cells", cellsJson(input.cells))
        return root
    }

    private fun cellsJson(cells: List<TerrainSurfaceCell>) = array(cells.map { cell ->
        obj().apply {
            addChild("column", number(cell.column)); addChild("row", number(cell.row))
            addChild("x", JsonValue(cell.x)); addChild("y", JsonValue(cell.y))
            addChild("latitude", JsonValue(cell.latitude.toDouble())); addChild("longitude", JsonValue(cell.longitude.toDouble()))
            addChild("neighbors", array(cell.neighbors.map { number(it) }))
            addChild("baseTerrain", string(cell.baseTerrain)); addChild("features", strings(cell.features))
            addChild("temperature", cell.temperature?.let { JsonValue(it) } ?: nil())
            addChild("humidity", cell.humidity?.let { JsonValue(it) } ?: nil())
        }
    })

    private fun decodeCells(value: JsonValue): List<TerrainSurfaceCell> = value.map { cell ->
        TerrainSurfaceCell(cell.getInt("column"), cell.getInt("row"), cell.getDouble("x"), cell.getDouble("y"),
            cell.getFloat("latitude"), cell.getFloat("longitude"), cell.get("neighbors").map { it.asInt() },
            cell.getString("baseTerrain"), cell.get("features").map { it.asString() },
            cell.get("temperature").let { if (it.isNull) null else it.asDouble() },
            cell.get("humidity").let { if (it.isNull) null else it.asDouble() })
    }

    private fun decodeInput(root: JsonValue): TerrainExperimentInput {
        require(root.getInt("formatVersion") == 1) { "Unsupported terrain input format" }
        require(root.getString("algorithmVersion") == TerrainGenerationPipeline.ALGORITHM_VERSION) { "Unsupported terrain algorithm" }
        val s = root.get("settings")
        val settings = TerrainExperimentSettings(s.getLong("seed"), s.getDouble("climateScale"),
            s.getDouble("vegetationScale"), s.getDouble("elevationScale"), s.getFloat("elevationExponent"),
            s.getFloat("temperatureExtremeness"), s.getFloat("temperatureShift"), s.getFloat("vegetationRichness"),
            s.getBoolean("flatEarth"), TerrainGenerationMode.valueOf(s.getString("mode")))
        val r = root.get("rules")
        fun set(name: String) = r.get(name).map { it.asString() }.toSet()
        fun optional(name: String): String? = r.get(name).let { if (it.isNull) null else it.asString() }
        val rules = TerrainRuleSnapshot(set("terrains"), set("waterTerrains"), set("impassableTerrains"),
            r.getString("firstLandTerrain"), optional("flatTerrain"), optional("mountainTerrain"), optional("hillTerrain"),
            set("climateExcludedTerrains"), r.get("climateRanges").map {
                // Нижние границы уже включают исторический ulp; повторный сдвиг запрещён.
                ClimateTerrainRange(it.getString("terrain"), it.getFloat("temperatureFrom"), it.getFloat("temperatureTo"),
                    it.getFloat("humidityFrom"), it.getFloat("humidityTo"))
            }, r.get("vegetation").map {
                VegetationRule(it.getString("terrain"), it.get("occursOn").map { base -> base.asString() })
            }, r.getString("risingMarker"), r.getString("loweringMarker"))
        return TerrainExperimentInput(decodeCells(root.get("cells")), rules, settings)
    }

    private fun obj() = JsonValue(JsonValue.ValueType.`object`)
    private fun nil() = JsonValue(JsonValue.ValueType.nullValue)
    private fun string(value: String?): JsonValue = if (value == null) nil() else JsonValue(value)
    private fun number(value: Int) = JsonValue(value.toLong())
    private fun array(values: List<JsonValue>) = JsonValue(JsonValue.ValueType.array).apply { values.forEach { addChild(it) } }
    private fun strings(values: List<String>) = array(values.map { string(it) })

    /** Записывает строгий JSON, сохраняя знак нуля и явные null. */
    private fun text(value: JsonValue): String = when {
        value.isNull -> "null"
        value.isObject -> value.joinToString(prefix = "{", postfix = "}", separator = ",") {
            JsonWriter.OutputType.json.quoteName(it.name) + ":" + text(it)
        }

        value.isArray -> value.joinToString(prefix = "[", postfix = "]", separator = ",") {
            text(it)
        }

        value.isString -> JsonWriter.OutputType.json.quoteValue(value.asString())
        value.isDouble -> {
            val number = value.asDouble()
            require(number.isFinite()) { "Non-finite number cannot be stored in a terrain snapshot" }
            number.toString()
        }

        value.isLong -> value.asLong().toString()
        value.isBoolean -> value.asBoolean().toString()
        else -> error("Unsupported JSON value type: ${value.type()}")
    }
    private fun sha256(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 255) }
    private fun write(file: File, value: JsonValue) {
        file.absoluteFile.parentFile.mkdirs()
        file.writeText(text(value), Charsets.UTF_8)
    }
}
