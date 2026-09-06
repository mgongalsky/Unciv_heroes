package com.unciv.pure.domain.troop

interface ITroopDefinitionSource {
    fun getSpeed(unitName: String): Int
    fun getDamage(unitName: String): Int
    fun getMaxHealth(unitName: String): Int
    fun getRangedStrength(unitName: String): Int
    fun isSelfFeeding(unitName: String): Boolean
    fun getFormationHealthPercent(unitName: String): Int = 0
    fun getFormationDamageReductionPercent(unitName: String): Int = 0
}
