package com.unciv.testing.simulations

import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test

class MovementImmunityUnitSourcesTest {
    @Test
    fun `game horseman retains immunity through experiment ruleset and troop factory`() {
        val definitions = BalanceUnitSources.preset
        assertTrue(definitions.single { it.name == "Horseman" }.ignoresMovementFormationPenalty)
        assertTrue(definitions.filter { it.name != "Horseman" }.none { it.ignoresMovementFormationPenalty })
        val source = RulesetTroopDefinitionSource(BalanceUnitSources.ruleset(definitions))
        val horseman = TroopFactory.createWithId(1, "Horseman", 10, source)
        assertTrue(horseman.ignoresMovementFormationPenalty)
        assertTrue(horseman.hasFormation)
        assertFalse(TroopFactory.createWithId(2, "Peasant", 10, source).hasFormation)
    }

    @Test
    fun `custom experiment can enable and disable immunity explicitly`() {
        fun parse(enabled: Boolean) = BalanceUnitSources.parse(
            listOf("Peasant", "Swordsman", "Archer", "Spearman", "Horseman").joinToString(
                prefix = "[", postfix = "]"
            ) { name ->
                """{"name":"$name","speed":7,"health":70,"damage":20,"ignoresMovementFormationPenalty":$enabled}"""
            }
        )
        for (enabled in listOf(false, true)) {
            val definitions = parse(enabled)
            assertTrue(definitions.all { it.ignoresMovementFormationPenalty == enabled })
            val ruleset = BalanceUnitSources.ruleset(definitions)
            assertEquals(enabled, ruleset.units.getValue("Horseman").ignoresMovementFormationPenalty)
        }
    }
}
