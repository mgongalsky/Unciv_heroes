package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.IBattleTile
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.FlankingProfile
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class FlankingIntegrationTest {
    private class Scenario(val enabled: Boolean, formed: Boolean = true) {
        val field = TacticalBattleField((-3..3).flatMap { x ->
            (-3..3).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        val targetTile = field.getTileAt(Vector2(0f, 0f))!!
        val ring = targetTile.neighbors.filterIsInstance<IBattleTile>()
            .sortedWith(compareBy<IBattleTile> { it.toPoint().x }.thenBy { it.toPoint().y }).toList()
        val attackers = (1..4).map { id ->
            Troop(if (enabled) "Spearman" else "Peasant", 4, 10 - id, 50, 1000, id = id)
                .apply { flanking = if (enabled) FlankingProfile() else null }
        }
        val defender = Troop(
            if (formed) "Guard" else "Peasant", 4, 3, 10, 1000, id = 10,
            formationHealthPercent = if (formed) 10 else 0,
            formationDamageReductionPercent = if (formed) 50 else 0
        )
        val attackerArmy = Army(5).apply { attackers.forEachIndexed { i, troop -> setTroopAt(i, troop) } }
        val defenderArmy = Army(3).apply { setTroopAt(0, defender) }
        var randomCalls = 0
        private val random = SeededBattleRandom(1729L)
        val manager = BattleManager(attackerArmy, defenderArmy, field,
            object : IBattleRandom {
                override fun nextDouble(): Double { randomCalls++; return random.nextDouble() }
            }, moraleProbability = 0.0, luckProbability = 0.0
        )
        init {
            place(defender, targetTile)
            attackers.forEachIndexed { i, troop -> place(troop, ring[i]) }
            manager.initializeTurnQueue()
        }
        fun place(troop: Troop, tile: IBattleTile) {
            manager.getTroopTile(troop)?.clearTroop()
            tile.receiveTroop(troop)
            manager.setTroopPosition(troop, tile)
        }
        fun attack(onEvent: (BattleEvent) -> Unit = {}) = manager.execute(
            BattleCommand.Attack(attackers.first().id, Point(0, 0), ring.first().toPoint()), onEvent
        )
        fun simulate() = AIBattlePolicy(manager).let { policy ->
            BattleSimulationRunner(manager, policy, policy,
                maxTurns = 200, maxTurnsWithoutProgress = 20, seed = 1729L).run()
        }
    }

    @Test
    fun `command applies both bonuses and publishes the same breakdown after mutation`() {
        val s = Scenario(true)
        var observed: BattleEvent.TroopAttacked? = null
        val result = s.attack { event ->
            if (event is BattleEvent.TroopAttacked) {
                observed = event
                assertEquals(280, s.defender.formation.current)
                assertEquals(890, s.defender.currentHealth)
            }
        }
        assertTrue(result.success)
        assertEquals(FlankingCalculator.Result(4, 2, 10, 20, 20), result.attackFlanking)
        assertEquals(result.attackFlanking, observed!!.attackFlanking)
        assertEquals(3, s.randomCalls)
        // Guard has no flanking profile and cannot contribute to encircling the attacker.
        assertEquals(FlankingCalculator.Result(), result.retaliationFlanking)
    }

    @Test
    fun `formed target takes more damage only when surrounding troops have the property`() {
        val control = Scenario(false)
        val flanked = Scenario(true)
        assertTrue(control.attack().success)
        assertTrue(flanked.attack().success)
        assertEquals(900, control.defender.currentHealth)
        assertEquals(300, control.defender.formation.current)
        assertEquals(890, flanked.defender.currentHealth)
        assertEquals(280, flanked.defender.formation.current)
    }

    @Test
    fun `dead removed and unequipped neighbors stop contributing immediately`() {
        val s = Scenario(true)
        s.attackers[1].currentAmount = 0
        assertEquals(1, s.manager.getFlanking(s.attackers[0], s.defender).flankLevel)
        s.manager.removeTroop(s.attackers[2])
        assertEquals(0, s.manager.getFlanking(s.attackers[0], s.defender).flankLevel)
        s.attackers[3].flanking = null
        assertEquals(1, s.manager.getFlanking(s.attackers[0], s.defender).flankScore)
    }

    @Test
    fun `archer with an accidentally configured profile does not contribute`() {
        val s = Scenario(true)
        s.manager.removeTroop(s.attackers[3])
        val archer = Troop("Archer", 4, 3, 50, 1000, rangedStrength = 7, id = 20)
            .apply { flanking = FlankingProfile() }
        s.attackerArmy.setTroopAt(3, archer)
        s.place(archer, s.ring[3])
        assertEquals(3, s.manager.getFlanking(s.attackers[0], s.defender).flankScore)
        assertEquals(1, s.manager.getFlanking(s.attackers[0], s.defender).flankLevel)
        assertEquals(0, s.manager.getFlanking(archer, s.defender).formedDamageBonusPercent)
    }

    @Test
    fun `supporting defender neighbor reduces effective encirclement`() {
        val s = Scenario(true)
        s.manager.removeTroop(s.attackers[3])
        val support = Troop("Support", 4, 2, 10, 1000, id = 20, supportBonusPercent = 25)
        s.defenderArmy.setTroopAt(1, support)
        s.place(support, s.ring[4])
        assertEquals(2, s.manager.getFlanking(s.attackers[0], s.defender).flankScore)
        assertEquals(1, s.manager.getFlanking(s.attackers[0], s.defender).flankLevel)
        assertEquals(1, s.manager.getCurrentFlankingLevel(s.defender))
        val result = s.attack()
        assertTrue(result.success)
        assertEquals(FlankingCalculator.Result(2, 1, 5, 10, 10), result.attackFlanking)
        assertEquals(290, s.defender.formation.current)
        assertEquals(895, s.defender.currentHealth)
    }

    @Test
    fun `prospective position counts the attacker once without mutating state`() {
        val s = Scenario(true)
        val far = s.field.getTileAt(Vector2(-3f, -3f))!!
        s.place(s.attackers[0], far)
        val result = s.manager.getFlanking(s.attackers[0], s.defender, s.ring[0])
        assertEquals(4, result.flankScore)
        assertEquals(2, result.flankLevel)
        assertSame(far, s.manager.getTroopTile(s.attackers[0]))
        assertNull(s.ring[0].getTroop())
        assertEquals(0, s.randomCalls)
    }

    @Test
    fun `rejected attack does not mutate formation or publish a flanking bonus`() {
        val s = Scenario(true)
        val result = s.manager.execute(BattleCommand.Attack(
            s.attackers[0].id, s.ring[1].toPoint(), s.ring[0].toPoint()
        ))
        assertFalse(result.success)
        assertEquals(FlankingCalculator.Result(), result.attackFlanking)
        assertEquals(400, s.defender.formation.current)
        assertEquals(1000, s.defender.currentHealth)
    }

    @Test
    fun `peasant control finishes through the real simulator without any flanking bonus`() {
        val s = Scenario(false, formed = false)
        val result = s.simulate()
        assertEquals(BattleTermination.VICTORY, result.termination)
        assertTrue(result.turns in 1..200)
        val attacks = result.events.filterIsInstance<BattleEvent.TroopAttacked>()
        assertTrue(attacks.isNotEmpty())
        assertTrue(attacks.all { it.attackFlanking == FlankingCalculator.Result() })
        assertTrue(result.events.last() is BattleEvent.BattleEnded)
        assertTrue(result.events[result.events.lastIndex - 1] is BattleEvent.TroopAttacked)
    }

    @Test
    fun `flanking simulator scenario finishes and repeats its full breakdown transcript`() {
        val first = Scenario(true).simulate()
        val second = Scenario(true).simulate()
        assertEquals(BattleTermination.VICTORY, first.termination)
        assertTrue(first.turns in 1..200)
        assertEquals(first.commands, second.commands)
        assertEquals(first.events, second.events)
        assertEquals(first.turns, second.turns)
        assertEquals(first.winnerIsAttacker, second.winnerIsAttacker)
        val attack = first.events.filterIsInstance<BattleEvent.TroopAttacked>().first()
        assertEquals(FlankingCalculator.Result(4, 2, 10, 20, 20), attack.attackFlanking)
        assertTrue(first.events.last() is BattleEvent.BattleEnded)
    }

    @Test
    fun `current highlight level follows movement deaths and removal without random draws`() {
        val s = Scenario(true)
        assertEquals(2, s.manager.getCurrentFlankingLevel(s.defender))
        val far = s.field.getTileAt(Vector2(-3f, -3f))!!
        s.place(s.attackers[0], far)
        assertEquals(1, s.manager.getCurrentFlankingLevel(s.defender))
        assertEquals(2, s.manager.getFlanking(s.attackers[0], s.defender, s.ring[0]).flankLevel)
        assertEquals(1, s.manager.getCurrentFlankingLevel(s.defender))
        assertSame(far, s.manager.getTroopTile(s.attackers[0]))
        s.attackers[1].currentAmount = 0
        assertEquals(0, s.manager.getCurrentFlankingLevel(s.defender))
        s.manager.removeTroop(s.attackers[2])
        assertEquals(0, s.manager.getCurrentFlankingLevel(s.defender))
        assertEquals(0, s.randomCalls)
    }

    @Test
    fun `current highlight respects support and excludes dead targets and missing profiles`() {
        val control = Scenario(false)
        assertEquals(0, control.manager.getCurrentFlankingLevel(control.defender))
        val s = Scenario(true)
        s.manager.removeTroop(s.attackers[3])
        val support = Troop("Support", 4, 2, 10, 1000, id = 20, supportBonusPercent = 25)
        s.defenderArmy.setTroopAt(1, support)
        s.place(support, s.ring[4])
        assertEquals(1, s.manager.getCurrentFlankingLevel(s.defender))

        val secondSupport = Troop("Support", 4, 2, 10, 1000, id = 21, supportBonusPercent = 25)
        s.defenderArmy.setTroopAt(2, secondSupport)
        s.place(secondSupport, s.ring[5])
        assertEquals(0, s.manager.getCurrentFlankingLevel(s.defender))

        s.manager.removeTroop(secondSupport)
        val ordinaryAlly = Troop("Peasant", 4, 2, 10, 1000, id = 22, supportBonusPercent = 0)
        s.defenderArmy.setTroopAt(2, ordinaryAlly)
        s.place(ordinaryAlly, s.ring[5])
        assertEquals(1, s.manager.getCurrentFlankingLevel(s.defender))

        s.defender.currentAmount = 0
        assertEquals(0, s.manager.getCurrentFlankingLevel(s.defender))
        assertEquals(0, s.randomCalls)
    }

    @Test
    fun `preview flanking and formation losses match execution from a prospective position`() {
        val s = Scenario(true)
        val attacker = s.attackers.first()
        attacker.flanking = FlankingProfile(17, 29, 43)
        val original = s.field.getTileAt(Vector2(-3f, -3f))!!
        s.place(attacker, original)
        val preview =
                requireNotNull(s.manager.getAttackPreview(attacker, s.defender, s.ring.first()))
        assertEquals(34, preview.attack.flanking.formedDamageBonusPercent)
        assertEquals(58, preview.attack.flanking.formationDamageBonusPercent)
        assertSame(original, s.manager.getTroopTile(attacker))
        assertEquals(0, s.randomCalls)
        val result = s.attack()
        assertTrue(result.success)
        assertEquals(preview.attack.flanking, result.attackFlanking)
        assertEquals(preview.attack.result.remainingHealth, s.defender.currentHealth)
        assertEquals(preview.attack.remainingFormation, s.defender.formation.current)
    }

    @Test
    fun `retaliation flanking is forecast around the approach rather than the original position`() {
        val s = Scenario(true)
        val attacker = s.attackers.first()
        val approach = s.ring.first()
        s.attackers.drop(1).forEach { s.manager.removeTroop(it) }
        val original = s.field.getTileAt(Vector2(-3f, -3f))!!
        s.place(attacker, original)
        s.defender.flanking = FlankingProfile(11, 23, 37)
        val available = approach.neighbors.filterIsInstance<IBattleTile>()
            .filter { it.getTroop() == null && it !== original }.take(2).toList()
        assertEquals(2, available.size)
        available.forEachIndexed { index, tile ->
            val enemy = Troop("Spearman", 4, 2, 10, 1000, id = 30 + index)
                .apply { flanking = FlankingProfile() }
            s.defenderArmy.setTroopAt(index + 1, enemy)
            s.place(enemy, tile)
        }
        val preview = requireNotNull(s.manager.getAttackPreview(attacker, s.defender, approach))
        assertTrue(preview.retaliation!!.flanking.unformedDamageBonusPercent > 0)
        assertEquals(0, s.manager.getFlanking(s.defender, attacker).flankLevel)
        assertEquals(0, s.randomCalls)
        val result = s.attack()
        assertTrue(result.success)
        assertEquals(preview.retaliation!!.flanking, result.retaliationFlanking)
        assertEquals(preview.retaliation!!.result.remainingHealth, attacker.currentHealth)
    }
}
