package com.unciv.clean.application.ports

import com.unciv.logic.GameInfo

interface GameStateRepository {
    fun load(): GameInfo
    fun save(state: GameInfo)
    val compatibility: Int
}
