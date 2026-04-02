package com.unciv.pure.domain.battle

data class DamageResult(
    val damageDealt: Int,
    val perished: Int,
    val remainingAmount: Int,
    val remainingHealth: Int,
    val isLuck: Boolean
)
