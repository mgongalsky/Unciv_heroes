package com.unciv.pure.domain.army

import com.unciv.pure.domain.troop.Troop

/** Independent local snapshot; does not change the legacy ArmyInfo.clone contract. */
fun Army.deepCopy(): Army = Army(maxSlots, getAllTroops().map { it?.copyForArmy() }.toTypedArray())

private fun Troop.copyForArmy(): Troop = Troop(
    unitName = unitName,
    amount = amount,
    speed = speed,
    damage = damage,
    maxHealth = maxHealth,
    rangedStrength = rangedStrength,
    isSelfFeeding = isSelfFeeding,
    currentAmount = currentAmount,
    currentHealth = currentHealth,
    id = id,
    formation = formation.copy(),
    formationHealthPercent = formationHealthPercent,
    formationDamageReductionPercent = formationDamageReductionPercent,
    supportBonusPercent = supportBonusPercent
).also {
    // Construction can normalize formation; a snapshot preserves the exact source state.
    it.formation = formation.copy()
    it.flanking = flanking
    it.rangedShot = rangedShot
    it.ignoresMovementFormationPenalty = ignoresMovementFormationPenalty
    it.isCavalry = isCavalry
    it.antiCavalryDamageBonusPercent = antiCavalryDamageBonusPercent
}
