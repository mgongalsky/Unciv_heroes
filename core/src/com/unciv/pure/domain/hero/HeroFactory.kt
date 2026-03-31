package com.unciv.pure.domain.hero

object HeroFactory {
    fun create(
        source: IHeroDefinitionSource,
        unitName: String,
        currentFood: Float = 3f,
        morale: Int = 3,
        luck: Int = 3
    ): Hero = Hero(
        baseAttackSkill = source.getAttackSkill(unitName),
        baseDefenseSkill = source.getDefenseSkill(unitName),
        baseFoodCapacity = source.getFoodCapacity(unitName),
        currentFood = currentFood,
        morale = morale,
        luck = luck
    )
}
