package com.unciv.testing.pure.domain.troop

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArmyAntiCavalryMigrationTest {
    @Before
    fun setUp() {
        val ruleset = Ruleset().apply {
            units["Guard"] = BaseUnit().apply {
                name = "Guard"
                unitType = "Mounted"
                antiCavalryDamageBonusPercent = 50
            }
        }
        startKoin { modules(module {
            single<ITroopDefinitionSource> { RulesetTroopDefinitionSource(ruleset) }
        }) }
    }

    @After
    fun tearDown() { stopKoin() }

    private fun restore(extra: String = ""): Troop {
        val army = ArmyInfo(FakeCivilizationInfo(), 1)
        army.read(json(), JsonReader().parse(
            """{"slots":[{"unitName":"Guard","amount":10,"speed":7,"damage":20,
                "maxHealth":70,"currentAmount":7,"currentHealth":31,"id":123,
                "supportBonusPercent":0,"formationHealthPercent":10,"formationDamageReductionPercent":25,
                "formation":{"current":17,"maximum":70}$extra}]}"""
        ))
        return army.getTroopAt(0)!!
    }

    @Test
    fun missingPropertiesComeFromRulesetWithoutResettingCombatState() {
        val troop = restore()
        assertTrue(troop.isCavalry)
        assertEquals(50, troop.antiCavalryDamageBonusPercent)
        assertEquals(123, troop.id)
        assertEquals(10, troop.amount)
        assertEquals(7, troop.currentAmount)
        assertEquals(31, troop.currentHealth)
        assertEquals(17, troop.formation.current)
        assertEquals(70, troop.formation.maximum)
    }

    @Test
    fun explicitZeroAndFalseOverrideRuleset() {
        val troop = restore(",\"isCavalry\":false,\"antiCavalryDamageBonusPercent\":0")
        assertFalse(troop.isCavalry)
        assertEquals(0, troop.antiCavalryDamageBonusPercent)
    }

    @Test
    fun explicitCustomBonusOverridesRuleset() {
        val troop = restore(",\"isCavalry\":true,\"antiCavalryDamageBonusPercent\":35")
        assertTrue(troop.isCavalry)
        assertEquals(35, troop.antiCavalryDamageBonusPercent)
    }
}
