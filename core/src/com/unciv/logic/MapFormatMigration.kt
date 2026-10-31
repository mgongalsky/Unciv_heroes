package com.unciv.logic

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter

/**
 * Последовательно обновляет JSON карты до чтения в текущую модель.
 * Отсутствующая версия означает старый формат 0. Каждый переход меняет только один номер версии;
 * новые переходы добавляются сюда вместе с увеличением currentVersion, без запуска игровой логики.
 */
object MapFormatMigration {
    const val currentVersion = 2
    private const val legacyVersion = 0

    /** Возвращает обновлённый JSON; будущий неизвестный формат отклоняется, а не читается наугад. */
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
                else -> throw UncivShowableException("No map migration exists from format version [$version].")
            }
            version++
        }

        return root.toJson(JsonWriter.OutputType.json)
    }

    /** Старые карты не имели описаний городов и слотов: добавляем пустые коллекции, не игровые объекты. */
    private fun migrateVersion1ToVersion2(root: JsonValue, parameters: JsonValue) {
        if (root.get("scenarioCities") == null)
            root.addChild("scenarioCities", JsonValue(JsonValue.ValueType.array))
        if (root.get("scenarioPlayerSlots") == null)
            root.addChild("scenarioPlayerSlots", JsonValue(JsonValue.ValueType.array))
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(2L))
    }

    /** Первый переход только вводит явный номер формата. */
    private fun migrateLegacyToVersion1(parameters: JsonValue) {
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(1L))
    }
}
