package com.unciv.logic.event

import com.unciv.logic.map.TileInfo

/** Event emitted when a one-time loot improvement should play its pickup animation. */
data class LootPickupAnimationRequested(
    val tile: TileInfo,
    val improvementName: String,
    val durationSeconds: Float = 2f
) : Event
