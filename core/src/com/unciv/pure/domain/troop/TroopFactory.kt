package com.unciv.pure.domain.troop

object TroopFactory {
    fun create(
        unitName: String,
        amount: Int,
        source: ITroopDefinitionSource
    ): Troop = Troop(
        unitName = unitName,
        amount = amount,
        speed = source.getSpeed(unitName),
        damage = source.getDamage(unitName),
        maxHealth = source.getMaxHealth(unitName),
        rangedStrength = source.getRangedStrength(unitName),
        currentAmount = amount,
        currentHealth = source.getMaxHealth(unitName)
    )
}
