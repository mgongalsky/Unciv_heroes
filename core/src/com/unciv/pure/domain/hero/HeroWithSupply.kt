package com.unciv.pure.domain.hero

interface HeroWithSupply {
    val currentFood: Float
    val baseFoodCapacity: Float
    fun addFood(amount: Float)
    fun setFood(amount: Float)
}
