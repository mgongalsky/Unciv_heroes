package com.unciv.pure.domain.troop

object TroopFactory {
    private var nextId = 1

    // Seam for testing
    fun resetIdCounter() { nextId = 1 }

    fun create(
        unitName: String,
        amount: Int,
        source: ITroopDefinitionSource
    ): Troop {
        return Troop(
            id = nextId++,
            unitName = unitName,
            amount = amount,
            speed = source.getSpeed(unitName),
            damage = source.getDamage(unitName),
            maxHealth = source.getMaxHealth(unitName),
            rangedStrength = source.getRangedStrength(unitName),
            currentAmount = amount,
            currentHealth = source.getMaxHealth(unitName),
            isSelfFeeding = source.isSelfFeeding(unitName),
            formationHealthPercent = source.getFormationHealthPercent(unitName).coerceAtLeast(0),
            formationDamageReductionPercent = source.getFormationDamageReductionPercent(unitName)
                .coerceIn(0, 100),
            supportBonusPercent = source.getSupportBonusPercent(unitName).coerceAtLeast(0)
        )
    }
}
