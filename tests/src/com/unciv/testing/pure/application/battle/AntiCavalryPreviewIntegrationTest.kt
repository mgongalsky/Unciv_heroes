package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.execute
import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.battle.DamageResult
import com.unciv.pure.domain.battle.FlankingCalculator
import com.unciv.pure.domain.battle.Point
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeArmy
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Test

class AntiCavalryPreviewIntegrationTest {
    private fun health(troop: Troop) =
        (troop.currentAmount - 1) * troop.maxHealth + troop.currentHealth

    @Test
    fun previewMatchesExecutedAttackAndRetaliationForEachTargetType() {
        for (attackerCavalry in listOf(false, true)) {
            for (defenderCavalry in listOf(false, true)) {
                val attacker = Troop("Guard", 10, 5, 10, 100, id = 1).apply {
                    isCavalry = attackerCavalry
                    antiCavalryDamageBonusPercent = 50
                }
                val defender = Troop("Opponent", 10, 1, 10, 100, id = 2).apply {
                    isCavalry = defenderCavalry
                    antiCavalryDamageBonusPercent = 50
                }
                val origin = FakeBattleTile(Vector2(0f, 0f))
                val target = FakeBattleTile(Vector2(1f, 0f))
                origin.addNeighbor(target)
                target.addNeighbor(origin)
                val manager = BattleManager(
                    FakeArmy(attacker), FakeArmy(defender),
                    FakeBattleField(listOf(origin, target)), SeededBattleRandom(42),
                    moraleProbability = 0.0, luckProbability = 0.0
                )
                manager.setTroopPosition(attacker, origin)
                origin.receiveTroop(attacker)
                manager.setTroopPosition(defender, target)
                target.receiveTroop(defender)
                manager.initializeTurnQueue()
                val preview = requireNotNull(manager.getAttackPreview(attacker, defender, origin))
                assertEquals(if (defenderCavalry) 50 else 0, preview.attack.antiCavalryDamageBonusPercent)
                assertEquals(if (attackerCavalry) 50 else 0, preview.retaliation!!.antiCavalryDamageBonusPercent)
                assertEquals(if (defenderCavalry) 150 else 100, preview.attack.result.damageDealt)
                assertEquals(if (attackerCavalry) 150 else 100, preview.retaliation!!.result.damageDealt)
                assertEquals(1000, health(attacker))
                assertEquals(1000, health(defender))
                val result = manager.execute(BattleCommand.Attack(1, Point(1, 0), Point(0, 0)))
                assertTrue(result.success)
                assertEquals(preview.attack.result.damageDealt, 1000 - health(defender))
                assertEquals(preview.retaliation!!.result.damageDealt, 1000 - health(attacker))
                assertEquals(preview.remainingRetaliationBudget, manager.getRetaliationBudget(defender))
            }
        }
    }

    @Test
    fun flankingPercentageUsesBoostedDamageIncludingStoredRetaliation() {
        fun preview(raw: Int, bonus: Int, stored: Boolean): BattleAttackPreview {
            val hit = BattleAttackPreview.Hit(
                if (stored) null else PreparedBattleDamage(1, raw, 1, raw, 0, raw, 0, raw),
                FlankingCalculator.Result(3, 1, 5, 10, 10),
                80, 60, 0, DamageResult(0, 0, 10, 100, false),
                antiCavalryDamageBonusPercent = bonus
            )
            return BattleAttackPreview(hit, hit, raw, 0, usesStoredRetaliation = stored)
        }
        for (stored in listOf(false, true)) {
            val boosted = preview(160, 50, stored).flankingBonusPercent(stored)
            val equivalent = preview(240, 0, stored).flankingBonusPercent(stored)
            assertEquals(equivalent, boosted, 0.0001)
            assertTrue(kotlin.math.abs(boosted - preview(160, 0, stored).flankingBonusPercent(stored)) > 0.01)
        }
    }
}
