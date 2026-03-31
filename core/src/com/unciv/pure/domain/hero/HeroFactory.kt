package com.unciv.pure.domain.hero

object HeroFactory {
    fun create(
        baseAttackSkill: Int,
        baseDefenseSkill: Int,
        baseFoodCapacity: Float = 15f
    ): Hero = Hero(
        baseAttackSkill = baseAttackSkill,
        baseDefenseSkill = baseDefenseSkill,
        baseFoodCapacity = baseFoodCapacity,
        currentFood = 3f,
        morale = 3,
        luck = 3
    )
}
