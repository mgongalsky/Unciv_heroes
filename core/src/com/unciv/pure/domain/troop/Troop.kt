package com.unciv.pure.domain.troop

class Troop(
    val unitName: String = "",
    var amount: Int = 0,
    val speed: Int = 0,
    val damage: Int = 0,
    val maxHealth: Int = 0,
    val rangedStrength: Int = 0,
    val isSelfFeeding: Boolean = false,
    currentAmount: Int = amount,
    currentHealth: Int = maxHealth,
    val id: Int = 0,
    var formation: Formation = Formation.forSoldiers(currentAmount)
) {
    var currentAmount: Int = currentAmount
    var currentHealth: Int = currentHealth
    val isRanged: Boolean get() = rangedStrength > 0

    /** Initializes saves created before formation existed without repairing a deliberately broken formation. */
    fun restoreFormationIfMissing() {
        if (currentAmount > 0 && formation.maximum == 0 && formation.current == 0) {
            formation = Formation.forSoldiers(currentAmount)
        } else {
            formation.normalize()
        }
    }
}
