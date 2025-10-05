package com.unciv.clean.adapters.persistence

import com.unciv.clean.application.dto.SaveEnvelopeDto
import com.unciv.clean.application.ports.persistence.SaveFormatVersion
import com.unciv.clean.application.ports.persistence.SaveMigration

class SaveMigrationRegistry(
    private val migrations: List<SaveMigration> = emptyList()
) {
    fun migrateToCurrent(envelope: SaveEnvelopeDto): SaveEnvelopeDto {
        var current = envelope
        val target = SaveFormatVersion.CURRENT
        if (current.formatVersion > target) {
            // Future format: leave as is (alternatively, throw)
            return current
        }
        while (current.formatVersion < target) {
            val next = current.formatVersion + 1
            val step = migrations.firstOrNull { it.from == current.formatVersion && it.to == next }
                ?: error("No migration ${current.formatVersion} -> $next")
            current = step.migrate(current)
        }
        return current
    }
}
