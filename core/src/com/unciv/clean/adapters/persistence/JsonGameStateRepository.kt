package com.unciv.clean.adapters.persistence

import com.unciv.clean.application.ports.GameStateRepository
import com.unciv.logic.GameInfo
import com.unciv.json.json

class JsonGameStateRepository(
    private val readBytes: () -> String,
    private val writeBytes: (String) -> Unit
) : GameStateRepository {
    override val compatibility: Int = com.unciv.logic.GameInfo.CURRENT_COMPATIBILITY_NUMBER
    override fun load(): GameInfo = json().fromJson(GameInfo::class.java, readBytes())
    override fun save(state: GameInfo) = writeBytes(json().toJson(state))
}
