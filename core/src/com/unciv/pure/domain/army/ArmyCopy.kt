package com.unciv.pure.domain.army

import com.unciv.pure.domain.troop.Troop

/**
 * Независимый снимок армии для копирования города: новые слоты, войска и изменяемый строй.
 * Существующий контракт ArmyInfo.clone других потребителей этим расширением не меняется.
 */
fun Army.deepCopy(): Army = Army(maxSlots, getAllTroops().map { it?.copyForArmy() }.toTypedArray())

/** Сохраняет ID и текущее боевое состояние войска, но отделяет изменяемые данные копии. */
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
    // Конструктор может нормализовать строй; снимок должен сохранить точное исходное состояние.
    it.formation = formation.copy()
    // Эти профили разделяются как значения; изменяемый строй выше скопирован отдельно.
    it.flanking = flanking
    it.rangedShot = rangedShot
    it.ignoresMovementFormationPenalty = ignoresMovementFormationPenalty
    it.isCavalry = isCavalry
    it.antiCavalryDamageBonusPercent = antiCavalryDamageBonusPercent
}
