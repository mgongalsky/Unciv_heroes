package com.unciv.logic

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter

/** Последовательно обновляет JSON карты без создания игровых объектов или событий захвата. */
object MapFormatMigration {
    const val currentVersion = 3
    private const val legacyVersion = 0

    /** Будущий неизвестный формат отклоняется до чтения карты и её превью. */
    fun migrate(mapJson: String): String {
        val root = JsonReader().parse(mapJson)
        val parameters = root.get("mapParameters")
            ?: throw UncivShowableException("Invalid map: mapParameters are missing.")
        var version = parameters.get("mapFormatVersion")?.asInt() ?: legacyVersion
        if (version > currentVersion) {
            throw UncivShowableException(
                "This map uses format version [$version], but this game supports only up to [$currentVersion]."
            )
        }
        while (version < currentVersion) {
            when (version) {
                legacyVersion -> migrateLegacyToVersion1(parameters)
                1 -> migrateVersion1ToVersion2(root, parameters)
                2 -> migrateVersion2ToVersion3(root, parameters)
                else -> throw UncivShowableException("No map migration exists from format version [$version].")
            }
            version++
        }
        return root.toJson(JsonWriter.OutputType.json)
    }

    /** Старые карты получают пустые описания городов и слотов, а не игровые города. */
    private fun migrateVersion1ToVersion2(root: JsonValue, parameters: JsonValue) {
        if (root.get("scenarioCities") == null)
            root.addChild("scenarioCities", JsonValue(JsonValue.ValueType.array))
        if (root.get("scenarioPlayerSlots") == null)
            root.addChild("scenarioPlayerSlots", JsonValue(JsonValue.ValueType.array))
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(2L))
    }

    /** Отсутствие авторской политики сохраняет выбор побед игрока на старых картах. */
    private fun migrateVersion2ToVersion3(root: JsonValue, parameters: JsonValue) {
        if (root.get("scenarioVictorySettings") == null)
            root.addChild("scenarioVictorySettings", JsonValue(JsonValue.ValueType.nullValue))
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(3L))
    }

    /** Первый переход вводит только явный номер формата. */
    private fun migrateLegacyToVersion1(parameters: JsonValue) {
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(1L))
    }
}
