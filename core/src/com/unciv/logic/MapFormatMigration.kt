package com.unciv.logic

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter

/**
 * Upgrades serialized maps before they are read into the current domain model.
 *
 * Maps without a version predate explicit format versioning and are treated as version 0.
 * Add future migrations as one-step transformations and increment [currentVersion].
 */
object MapFormatMigration {
    const val currentVersion = 1
    private const val legacyVersion = 0

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
                else -> throw UncivShowableException("No map migration exists from format version [$version].")
            }
            version++
        }

        return root.toJson(JsonWriter.OutputType.json)
    }

    private fun migrateLegacyToVersion1(parameters: JsonValue) {
        parameters.remove("mapFormatVersion")
        parameters.addChild("mapFormatVersion", JsonValue(1L))
    }
}
