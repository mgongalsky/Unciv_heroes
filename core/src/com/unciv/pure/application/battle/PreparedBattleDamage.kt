package com.unciv.pure.application.battle

/** Values captured while preparing a hit, before target-specific formation and flanking. */
data class PreparedBattleDamage(
    val amount: Int,
    val damagePerSoldier: Int,
    val luckMultiplier: Int,
    val afterLuck: Int,
    val supportBonusPercent: Int,
    val afterSupport: Int,
    val penaltyPercent: Int,
    val incomingDamage: Int
)
