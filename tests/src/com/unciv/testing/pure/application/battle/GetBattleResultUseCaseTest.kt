package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.GetBattleResultUseCase
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeArmy
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetBattleResultUseCaseTest {

    private val source = HardcodedTroopDefinitionSource(
        speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0, isSelfFeeding = false
    )

    private fun aliveTroop(): Troop =
            TroopFactory.create("Spearman", 10, source).also { it.currentAmount = 10 }

    private fun deadTroop(): Troop =
            TroopFactory.create("Spearman", 10, source).also { it.currentAmount = 0 }

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
