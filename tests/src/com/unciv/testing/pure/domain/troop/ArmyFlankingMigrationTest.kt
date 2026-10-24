package com.unciv.testing.pure.domain.troop

import com.badlogic.gdx.utils.JsonReader
import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.battle.FlankingProfile
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

class ArmyFlankingMigrationTest {
    @Before
    fun setUp() {
        val ruleset = Ruleset().apply {
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                flanking = FlankingProfile()
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
            """{"slots":[{
                "unitName":"Spearman","amount":10,"speed":5,"damage":10,
                "maxHealth":40,"currentAmount":7,"currentHealth":31,"id":123,
                "supportBonusPercent":0,
                "formationHealthPercent":50,"formationDamageReductionPercent":60,
                "formation":{"current":17,"maximum":200}$extra
            }]}"""
        ))
        return army.getTroopAt(0)!!
    }

    @Test
    fun `old snapshot gains the ruleset profile without changing combat state`() {
        val troop = restore()
        assertEquals(FlankingProfile(), troop.flanking)
        assertEquals(123, troop.id)
        assertEquals(10, troop.amount)
        assertEquals(7, troop.currentAmount)
        assertEquals(31, troop.currentHealth)
        assertEquals(17, troop.formation.current)
        assertEquals(200, troop.formation.maximum)
        assertEquals(0, troop.supportBonusPercent)
    }

    @Test
    fun `explicit null profile is preserved`() {
        assertNull(restore(",\"flanking\":null").flanking)
    }

    @Test
    fun `explicit stronger saved profile is preserved`() {
        val troop = restore(",\"flanking\":{\"formedDamagePercent\":5,\"formationDamagePercent\":10,\"unformedDamagePercent\":15}")
        assertEquals(FlankingProfile(unformedDamagePercent = 15), troop.flanking)
    }

    @Test
    fun `profile survives the production troop JSON round trip`() {
        val troop = restore().apply { flanking = FlankingProfile(7, 12, 15) }
        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
        assertEquals(troop.flanking, restored.flanking)
        assertEquals(troop.currentAmount, restored.currentAmount)
        assertEquals(troop.currentHealth, restored.currentHealth)
        assertEquals(troop.formation, restored.formation)
    }
}
