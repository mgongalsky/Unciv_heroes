package com.unciv.testing.pure.domain.troop

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.battle.RangedShotProfile
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

class ArmyRangedShotMigrationTest {
    @Before
    fun setUp() {
        val ruleset = Ruleset().apply {
            units["Custom shooter"] = BaseUnit().apply {
                name = "Custom shooter"
                rangedStrength = 7
                rangedShot = RangedShotProfile(3, 20)
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
            """{"slots":[{"unitName":"Custom shooter","amount":10,"speed":5,"damage":10,
                "maxHealth":40,"rangedStrength":7,"currentAmount":7,"currentHealth":31,"id":123,
                "supportBonusPercent":0,"formationHealthPercent":50,"formationDamageReductionPercent":60,
                "formation":{"current":17,"maximum":200}$extra}]}"""
        ))
        return army.getTroopAt(0)!!
    }

    @Test
    fun `old snapshot gains profile without changing combat state`() {
        val troop = restore()
        assertEquals(RangedShotProfile(3, 20), troop.rangedShot)
        assertEquals(123, troop.id)
        assertEquals(10, troop.amount)
        assertEquals(7, troop.currentAmount)
        assertEquals(31, troop.currentHealth)
        assertEquals(17, troop.formation.current)
        assertEquals(200, troop.formation.maximum)
    }

    @Test
    fun `explicit null remains disabled`() {
        assertNull(restore(",\"rangedShot\":null").rangedShot)
    }

    @Test
    fun `saved custom profile takes precedence over ruleset`() {
        assertEquals(RangedShotProfile(5, 10), restore(
            ",\"rangedShot\":{\"directShotRange\":5,\"indirectPenaltyPercent\":10}"
        ).rangedShot)
    }
}
