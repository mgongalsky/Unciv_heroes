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
    var formation: Formation = Formation(),
    val formationHealthPercent: Int = 0,
    val formationDamageReductionPercent: Int = 0
) {
    var currentAmount: Int = currentAmount
    var currentHealth: Int = currentHealth
    val isRanged: Boolean get() = rangedStrength > 0
    val hasFormation: Boolean
        get() = formationHealthPercent > 0 && formationDamageReductionPercent > 0

    init {
        restoreFormationIfMissing()
    }

    /** Creates a full formation for the surviving soldiers using their maximum health. */
    fun resetFormation() {
        formation = if (hasFormation) Formation.forHealth(
            currentAmount, maxHealth, formationHealthPercent
        ) else Formation()
    }

    /** Missing configuration disables formation; a configured broken formation stays broken. */
    fun restoreFormationIfMissing() {
        if (!hasFormation) {
            formation = Formation()
        } else if (formation.maximum == 0 && formation.current == 0) {
            resetFormation()
        } else {
            formation.normalize()
        }
    }
}
