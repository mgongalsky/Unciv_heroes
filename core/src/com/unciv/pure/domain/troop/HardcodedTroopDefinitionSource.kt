package com.unciv.pure.domain.troop

class HardcodedTroopDefinitionSource(
    private val speed: Int,
    private val damage: Int,
    private val maxHealth: Int,
    private val rangedStrength: Int,
    private val isSelfFeeding: Boolean = false
) : ITroopDefinitionSource {
    override fun getSpeed(unitName: String) = speed
    override fun getDamage(unitName: String) = damage
    override fun getMaxHealth(unitName: String) = maxHealth
    override fun getRangedStrength(unitName: String) = rangedStrength
    override fun isSelfFeeding(unitName: String) = isSelfFeeding
}
