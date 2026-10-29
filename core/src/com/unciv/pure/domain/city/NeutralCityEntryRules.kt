package com.unciv.pure.domain.city

/** World entry decision; tactical combat and ownership effects are applied by their adapters. */
object NeutralCityEntryRules {
    enum class Decision {
        NOT_NEUTRAL,
        INELIGIBLE_UNIT,
        ACQUIRE,
        FIGHT_GARRISON
    }

    fun decide(
        ownership: CityOwnership?,
        eligibleHero: Boolean,
        livingDefenders: Int
    ): Decision {
        require(livingDefenders >= 0) { "Living defender count cannot be negative" }
        if (ownership != CityOwnership.Neutral) return Decision.NOT_NEUTRAL
        if (!eligibleHero) return Decision.INELIGIBLE_UNIT
        return if (livingDefenders == 0) Decision.ACQUIRE else Decision.FIGHT_GARRISON
    }
}
