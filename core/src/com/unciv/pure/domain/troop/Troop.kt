package com.unciv.pure.domain.troop

class Troop(
    val unitName: String,
    val amount: Int,
    val speed: Int,
    val damage: Int,
    val maxHealth: Int,
    val rangedStrength: Int,
    val isSelfFeeding: Boolean,
    currentAmount: Int,
    currentHealth: Int,
    val id: Int
) {
    var currentAmount: Int = currentAmount
    var currentHealth: Int = currentHealth
}
