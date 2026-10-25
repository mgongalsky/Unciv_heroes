package com.unciv.battle.protocol

data class WireBattleLoss(val troopId: Int, val unitName: String, val amount: Int)

/** Server-computed values; clients must not value survivors using their local ruleset. */
data class WireBattleScore(val initialValue: Long, val remainingValue: Long)

data class WireBattleReport(
    val winner: WireSide?,
    val attackerLosses: List<WireBattleLoss>,
    val defenderLosses: List<WireBattleLoss>,
    val winnerScore: WireBattleScore? = null
)
