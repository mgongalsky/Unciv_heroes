package com.unciv.testing.pure.application.battle

import com.unciv.logic.army.TroopInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.GetBattleResultUseCase
import com.unciv.testing.pure.fakes.FakeArmy
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GetBattleResultUseCaseTest {

    private fun aliveTroop() = TroopInfo(amount = 10, unitName = "Spearman").apply { currentAmount = 10 }
    private fun deadTroop() = TroopInfo(amount = 10, unitName = "Spearman").apply { currentAmount = 0 }

    @Before
    fun setUp() {
        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                health = 100
            }
        }
        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } })
        }
    }

    @After
    fun tearDown() = stopKoin()

    @Test
    fun `returns attacker wins when defenders are all dead`() {
        val result = GetBattleResultUseCase.execute(
            FakeArmy(listOf(aliveTroop())),
            FakeArmy(listOf(deadTroop()))
        )
        assertNotNull(result)
        assertTrue(result!!.winnerIsAttacker)
    }

    @Test
    fun `returns defender wins when attackers are all dead`() {
        val result = GetBattleResultUseCase.execute(
            FakeArmy(listOf(deadTroop())),
            FakeArmy(listOf(aliveTroop()))
        )
        assertNotNull(result)
        assertTrue(!result!!.winnerIsAttacker)
    }

    @Test
    fun `returns null when both armies have living troops`() {
        val result = GetBattleResultUseCase.execute(
            FakeArmy(listOf(aliveTroop())),
            FakeArmy(listOf(aliveTroop()))
        )
        assertNull(result)
    }
}
