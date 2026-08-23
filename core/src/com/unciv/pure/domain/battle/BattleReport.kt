package com.unciv.pure.domain.battle

enum class BattleSide {
    ATTACKER,
    DEFENDER
}

data class BattleLoss(
    val troopId: Int,
    val unitName: String,
    val amount: Int
)

data class BattleReport(
    val winner: BattleSide?,
    val attackerLosses: List<BattleLoss>,
    val defenderLosses: List<BattleLoss>
)
