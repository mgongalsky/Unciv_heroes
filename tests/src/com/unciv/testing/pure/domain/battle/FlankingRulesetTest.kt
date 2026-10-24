package com.unciv.testing.pure.domain.battle

import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.battle.FlankingProfile
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlankingRulesetTest {
    @Test
    fun `factory preserves a custom flanking profile from the ruleset`() {
        val profile = FlankingProfile(unformedDamagePercent = 15)
        val ruleset = Ruleset().apply {
            units["Custom melee"] = BaseUnit().apply {
                name = "Custom melee"
                health = 40
                damage = 10
                flanking = profile
            }
        }
        val troop = TroopFactory.create("Custom melee", 10, RulesetTroopDefinitionSource(ruleset))
        assertTrue(troop.hasFlanking)
        assertEquals(profile, troop.flanking)
        assertEquals(15, troop.flanking!!.unformedDamagePercent)
    }

    @Test
    fun `absent property disables flanking and ranged troops cannot participate`() {
        val ruleset = Ruleset().apply {
            units["Peasant"] = BaseUnit().apply { name = "Peasant" }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"
                rangedStrength = 7
                flanking = FlankingProfile()
            }
        }
        val source = RulesetTroopDefinitionSource(ruleset)
        val peasant = TroopFactory.create("Peasant", 10, source)
        assertNull(peasant.flanking)
        assertFalse(peasant.hasFlanking)
        assertFalse(TroopFactory.create("Archer", 10, source).hasFlanking)
        assertNull(source.getFlankingProfile("Missing unit"))
    }
}
