package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.StandaloneBattleSetup
import com.unciv.logic.battle.execute
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.battle.RangedShotProfile
import com.unciv.pure.domain.battle.RangedShotRules.IndirectReason
import com.unciv.pure.domain.battle.RangedShotRules.ShotType
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class DirectIndirectShotIntegrationTest {
    private class Scenario(
        blockerSide: String? = null,
        targetDistance: Int = 3,
        profile: RangedShotProfile? = RangedShotProfile(3, 20),
        formedTarget: Boolean = false
    ) {
        val ruleset = Ruleset().apply {
            units["Shooter"] = BaseUnit().apply {
                name = "Shooter"; speed = 10; health = 100; damage = 10
                rangedStrength = 7; rangedShot = profile
            }
            units["Target"] = BaseUnit().apply {
                name = "Target"; speed = 1; health = 100; damage = 1
                if (formedTarget) {
                    formationHealthPercent = 100
                    formationDamageReductionPercent = 50
                }
            }
            units["Blocker"] = BaseUnit().apply {
                name = "Blocker"; speed = 2; health = 100; damage = 1
            }
        }
        val attackers = StandaloneBattleSetup.army(
            listOf(StandaloneBattleSetup.Stack("Shooter", 10)) +
                if (blockerSide == "ally") listOf(StandaloneBattleSetup.Stack("Blocker", 1)) else emptyList(),
            ruleset
        )
        val defenders = StandaloneBattleSetup.army(
            listOf(StandaloneBattleSetup.Stack("Target", 10)) +
                if (blockerSide == "enemy") listOf(StandaloneBattleSetup.Stack("Blocker", 1)) else emptyList(),
            ruleset
        )
        val shooter = attackers.getTroopAt(0)!!
        val target = defenders.getTroopAt(0)!!
        val blocker = when (blockerSide) {
            "ally" -> attackers.getTroopAt(1)
            "enemy" -> defenders.getTroopAt(1)
            else -> null
        }
        val field = TacticalBattleField((-3..6).flatMap { x ->
            (-3..3).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        val manager = BattleManager(attackers, defenders, field, SeededBattleRandom(42L),
            moraleProbability = 0.0, luckProbability = 0.0)

        fun place(troop: Troop, point: Point) {
            manager.getTroopTile(troop)?.clearTroop()
            val tile = field.getTileAt(Vector2(point.x.toFloat(), point.y.toFloat()))!!
            tile.receiveTroop(troop)
            manager.setTroopPosition(troop, tile)
        }

        init {
            place(shooter, Point(0, 0))
            place(target, Point(targetDistance, 0))
            blocker?.let { place(it, Point(2, 0)) }
            manager.initializeTurnQueue()
        }

        fun health(troop: Troop): Int =
            if (troop.currentAmount <= 0) 0 else (troop.currentAmount - 1) * troop.maxHealth + troop.currentHealth

        fun shoot(): Pair<com.unciv.pure.application.battle.BattleCommandResult, List<BattleEvent>> {
            val events = mutableListOf<BattleEvent>()
            val result = manager.execute(BattleCommand.Shoot(shooter.id, manager.getTroopTile(target)!!.toPoint())) {
                events.add(it)
            }
            return result to events
        }
    }

    @Test
    fun `clear direct shot matches legacy damage and returns the preview breakdown`() {
        val direct = Scenario()
        val legacy = Scenario(profile = null)
        val preview = direct.manager.getRangedShot(direct.shooter, direct.target)
        val (result, events) = direct.shoot()
        legacy.shoot()
        assertTrue(result.success)
        assertEquals(ShotType.DIRECT, result.rangedShot!!.shotType)
        assertEquals(preview, result.rangedShot)
        assertEquals(result.rangedShot, events.filterIsInstance<BattleEvent.TroopShot>().single().rangedShot)
        assertEquals(900, direct.health(direct.target))
        assertEquals(legacy.health(legacy.target), direct.health(direct.target))
    }

    @Test
    fun `friendly and enemy intermediate troops both reduce shot damage`() {
        for (side in listOf("ally", "enemy")) {
            val s = Scenario(blockerSide = side)
            assertTrue(s.manager.canShoot(s.shooter))
            val blockerHealth = s.health(s.blocker!!)
            val (result, events) = s.shoot()
            assertTrue(result.success)
            val shot = requireNotNull(result.rangedShot)
            assertEquals(ShotType.INDIRECT, shot.shotType)
            assertEquals(IndirectReason.BLOCKED_LINE, shot.reason)
            assertEquals(-20, shot.damageModifierPercent)
            assertEquals(920, s.health(s.target))
            assertEquals(blockerHealth, s.health(s.blocker))
            assertEquals(shot, events.filterIsInstance<BattleEvent.TroopShot>().single().rangedShot)
        }
    }

    @Test
    fun `long range always reports distance regardless of blockers`() {
        for (side in listOf(null, "ally", "enemy")) {
            val s = Scenario(blockerSide = side, targetDistance = 4)
            val (result, _) = s.shoot()
            assertTrue(result.success)
            val shot = requireNotNull(result.rangedShot)
            assertEquals(IndirectReason.DISTANCE, shot.reason)
            assertNull(shot.lineBlocked)
            assertEquals(920, s.health(s.target))
        }
    }

    @Test
    fun `off line and dead troops do not downgrade a direct shot`() {
        val s = Scenario(blockerSide = "ally")
        s.place(s.blocker!!, Point(1, 1))
        assertEquals(ShotType.DIRECT, s.manager.getRangedShot(s.shooter, s.target)!!.shotType)
        s.place(s.blocker, Point(2, 0))
        s.blocker.currentAmount = 0
        assertEquals(ShotType.DIRECT, s.manager.getRangedShot(s.shooter, s.target)!!.shotType)
    }

    @Test
    fun `indirect penalty applies before formation absorption`() {
        val s = Scenario(blockerSide = "ally", formedTarget = true)
        val formationBefore = s.target.formation.current
        assertTrue(s.shoot().first.success)
        assertEquals(40, formationBefore - s.target.formation.current)
        assertEquals(960, s.health(s.target))
    }

    @Test
    fun `AI breaks equal target priorities using the shared shot modifier without mutation`() {
        val s = Scenario(blockerSide = "enemy")
        // The blocker is faster, so first preserve the existing speed priority.
        assertEquals(BattleCommand.Shoot(s.shooter.id, Point(2, 0)),
            AIBattlePolicy(s.manager).chooseCommand(s.shooter.id))
        val extra = Troop("Equal target", 10, 1, 1, 100, id = -100)
        s.defenders.setTroopAt(1, extra)
        s.place(extra, Point(0, 3))
        // Move the original target beyond direct range; both candidates now have equal priorities.
        s.place(s.target, Point(4, 0))
        val before = s.health(s.target)
        assertEquals(BattleCommand.Shoot(s.shooter.id, Point(0, 3)),
            AIBattlePolicy(s.manager).chooseCommand(s.shooter.id))
        assertEquals(before, s.health(s.target))
        assertEquals(Point(0, 0), s.manager.getTroopTile(s.shooter)!!.toPoint())
    }

    @Test
    fun `clear and blocked simulator scenarios use real combat and reproduce their transcripts`() {
        for (side in listOf(null, "ally")) {
            fun run(): Pair<com.unciv.pure.application.battle.BattleSimulationResult, Int> {
                val s = Scenario(blockerSide = side)
                val policy = AIBattlePolicy(s.manager)
                val result = BattleSimulationRunner(s.manager, policy, policy,
                    maxTurns = 1, maxTurnsWithoutProgress = 10, seed = 42L).run()
                return result to s.health(s.target)
            }
            val first = run()
            val second = run()
            val shot = first.first.events.filterIsInstance<BattleEvent.TroopShot>().single()
            assertEquals(if (side == null) ShotType.DIRECT else ShotType.INDIRECT, shot.rangedShot!!.shotType)
            assertEquals(if (side == null) 900 else 920, first.second)
            assertEquals(first.first.termination, second.first.termination)
            assertEquals(first.first.turns, second.first.turns)
            assertEquals(first.second, second.second)
            assertEquals(shot.rangedShot,
                second.first.events.filterIsInstance<BattleEvent.TroopShot>().single().rangedShot)
        }
    }

    @Test
    fun `configured direct and blocked scenarios finish full battles reproducibly`() {
        for (side in listOf(null, "ally")) {
            fun simulate(): Pair<com.unciv.pure.application.battle.BattleSimulationResult, List<String>> {
                val s = Scenario(blockerSide = side)
                val names = listOfNotNull(
                    s.shooter,
                    s.target,
                    s.blocker
                ).associate { it.id to it.unitName }
                val policy = AIBattlePolicy(s.manager)
                val result = BattleSimulationRunner(
                    s.manager, policy, policy,
                    maxTurns = 200, maxTurnsWithoutProgress = 30, seed = 42L
                ).run()
                val transcript = result.events.map { event ->
                    when (event) {
                        is BattleEvent.TroopShot ->
                            "shot:${names[event.attackerId]}:${names[event.defenderId]}:${event.defenderRemainingAmount}:${event.isLuck}:${event.isMorale}:${event.defenderDied}:${event.rangedShot}"

                        is BattleEvent.TroopAttacked ->
                            "attack:${names[event.attackerId]}:${names[event.defenderId]}:${event.defenderRemainingAmount}:${event.isLuck}:${event.isMorale}:${event.defenderDied}:${event.attackFlanking}:${event.retaliationFlanking}"

                        is BattleEvent.TroopMoved ->
                            "move:${names[event.troopId]}:${event.from}:${event.to}:${event.isMorale}"

                        is BattleEvent.TurnAdvanced -> "turn:${names[event.nextTroopId]}"
                        is BattleEvent.BattleEnded -> "end:${event.winnerIsAttacker}"
                        BattleEvent.TurnSkipped -> "skip"
                    }
                }
                return result to transcript
            }
            val (first, firstTranscript) = simulate()
            val (second, secondTranscript) = simulate()
            assertEquals(
                com.unciv.pure.application.battle.BattleTermination.VICTORY,
                first.termination
            )
            assertTrue(first.turns in 2..200)
            assertTrue(first.events.last() is BattleEvent.BattleEnded)
            assertEquals(
                if (side == null) ShotType.DIRECT else ShotType.INDIRECT,
                first.events.filterIsInstance<BattleEvent.TroopShot>().first().rangedShot!!.shotType
            )
            assertEquals(first.termination, second.termination)
            assertEquals(first.winnerIsAttacker, second.winnerIsAttacker)
            assertEquals(first.turns, second.turns)
            assertEquals(firstTranscript, secondTranscript)
        }
    }
}
