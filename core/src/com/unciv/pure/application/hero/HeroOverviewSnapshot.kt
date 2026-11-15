package com.unciv.pure.application.hero

/** Неизменяемые сведения для обзора героя; не содержат ссылок на игровую модель или UI. */
data class HeroOverviewSnapshot(
    val name: String,
    val attack: Int,
    val defense: Int,
    val morale: Int,
    val luck: Int,
    val supplyEnabled: Boolean,
    val food: Float,
    val overviewCapacity: Float,
    val manualCapacity: Float,
    val outsideMaintenance: Float,
    val foodDuration: Int,
    val settlementPopulation: Int
)
