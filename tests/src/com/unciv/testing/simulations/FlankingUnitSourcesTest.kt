package com.unciv.testing.simulations

import com.unciv.logic.battle.StandaloneBattleSetup
import com.unciv.pure.domain.battle.FlankingProfile
import org.junit.Assert.*
import org.junit.Test

class FlankingUnitSourcesTest {
    @Test
    fun `shipped melee profiles reach real standalone troops while peasants and archers stay excluded`() {
        val definitions = BalanceUnitSources.preset
        val ruleset = BalanceUnitSources.ruleset(definitions)
        assertEquals(setOf("Spearman", "Swordsman", "Horseman"),
            ruleset.units.values.filter { it.flanking != null }.map { it.name }.toSet())
        for (name in listOf("Spearman", "Swordsman", "Horseman", "Peasant", "Archer")) {
            val troop = StandaloneBattleSetup.army(
                listOf(StandaloneBattleSetup.Stack(name, 4)), ruleset
            ).getTroopAt(0)!!
            if (name == "Peasant" || name == "Archer") {
                assertNull(troop.flanking)
                assertFalse(troop.hasFlanking)
            } else {
                assertEquals(FlankingProfile(), troop.flanking)
                assertTrue(troop.hasFlanking)
            }
        }
        assertEquals(25, ruleset.units.getValue("Spearman").supportBonusPercent)
        assertEquals(25, ruleset.units.getValue("Swordsman").supportBonusPercent)
        assertEquals(60, ruleset.units.getValue("Spearman").formationDamageReductionPercent)
    }

    @Test
    fun `selected experiment JSON can strengthen or disable the game flanking profile`() {
        val text = listOf("Peasant", "Swordsman", "Archer", "Spearman", "Horseman")
            .joinToString(prefix = "[", postfix = "]") { name ->
                val profile = when (name) {
                    "Horseman" -> ",\"flanking\":{\"unformedDamagePercent\":15}"
                    "Spearman" -> ",\"flanking\":{}"
                    "Swordsman" -> ",\"flanking\":null"
                    else -> ""
                }
                """{"name":"$name","speed":5,"health":40,"damage":10$profile}"""
            }
        val definitions = BalanceUnitSources.parse(text)
        val ruleset = BalanceUnitSources.ruleset(definitions)
        assertEquals(FlankingProfile(unformedDamagePercent = 15), ruleset.units.getValue("Horseman").flanking)
        assertEquals(FlankingProfile(), ruleset.units.getValue("Spearman").flanking)
        assertNull(ruleset.units.getValue("Swordsman").flanking)
        assertNull(ruleset.units.getValue("Peasant").flanking)
        assertNull(ruleset.units.getValue("Archer").flanking)
    }
}
