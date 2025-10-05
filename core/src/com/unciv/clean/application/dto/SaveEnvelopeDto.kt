package com.unciv.clean.application.dto

/**
 * "Конверт" версии V1: метаданные + сырой payload (старый JSON GameInfo).
 * На Stage 11 мы НЕ меняем структуру payload, только оборачиваем его.
 */
data class SaveEnvelopeDto(
    val formatVersion: Int,
    val meta: SaveMetaDto,
    val payloadRaw: String
)

data class SaveMetaDto(
    val createdAtEpochMs: Long,
    val gameBuild: String? = null,
    val rulesetName: String? = null,
    val rulesetHash: String? = null,
    val rngSeed: Long? = null
)
