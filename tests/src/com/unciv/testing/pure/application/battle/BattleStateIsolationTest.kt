package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.configureEffectProbabilities
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class BattleStateIsolationTest {
    private data class Fixture(val manager: BattleManager, val attacker: Troop, val defender: Troop)

    private fun fixture(): Fixture {
        // Deliberately reuse IDs across battles, without touching the global factory counter.
        val attacker = Troop(
            unitName = "Attacker", amount = 10, speed = 5,
            damage = 10, maxHealth = 1000, id = 1
        )
        val defender = Troop(
            unitName = "Defender", amount = 10, speed = 4,
            damage = 10, maxHealth = 1000, id = 2
        )

        fun army(troop: Troop): IArmy {
            val army = Army(1).apply { addTroop(troop) }
            return object : IArmy by army {
                override fun getBattleLuck() = 3
                override fun getBattleMorale() = 3
            }
        }

        val start = FakeBattleTile(Vector2(0f, 0f))
        val target = FakeBattleTile(Vector2(1f, 0f))
        start.addNeighbor(target)
        target.addNeighbor(start)
        val manager = BattleManager(
            army(attacker), army(defender),
            FakeBattleField(listOf(start, target)),
            object : IBattleRandom {
                override fun nextDouble() = 0.5
            },
            moraleProbability = 0.0, luckProbability = 0.0
        )
        start.receiveTroop(attacker)
        target.receiveTroop(defender)
        manager.setTroopPosition(attacker, start)
        manager.setTroopPosition(defender, target)
        manager.initializeTurnQueue()
        return Fixture(manager, attacker, defender)
    }

    private fun attack(fixture: Fixture) = fixture.manager.execute(
        BattleCommand.Attack(fixture.attacker.id, Point(1, 0), Point(0, 0))
    ).also { assertTrue(it.success) }

    @Test
    fun retaliationIsIndependentForBattlesWithIdenticalTroopIds() {
        val first = fixture()
        val second = fixture()
        attack(first)
        assertEquals(900, first.attacker.currentHealth)
        assertFalse(first.manager.hasRetaliationRemaining(first.defender))
        assertTrue(second.manager.hasRetaliationRemaining(second.defender))
        assertEquals(1000, second.attacker.currentHealth)
        attack(second)
        assertEquals(900, second.attacker.currentHealth)
        attack(first)
        assertEquals(900, first.attacker.currentHealth)
        assertEquals(900, second.attacker.currentHealth)
    }

    @Test
    fun probabilityOverridesAndTheirResetAffectOnlyTheirBattle() {
        val first = fixture()
        val second = fixture()
        first.manager.configureEffectProbabilities(1.0, 1.0)
        second.manager.configureEffectProbabilities(0.0, 0.0)
        val boosted = attack(first)
        assertTrue(boosted.isLuck)
        assertTrue(boosted.isMorale)
        val ordinary = attack(second)
        assertFalse(ordinary.isLuck)
        assertFalse(ordinary.isMorale)
        second.manager.configureEffectProbabilities(1.0, 1.0)
        first.manager.configureEffectProbabilities(null, null)
        val reset = attack(first)
        assertFalse(reset.isLuck)
        assertFalse(reset.isMorale)
        val stillBoosted = attack(second)
        assertTrue(stillBoosted.isLuck)
        assertTrue(stillBoosted.isMorale)
    }
}
