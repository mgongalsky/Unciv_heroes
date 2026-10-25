package com.unciv.testing.pure.domain.troop

import com.unciv.json.json
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.battle.AntiCavalryDamageRules
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test

class AntiCavalryRulesetTest {
    @Test fun jsonPropertiesReachFactoryAndDamageRuleWithoutUnitNameChecks() {
        val serializer = json()
        val pikes = serializer.fromJson(BaseUnit::class.java,
            """{"name":"Custom guard","unitType":"Sword","health":100,"damage":10,"antiCavalryDamageBonusPercent":50}""")
        val mounted = serializer.fromJson(BaseUnit::class.java,
            """{"name":"Custom rider","unitType":"Mounted","health":100,"damage":10}""")
        val ruleset = Ruleset().apply {
            units[pikes.name] = pikes
            units[mounted.name] = mounted
        }
        val source = RulesetTroopDefinitionSource(ruleset)
        val guard = TroopFactory.create(pikes.name, 10, source)
        val rider = TroopFactory.createWithId(900, mounted.name, 10, source)
        assertEquals(50, guard.antiCavalryDamageBonusPercent)
        assertFalse(guard.isCavalry)
        assertTrue(rider.isCavalry)
        assertEquals(0, rider.antiCavalryDamageBonusPercent)
        assertEquals(150, AntiCavalryDamageRules.damage(
            guard.currentAmount * guard.damage, guard.antiCavalryDamageBonusPercent, rider.isCavalry
        ))
    }

    @Test fun absentTypeAndAbsentBonusPreserveLegacyDefaults() {
        val ruleset = Ruleset().apply {
            units["Legacy"] = BaseUnit().apply { name = "Legacy"; health = 20 }
        }
        val source = RulesetTroopDefinitionSource(ruleset)
        val troop = TroopFactory.create("Legacy", 5, source)
        assertFalse(troop.isCavalry)
        assertEquals(0, troop.antiCavalryDamageBonusPercent)
        assertFalse(source.isCavalry("Missing"))
        assertEquals(0, source.getAntiCavalryDamageBonusPercent("Missing"))
    }

    @Test fun serializationPreservesCapabilitiesAndWoundedState() {
        val troop = Troop("Custom", 10, 4, 10, 100,
            currentAmount = 7, currentHealth = 31,
            formationHealthPercent = 20, formationDamageReductionPercent = 50).apply {
            isCavalry = true
            antiCavalryDamageBonusPercent = 35
            formation.current = 12
        }
        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
        assertTrue(restored.isCavalry)
        assertEquals(35, restored.antiCavalryDamageBonusPercent)
        assertEquals(7, restored.currentAmount)
        assertEquals(31, restored.currentHealth)
        assertEquals(troop.formation.maximum, restored.formation.maximum)
        assertEquals(12, restored.formation.current)
    }

    @Test fun negativeRulesetBonusIsRejected() {
        val ruleset = Ruleset().apply {
            units["Invalid"] = BaseUnit().apply {
                name = "Invalid"
                antiCavalryDamageBonusPercent = -1
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            TroopFactory.create("Invalid", 1, RulesetTroopDefinitionSource(ruleset))
        }
    }
}
