package com.unciv.clean.adapters.persistence

import com.unciv.clean.application.dto.GameInfoDto
import com.unciv.clean.application.mappers.GameInfoMapper
import com.unciv.clean.application.ports.GameStateRepository
import com.unciv.clean.application.ports.persistence.SaveWritePolicy
import com.unciv.logic.GameInfo
import com.unciv.json.json

class JsonGameStateRepository(
    private val readBytes: () -> String,
    private val writeBytes: (String) -> Unit
) : GameStateRepository {
    override val compatibility: Int = com.unciv.logic.GameInfo.CURRENT_COMPATIBILITY_NUMBER

    private val json = json()
    private val migrationRegistry = SaveMigrationRegistry()
    private val saveIO = VersionedSaveIO(
        json = json,
        migrationRegistry = migrationRegistry,
        writePolicy = SaveWritePolicy.LEGACY_BY_DEFAULT
    )

    override fun load(): GameInfo {
        val dto = saveIO.readToDto(readBytes())
        return GameInfoMapper.fromDto(dto) { json.fromJson(GameInfo::class.java, it) }
    }

    override fun save(state: GameInfo) {
        val dto = GameInfoMapper.toDto(state) { json.toJson(it) }
        val out = saveIO.writeFromDto(dto)
        writeBytes(out)
    }
}
