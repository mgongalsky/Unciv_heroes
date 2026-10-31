package com.unciv.pure.domain.hero

/** Локальные характеристики и запасы героя без ссылок на карту, юнит или цивилизацию. */
class Hero(
    val baseAttackSkill: Int,
    val baseDefenseSkill: Int,
    override val baseFoodCapacity: Float,
    currentFood: Float,
    morale: Int,
    luck: Int
) : HeroWithSupply, HeroWithSettle {

    /** Загрузчик JSON сначала создаёт объект, затем восстанавливает сохранённые характеристики вместо этих нулей. */
    @Suppress("unused")
    constructor() : this(0, 0, 0f, 0f, 0, 0)

    override var currentFood: Float = currentFood

    var morale: Int = morale

    var luck: Int = luck

    override fun addFood(amount: Float) { currentFood += amount }
    override fun setFood(amount: Float) { currentFood = amount }

    /** Контракт чистого основания пока не реализован; действующая игровая механика остаётся в адаптерах. */
    override fun canSettle(): Boolean {
        TODO("Not yet implemented")
    }
}
