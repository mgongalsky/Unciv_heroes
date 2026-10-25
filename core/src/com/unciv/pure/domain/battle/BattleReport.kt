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

/** Whole surviving soldiers retain their recruitment value, including wounded soldiers. */
data class BattleScore(val initialValue: Long, val remainingValue: Long) {
    init {
        require(initialValue >= 0)
        require(remainingValue in 0..initialValue)
    }

    val percent: Double
        get() = if (initialValue == 0L) 0.0 else remainingValue.toDouble() / initialValue * 100.0
}

data class BattleReport(
    val winner: BattleSide?,
    val attackerLosses: List<BattleLoss>,
    val defenderLosses: List<BattleLoss>,
    val winnerScore: BattleScore? = null
)
