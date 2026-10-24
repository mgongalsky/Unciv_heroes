package com.unciv.pure.domain.battle

/** Percentage bonuses per effective flanking level, configured by the unit definition. */
data class FlankingProfile(
    val formedDamagePercent: Int = 5,
    val formationDamagePercent: Int = 10,
    val unformedDamagePercent: Int = 10
) {
    init {
        require(formedDamagePercent >= 0)
        require(formationDamagePercent >= 0)
        require(unformedDamagePercent >= 0)
    }
}
