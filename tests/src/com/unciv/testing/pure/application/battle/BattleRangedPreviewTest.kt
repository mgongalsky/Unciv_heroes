package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.*
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

class BattleRangedPreviewTest {
    private class Scenario(profile: RangedShotProfile?) {
        val shooter = Troop("Archer", 4, 6, 10, 100, rangedStrength = 7, id = 1,
            supportBonusPercent = 30).apply { rangedShot = profile }
        val ally = Troop("Guard", 4, 4, 10, 100, id = 2, supportBonusPercent = 30)
        val target = Troop("Enemy", 4, 3, 10, 100, id = 3,
            formationHealthPercent = 10, formationDamageReductionPercent = 40)
        val field = TacticalBattleField((-5..5).flatMap { x ->
            (-5..5).map { y -> TacticalBattleField.Cell(Point(x, y), false) }
        })
        var draws = 0
        val manager = BattleManager(
            Army(2).apply { setTroopAt(0, shooter); setTroopAt(1, ally) },
            Army(1).apply { setTroopAt(0, target) }, field,
            object : IBattleRandom {
                override fun nextDouble(): Double { draws++; return 1.0 }
            }, moraleProbability = 0.0, luckProbability = 0.0
        )
        init {
            place(shooter, 0, 0); place(ally, -1, 0); place(target, 3, 0)
            manager.initializeTurnQueue()
        }
        fun place(troop: Troop, x: Int, y: Int) {
            manager.getTroopTile(troop)?.clearTroop()
            val tile = field.getTileAt(Vector2(x.toFloat(), y.toFloat()))!!
            tile.receiveTroop(troop)
            manager.setTroopPosition(troop, tile)
        }
        fun verifyShot() {
            val before = draws
            val forecast = requireNotNull(manager.getAttackPreview(shooter, target))
            assertTrue(forecast.isRanged)
            assertNull(forecast.retaliation)
            assertEquals(before, draws)
            assertTrue(manager.execute(BattleCommand.Shoot(1, Point(3, 0))).success)
            assertEquals(forecast.attack.result.remainingAmount, target.currentAmount)
            assertEquals(forecast.attack.result.remainingHealth, target.currentHealth)
            assertEquals(forecast.attack.remainingFormation, target.formation.current)
            assertEquals(100, shooter.currentHealth)
        }
    }

    @Test
    fun `direct and legacy shots use the same support and formation calculation as execution`() {
        for (profile in listOf(null, RangedShotProfile(4, 30))) {
            val s = Scenario(profile)
            val preview = requireNotNull(s.manager.getAttackPreview(s.shooter, s.target))
            assertEquals(30, preview.attack.preparation!!.supportBonusPercent)
            assertEquals(40, preview.attack.formationReductionPercent)
            assertEquals(profile != null, preview.rangedShot != null)
            s.verifyShot()
        }
    }

    @Test
    fun `indirect shot exposes configured penalty and its actual reason`() {
        val distant = Scenario(RangedShotProfile(1, 30))
        val distancePreview = requireNotNull(distant.manager.getAttackPreview(distant.shooter, distant.target))
        assertEquals(-30, distancePreview.rangedShot!!.damageModifierPercent)
        assertEquals(RangedShotRules.IndirectReason.DISTANCE, distancePreview.rangedShot!!.reason)
        distant.verifyShot()
        val blocked = Scenario(RangedShotProfile(4, 65))
        blocked.place(blocked.ally, 1, 0)
        val blockedPreview = requireNotNull(blocked.manager.getAttackPreview(blocked.shooter, blocked.target))
        assertEquals(-65, blockedPreview.rangedShot!!.damageModifierPercent)
        assertEquals(RangedShotRules.IndirectReason.BLOCKED_LINE, blockedPreview.rangedShot!!.reason)
        blocked.verifyShot()
    }

    @Test
    fun `follow up shot includes movement penalty and preview does not consume the action`() {
        val s = Scenario(RangedShotProfile(1, 30))
        val move = s.manager.execute(BattleCommand.Move(1, Point(0, 1)))
        assertTrue(move.success)
        assertTrue(s.manager.hasPendingFollowUpShot(s.shooter))
        val preview = requireNotNull(s.manager.getAttackPreview(s.shooter, s.target))
        assertEquals(com.unciv.pure.application.battle.MoveAndShootRules.DEFAULT_PENALTY_PERCENT,
            preview.attack.preparation!!.penaltyPercent)
        assertEquals(-30, preview.rangedShot!!.damageModifierPercent)
        assertTrue(s.manager.hasPendingFollowUpShot(s.shooter))
        s.verifyShot()
    }
}
