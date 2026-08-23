package com.unciv.pure.application.battle

enum class BattleTermination {
    VICTORY,
    MUTUAL_DEFEAT,
    STALEMATE,
    MAX_TURNS,
    NO_CURRENT_TROOP
}

data class BattleSimulationResult(
    val termination: BattleTermination,
    val winnerIsAttacker: Boolean?,
    val turns: Int,
    val commands: List<BattleCommand>,
    val events: List<BattleEvent>,
    val seed: Long? = null
)
