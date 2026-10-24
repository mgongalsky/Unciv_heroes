package com.unciv.battle.protocol

data class WireTroopSnapshot(
    val id: Int,
    val side: WireSide,
    val slot: Int,
    val unitName: String,
    val initialAmount: Int,
    val currentAmount: Int,
    val currentHealth: Int,
    val maxHealth: Int,
    val speed: Int,
    val damage: Int,
    val rangedStrength: Int,
    val formation: Int,
    val maximumFormation: Int,
    val position: WirePoint?,
    val retaliationBudget: Int,
    val normalRetaliationBudget: Int,
    val hasPendingFollowUpShot: Boolean,
    val hasFormationRecoveryChance: Boolean,
    val supportBonusPercent: Int,
    val flankingLevel: Int,
    val formationHealthPercent: Int = 0,
    val formationDamageReductionPercent: Int = 0
)

data class WireFieldCell(
    val position: WirePoint,
    val baseTerrain: String,
    val features: List<String>,
    val temperature: Double,
    val humidity: Double,
    val impassable: Boolean
)

data class WireFieldDescription(val width: Int, val height: Int, val cells: List<WireFieldCell>)

/** An offered action, not a submitted request: no sequence or revision is assigned yet. */
data class WireAvailableCommand(
    val action: WireAction,
    val troopId: Int,
    val target: WirePoint? = null,
    val attackFrom: WirePoint? = null
)

data class WireBattleState(
    val troops: List<WireTroopSnapshot>,
    val turnQueue: List<Int>,
    val remainingTurnsInRound: Int,
    val currentTroopId: Int?,
    val currentSide: WireSide?,
    val finished: Boolean,
    val winner: WireSide?,
    val field: WireFieldDescription? = null,
    val availableCommands: List<WireAvailableCommand> = emptyList(),
    val presentation: WireBattlePresentation = WireBattlePresentation(
        emptyList(),
        emptyList(),
        emptyList()
    )
)

data class WireSessionSnapshot(
    val revision: Long,
    val lastSequence: Long,
    val lastReply: WireSessionReply?,
    val state: WireBattleState
)

/** Seat-specific read response. Never contains credentials or the opponent's receipt. */
data class WireRoomView(
    val protocolVersion: Int,
    val scenarioId: String,
    val side: WireSide,
    val phase: WireRoomPhase,
    val opponentJoined: Boolean,
    val attackerReady: Boolean,
    val defenderReady: Boolean,
    val battle: WireSessionSnapshot,
    val battleNumber: Int = 1,
    val attackerRematch: Boolean = false,
    val defenderRematch: Boolean = false,
    val report: WireBattleReport? = null
)

data class WireScenario(val id: String, val title: String, val description: String)
data class WireScenarioCatalog(val protocolVersion: Int, val scenarios: List<WireScenario>)
data class WireError(val protocolVersion: Int, val code: String)
