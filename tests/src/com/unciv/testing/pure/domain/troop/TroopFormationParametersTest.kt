package com.unciv.testing.pure.domain.troop

import com.unciv.json.json
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.application.battle.ApplyFormationDamageUseCase
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class TroopFormationParametersTest {
    private fun create(fields: String, amount: Int = 3): Troop {
        val unit = json().fromJson(
            BaseUnit::class.java,
            """{"name":"Test","health":15,$fields}"""
        )
        val ruleset = Ruleset().apply { units[unit.name] = unit }
        return TroopFactory.create(unit.name, amount, RulesetTroopDefinitionSource(ruleset))
    }

    @Test
    fun `capacity uses total maximum health and rounds down once`() {
        val troop = create("\"formationHealthPercent\":50,\"formationDamageReductionPercent\":60")
        assertEquals(22, troop.formation.maximum)
        assertEquals(22, troop.formation.current)
        assertEquals(60, troop.formationDamageReductionPercent)
    }

    @Test
    fun `missing either parameter disables formation`() {
        for (fields in listOf(
            "\"speed\":3", "\"formationHealthPercent\":50",
            "\"formationDamageReductionPercent\":60"
        )) {
            val troop = create(fields)
            troop.restoreFormationIfMissing()
            assertEquals(0, troop.formation.maximum)
            assertEquals(0, troop.formation.current)
        }
    }

    @Test
    fun `zero capacity or zero protection disables formation`() {
        for (fields in listOf(
            "\"formationHealthPercent\":0,\"formationDamageReductionPercent\":60",
            "\"formationHealthPercent\":50,\"formationDamageReductionPercent\":0"
        )) {
            assertEquals(0, create(fields).formation.maximum)
        }
    }

    @Test
    fun `empty troop has no formation`() {
        assertEquals(
            0,
            create(
                "\"formationHealthPercent\":50,\"formationDamageReductionPercent\":60",
                0
            ).formation.maximum
        )
    }

    @Test
    fun `configured protection absorbs damage only up to remaining capacity`() {
        val intact = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(100, 100, 60, 100)
        )
        assertEquals(60, intact.absorbedByFormation)
        assertEquals(40, intact.damageToSoldiers)
        val exhausted = ApplyFormationDamageUseCase.execute(
            ApplyFormationDamageUseCase.Input(100, 22, 60, 100)
        )
        assertEquals(22, exhausted.absorbedByFormation)
        assertEquals(78, exhausted.damageToSoldiers)
        assertEquals(0, exhausted.remainingFormation)
    }

    @Test
    fun `configured formation and its damage survive serialization without repair`() {
        val troop = create("\"formationHealthPercent\":50,\"formationDamageReductionPercent\":60")
        for (remaining in listOf(7, 0)) {
            troop.formation.current = remaining
            val serializer = json()
            val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
            restored.restoreFormationIfMissing()
            assertEquals(50, restored.formationHealthPercent)
            assertEquals(60, restored.formationDamageReductionPercent)
            assertEquals(22, restored.formation.maximum)
            assertEquals(remaining, restored.formation.current)
        }
    }
}
