package com.unciv.pure.domain.hero

class Hero(
    val baseAttackSkill: Int,
    val baseDefenseSkill: Int,
    override val baseFoodCapacity: Float,
    currentFood: Float,
    morale: Int,
    luck: Int
) : HeroWithSupply, HeroWithSettle {

    override var currentFood: Float = currentFood

    var morale: Int = morale


    var luck: Int = luck


    override fun addFood(amount: Float) { currentFood += amount }
    override fun setFood(amount: Float) { currentFood = amount }

    override fun canSettle(): Boolean {
        TODO("Not yet implemented")
    }
}
