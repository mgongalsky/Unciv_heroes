package com.unciv.testing.pure.domain.battle

import com.unciv.json.json
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.battle.RangedShotProfile
import com.unciv.pure.domain.battle.RangedShotRules
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test

class RangedShotRulesetTest {
    @Test
    fun `custom unit profile passes from JSON through factory to combat rule`() {
        val definition = json().fromJson(BaseUnit::class.java,
            """{"name":"Custom shooter","rangedStrength":7,"health":40,"damage":10,"rangedShot":{"directShotRange":4,"indirectPenaltyPercent":35}}""")
        val ruleset = Ruleset().apply { units[definition.name] = definition }
        val troop = TroopFactory.create(definition.name, 10, RulesetTroopDefinitionSource(ruleset))
        assertEquals(RangedShotProfile(4, 35), troop.rangedShot)
        assertEquals(-35, RangedShotRules.evaluate(troop.isRanged, troop.rangedShot, 5)!!.damageModifierPercent)
        assertEquals(0, RangedShotRules.evaluate(troop.isRanged, troop.rangedShot, 4)!!.damageModifierPercent)
    }

    @Test
    fun `unconfigured shooter and configured melee do not acquire shot modifiers`() {
        val ruleset = Ruleset().apply {
            units["Legacy shooter"] = BaseUnit().apply { name = "Legacy shooter"; rangedStrength = 7 }
            units["Melee"] = BaseUnit().apply { name = "Melee"; rangedShot = RangedShotProfile() }
        }
        val source = RulesetTroopDefinitionSource(ruleset)
        for (name in listOf("Legacy shooter", "Melee")) {
            val troop = TroopFactory.create(name, 10, source)
            assertNull(RangedShotRules.evaluate(troop.isRanged, troop.rangedShot, 5))
        }
        assertNull(source.getRangedShotProfile("Missing"))
    }

    @Test
    fun `profile survives production troop serialization without changing health`() {
        val troop = Troop("Custom shooter", 10, 5, 10, 40, rangedStrength = 7,
            currentAmount = 7, currentHealth = 31).apply { rangedShot = RangedShotProfile(4, 35) }
        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
        assertEquals(troop.rangedShot, restored.rangedShot)
        assertEquals(7, restored.currentAmount)
        assertEquals(31, restored.currentHealth)
    }
}
