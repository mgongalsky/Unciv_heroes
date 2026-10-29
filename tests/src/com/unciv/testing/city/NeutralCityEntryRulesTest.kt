package com.unciv.testing.city

import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.NeutralCityEntryRules
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision
import org.junit.Assert.*
import org.junit.Test

class NeutralCityEntryRulesTest {
    @Test
    fun heroAcquiresEmptyCityAndMustFightEvenOneLivingDefender() {
        assertEquals(Decision.ACQUIRE, decide(true, 0))
        assertEquals(Decision.FIGHT_GARRISON, decide(true, 1))
        assertEquals(Decision.FIGHT_GARRISON, decide(true, Int.MAX_VALUE))
        repeat(3) { assertEquals(Decision.FIGHT_GARRISON, decide(true, 1)) }
    }

    @Test
    fun ineligibleUnitCannotAcquireOrAttackThroughThisEntryRule() {
        assertEquals(Decision.INELIGIBLE_UNIT, decide(false, 0))
        assertEquals(Decision.INELIGIBLE_UNIT, decide(false, 1))
    }

    @Test
    fun ownedAndUnboundCitiesRemainOutsideNeutralEntry() {
        for (owner in listOf(CityOwnership.Player("Rome"), null)) {
            assertEquals(Decision.NOT_NEUTRAL,
                NeutralCityEntryRules.decide(owner, true, 0))
            assertEquals(Decision.NOT_NEUTRAL,
                NeutralCityEntryRules.decide(owner, true, 1))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeDefenderCountIsRejected() {
        decide(true, -1)
    }

    private fun decide(hero: Boolean, defenders: Int) =
        NeutralCityEntryRules.decide(CityOwnership.Neutral, hero, defenders)
}
