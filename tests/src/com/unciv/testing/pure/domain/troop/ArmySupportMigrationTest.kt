package com.unciv.testing.pure.domain.troop

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArmySupportMigrationTest {
    @Before
    fun setUp() {
        val ruleset = Ruleset().apply {
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                supportBonusPercent = 25
            }
        }
        startKoin {
            modules(module {
                single<ITroopDefinitionSource> { RulesetTroopDefinitionSource(ruleset) }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun restore(extra: String = ""): com.unciv.pure.domain.troop.Troop {
        val army = ArmyInfo(FakeCivilizationInfo(), 1)
        army.read(
            json(), JsonReader().parse(
                """{"slots":[{
            "unitName":"Spearman","amount":10,"speed":5,"damage":10,
            "maxHealth":40,"currentAmount":7,"currentHealth":31,"id":123,
            "formationHealthPercent":50,"formationDamageReductionPercent":60,
            "formation":{"current":17,"maximum":200}$extra
        }]}"""
            )
        )
        return army.getTroopAt(0)!!
    }

    @Test
    fun `old army gains support from rules without healing or rebuilding troops`() {
        val troop = restore()
        assertEquals(25, troop.supportBonusPercent)
        assertEquals(123, troop.id)
        assertEquals(10, troop.amount)
        assertEquals(7, troop.currentAmount)
        assertEquals(31, troop.currentHealth)
        assertEquals(10, troop.damage)
        assertEquals(17, troop.formation.current)
        assertEquals(200, troop.formation.maximum)
    }

    @Test
    fun `explicit saved support configuration is preserved`() {
        for (percent in listOf(0, 15, 25)) {
            assertEquals(percent, restore(",\"supportBonusPercent\":$percent").supportBonusPercent)
        }
    }
}
