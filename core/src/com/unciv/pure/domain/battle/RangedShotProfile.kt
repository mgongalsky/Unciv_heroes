package com.unciv.pure.domain.battle

/** Optional tactical shooting capability supplied by the unit ruleset. */
data class RangedShotProfile(
    val directShotRange: Int = 3,
    val indirectPenaltyPercent: Int = 50
) {
    init {
        require(directShotRange >= 0)
        require(indirectPenaltyPercent in 0..100)
    }
}
