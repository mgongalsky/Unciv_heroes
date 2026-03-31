package com.unciv.pure.domain.hero

interface IHeroDefinitionSource {
    fun getAttackSkill(unitName: String): Int
    fun getDefenseSkill(unitName: String): Int
    fun getFoodCapacity(unitName: String): Float
}
