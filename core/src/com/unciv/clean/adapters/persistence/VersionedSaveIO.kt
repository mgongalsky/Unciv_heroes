package com.unciv.clean.adapters.persistence

import com.badlogic.gdx.utils.Json
import com.unciv.clean.application.dto.GameInfoDto
import com.unciv.clean.application.dto.SaveEnvelopeDto
import com.unciv.clean.application.dto.SaveMetaDto
import com.unciv.clean.application.ports.persistence.SaveFormatVersion
import com.unciv.clean.application.ports.persistence.SaveWritePolicy

/**
 * IO-обёртка: умеет читать legacy и V1; писать legacy или V1 по политике.
 * Версия для работы со строками (raw), т.к. репозиторий использует read/write замыкания.
 */
class VersionedSaveIO(
    private val json: Json,
    private val migrationRegistry: SaveMigrationRegistry,
    private val writePolicy: SaveWritePolicy = SaveWritePolicy.LEGACY_BY_DEFAULT,
    private val metaProvider: () -> SaveMetaDto = { SaveMetaDto(System.currentTimeMillis()) }
) {

    fun readToDto(raw: String): GameInfoDto {
        val envelope = tryParseEnvelope(raw)
        return if (envelope == null) {
            // Legacy-сейв
            GameInfoDto(raw = raw)
        } else {
            val migrated = migrationRegistry.migrateToCurrent(envelope)
            GameInfoDto(raw = migrated.payloadRaw)
        }
    }

    fun writeFromDto(dto: GameInfoDto): String {
        return when (writePolicy) {
            SaveWritePolicy.LEGACY_BY_DEFAULT -> dto.raw
            SaveWritePolicy.VERSIONED_V1 -> json.toJson(
                SaveEnvelopeDto(
                    formatVersion = SaveFormatVersion.V1,
                    meta = metaProvider(),
                    payloadRaw = dto.raw
                ),
                SaveEnvelopeDto::class.java
            )
        }
    }

    private fun tryParseEnvelope(raw: String): SaveEnvelopeDto? {
        return try {
            if (!raw.contains("\"formatVersion\"")) null
            else json.fromJson(SaveEnvelopeDto::class.java, raw)
        } catch (_: Throwable) {
            null
        }
    }
}
