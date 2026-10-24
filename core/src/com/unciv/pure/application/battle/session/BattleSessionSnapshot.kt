package com.unciv.pure.application.battle.session

/** Captured atomically by the session owner; the receipt belongs only to the requesting seat. */
data class BattleSessionSnapshot(
    val revision: Long,
    val lastSequence: Long,
    val lastReply: BattleSessionReply?,
    val state: BattleStateSnapshot
)
