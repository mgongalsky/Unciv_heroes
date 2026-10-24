package com.unciv.testing.pure.application.battle.session

import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.session.BattleSessionSetup
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.session.BattleSessionRequest
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Test

class BattleSessionSetupTest {
    private fun source() = RulesetTroopDefinitionSource(Ruleset().apply {
        units["Soldier"] = BaseUnit().apply {
            name = "Soldier"
            speed = 5
            health = 100
            damage = 10
            formationHealthPercent = 20
            formationDamageReductionPercent = 50
            supportBonusPercent = 10
        }
    })

    private fun session() = BattleSessionSetup.create(
        BattleSessionSetup.Side(listOf(BattleSessionSetup.Stack("Soldier", 10))),
        BattleSessionSetup.Side(listOf(BattleSessionSetup.Stack("Soldier", 10))),
        source(), TacticalBattleField.rectangular(14, 8) { false }, SeededBattleRandom(42),
        luckProbability = 0.0, moraleProbability = 0.0
    )

    @Test
    fun roomsHaveStableLocalIdsAndIndependentState() {
        val first = session()
        val second = session()
        val before = second.read(BattleSide.ATTACKER)
        assertEquals(listOf(1, 2), before.state.troops.map { it.id })
        assertEquals(first.read(BattleSide.ATTACKER).state, before.state)
        val reply = first.submit(BattleSide.ATTACKER, BattleSessionRequest(1, 0, BattleCommand.Skip(1)))
        assertTrue(reply.result!!.success)
        assertEquals(before, second.read(BattleSide.ATTACKER))
        assertEquals(1L, first.read(BattleSide.ATTACKER).revision)
    }

    @Test
    fun explicitIdsDoNotConsumeLegacyIdsAndPreserveCombatDefinitions() {
        val source = source()
        val before = TroopFactory.create("Soldier", 10, source)
        val isolated = TroopFactory.createWithId(12345, "Soldier", 10, source)
        val after = TroopFactory.create("Soldier", 10, source)
        assertEquals(before.id + 1, after.id)
        assertEquals(12345, isolated.id)
        assertEquals(before.damage, isolated.damage)
        assertEquals(before.speed, isolated.speed)
        assertEquals(before.maxHealth, isolated.maxHealth)
        assertEquals(before.formation, isolated.formation)
        assertNotSame(before.formation, isolated.formation)
        assertEquals(before.supportBonusPercent, isolated.supportBonusPercent)
    }
}
