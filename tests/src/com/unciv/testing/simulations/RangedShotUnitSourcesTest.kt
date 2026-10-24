package com.unciv.testing.simulations

import com.unciv.logic.battle.StandaloneBattleSetup
import com.unciv.pure.domain.battle.RangedShotProfile
import org.junit.Assert.*
import org.junit.Test

class RangedShotUnitSourcesTest {
    private fun definitions(profile: String): String =
        listOf("Peasant", "Swordsman", "Archer", "Spearman", "Horseman")
            .joinToString(prefix = "[", postfix = "]") { name ->
                val extra = if (name == "Archer") ",\"rangedStrength\":7$profile" else ""
                """{"name":"$name","speed":5,"health":40,"damage":10$extra}"""
            }

    @Test
    fun `experiment profile reaches standalone shooter`() {
        val definitions = BalanceUnitSources.parse(definitions(
            ",\"rangedShot\":{\"directShotRange\":5,\"indirectPenaltyPercent\":35}"
        ))
        val ruleset = BalanceUnitSources.ruleset(definitions)
        val troop = StandaloneBattleSetup.army(
            listOf(StandaloneBattleSetup.Stack("Archer", 4)), ruleset
        ).getTroopAt(0)!!
        assertEquals(RangedShotProfile(5, 35), troop.rangedShot)
        assertTrue(troop.isRanged)
    }

    @Test
    fun `absent and null experiment profiles disable the mechanic`() {
        for (extra in listOf("", ",\"rangedShot\":null")) {
            val ruleset = BalanceUnitSources.ruleset(BalanceUnitSources.parse(definitions(extra)))
            assertNull(ruleset.units.getValue("Archer").rangedShot)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid experiment penalty is rejected`() {
        BalanceUnitSources.parse(definitions(",\"rangedShot\":{\"indirectPenaltyPercent\":101}"))
    }

    @Test
    fun `shipped profiles reach standalone archers and crossbowmen`() {
        val preset = BalanceUnitSources.preset
        assertEquals(RangedShotProfile(3, 50), RangedShotProfile())
        assertEquals(RangedShotProfile(3, 50), preset.single { it.name == "Archer" }.rangedShot)
        val ruleset = BalanceUnitSources.ruleset(preset)
        assertEquals(
            setOf("Archer", "Crossbowman"),
            ruleset.units.values.filter { it.rangedShot != null }.map { it.name }.toSet()
        )
        for (name in listOf("Archer", "Crossbowman")) {
            val troop = StandaloneBattleSetup.army(
                listOf(StandaloneBattleSetup.Stack(name, 4)), ruleset
            ).getTroopAt(0)!!
            assertTrue(troop.isRanged)
            assertEquals(RangedShotProfile(3, 50), troop.rangedShot)
            val direct = com.unciv.pure.domain.battle.RangedShotRules.evaluate(
                troop.isRanged, troop.rangedShot, 3
            )
            val distant = com.unciv.pure.domain.battle.RangedShotRules.evaluate(
                troop.isRanged, troop.rangedShot, 4
            )
            val blocked = com.unciv.pure.domain.battle.RangedShotRules.evaluate(
                troop.isRanged, troop.rangedShot, 3,
                listOf(com.unciv.pure.domain.battle.RangedShotRules.Blocker())
            )
            assertEquals(100, com.unciv.pure.domain.battle.RangedShotRules.damage(100, direct))
            assertEquals(50, com.unciv.pure.domain.battle.RangedShotRules.damage(100, distant))
            assertEquals(50, com.unciv.pure.domain.battle.RangedShotRules.damage(100, blocked))
        }
    }
}
