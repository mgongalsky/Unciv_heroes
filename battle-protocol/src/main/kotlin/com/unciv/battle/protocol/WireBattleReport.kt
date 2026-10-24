package com.unciv.battle.protocol

data class WireBattleLoss(val troopId: Int, val unitName: String, val amount: Int)

data class WireBattleReport(
    val winner: WireSide?,
    val attackerLosses: List<WireBattleLoss>,
    val defenderLosses: List<WireBattleLoss>
)
