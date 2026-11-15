package com.unciv.pure.application.city

/** Независимые значения снабжения; версия связывает команду с показанными игроку запасами. */
data class CityHeroSupplySnapshot(
    val revision: Long,
    val enabled: Boolean,
    val hasHero: Boolean,
    val canChange: Boolean,
    val cityFood: Int,
    val heroFood: Float,
    val manualCapacity: Float,
    val automaticCapacity: Float,
    val maintenance: Float,
    val morale: Int,
    val autoSupply: Boolean,
    val minimumTransfer: Float,
    val maximumTransfer: Float
)
