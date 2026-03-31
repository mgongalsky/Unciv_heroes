package com.unciv.pure.infrastructure.hero

import com.unciv.pure.domain.hero.IHeroDefinitionSource

class HardcodedHeroDefinitionSource(
    private val attackSkill: Int,
    private val defenseSkill: Int,
    private val foodCapacity: Float = 15f
) : IHeroDefinitionSource {
    override fun getAttackSkill(unitName: String) = attackSkill
    override fun getDefenseSkill(unitName: String) = defenseSkill
    override fun getFoodCapacity(unitName: String) = foodCapacity
}
