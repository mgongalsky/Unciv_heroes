package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CreateBattleReportUseCase
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeArmy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateBattleReportUseCaseTest {
    private val source = HardcodedTroopDefinitionSource(
        speed = 5,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0,
        isSelfFeeding = false
    )

    private fun troop(name: String, amount: Int): Troop =
        TroopFactory.create(name, amount, source)

    @Test
    fun `reports losses from surviving stacks`() {
        val attackers = FakeArmy(troop("Spearman", 10))
        val defenders = FakeArmy(troop("Archer", 8))
        val initial = CreateBattleReportUseCase.capture(attackers, defenders)
        attackers.getAllTroops().filterNotNull().single().currentAmount = 6
        defenders.getAllTroops().filterNotNull().single().currentAmount = 3

        val report = CreateBattleReportUseCase.execute(initial, attackers, defenders, true)

        assertEquals(BattleSide.ATTACKER, report.winner)
        assertEquals(4, report.attackerLosses.single().amount)
        assertEquals(5, report.defenderLosses.single().amount)
    }

    @Test
    fun `reports a removed stack as completely lost`() {
        val destroyed = troop("Archer", 8)
        val attackers = FakeArmy(troop("Spearman", 10))
        val defenders = FakeArmy(destroyed)
        val initial = CreateBattleReportUseCase.capture(attackers, defenders)
        defenders.removeTroop(destroyed)

        val report = CreateBattleReportUseCase.execute(initial, attackers, defenders, true)

        assertEquals("Archer", report.defenderLosses.single().unitName)
        assertEquals(8, report.defenderLosses.single().amount)
    }

    @Test
    fun `omits troops without losses`() {
        val attackers = FakeArmy(troop("Spearman", 10))
        val defenders = FakeArmy(troop("Archer", 8))
        val initial = CreateBattleReportUseCase.capture(attackers, defenders)

        val report = CreateBattleReportUseCase.execute(initial, attackers, defenders, false)

        assertEquals(0, report.attackerLosses.size)
        assertEquals(0, report.defenderLosses.size)
    }

    @Test
    fun `supports mutual defeat`() {
        val attackers = FakeArmy(troop("Spearman", 10))
        val defenders = FakeArmy(troop("Archer", 8))
        val initial = CreateBattleReportUseCase.capture(attackers, defenders)

        val report = CreateBattleReportUseCase.execute(initial, attackers, defenders, null)

        assertNull(report.winner)
    }
}
