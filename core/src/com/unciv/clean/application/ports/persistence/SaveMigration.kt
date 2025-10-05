package com.unciv.clean.application.ports.persistence

import com.unciv.clean.application.dto.SaveEnvelopeDto

interface SaveMigration {
    val from: Int
    val to: Int
    fun migrate(input: SaveEnvelopeDto): SaveEnvelopeDto
}
