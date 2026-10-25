package com.unciv.testing.pure.domain.troop

import com.unciv.json.json
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test

class MovementFormationRulesetTest {
    @Test
    fun `custom ruleset capability reaches both troop factory paths`() {
        val unit = json().fromJson(BaseUnit::class.java,
            """{"name":"Custom rider","speed":7,"health":70,"damage":20,
                "formationHealthPercent":10,"formationDamageReductionPercent":25,
                "ignoresMovementFormationPenalty":true}""")
        val source = RulesetTroopDefinitionSource(Ruleset().apply { units[unit.name] = unit })
        assertTrue(source.ignoresMovementFormationPenalty(unit.name))
        assertFalse(source.ignoresMovementFormationPenalty("Missing"))
        val troops = listOf(
            TroopFactory.create(unit.name, 10, source),
            TroopFactory.createWithId(123, unit.name, 10, source)
        )
        troops.forEach {
            assertTrue(it.ignoresMovementFormationPenalty)
            assertTrue(it.hasFormation)
            assertEquals(70, it.formation.current)
        }
    }

    @Test
    fun `missing property keeps the ordinary movement rule`() {
        val unit = json().fromJson(BaseUnit::class.java, """{"name":"Infantry"}""")
        val source = RulesetTroopDefinitionSource(Ruleset().apply { units[unit.name] = unit })
        assertFalse(unit.ignoresMovementFormationPenalty)
        assertFalse(TroopFactory.createWithId(1, unit.name, 10, source).ignoresMovementFormationPenalty)
    }

    @Test
    fun `capability survives serialization without changing damaged combat state`() {
        for (immune in listOf(false, true)) {
            val troop = Troop("Custom rider", 10, 7, 20, 70,
                currentAmount = 7, currentHealth = 31,
                formationHealthPercent = 10, formationDamageReductionPercent = 25).apply {
                ignoresMovementFormationPenalty = immune
                formation.current = 17
            }
            val serializer = json()
            val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
            assertEquals(immune, restored.ignoresMovementFormationPenalty)
            assertEquals(7, restored.currentAmount)
            assertEquals(31, restored.currentHealth)
            assertEquals(troop.formation, restored.formation)
        }
    }
}
