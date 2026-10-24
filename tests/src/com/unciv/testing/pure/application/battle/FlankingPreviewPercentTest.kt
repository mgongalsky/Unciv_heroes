package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.*
import com.unciv.pure.domain.battle.DamageResult
import com.unciv.pure.domain.battle.FlankingCalculator
import org.junit.Assert.*
import org.junit.Test

class FlankingPreviewPercentTest {
    private val flank = FlankingCalculator.Result(3, 1, 5, 10, 10)

    private fun preview(raw: Int, formation: Int, reduction: Int = 50,
                        flanking: FlankingCalculator.Result = flank,
                        stored: Boolean = false): BattleAttackPreview {
        val applied = ApplyFlankingDamageUseCase.execute(raw, formation, reduction, flanking)
        val hit = BattleAttackPreview.Hit(
            if (stored) null else PreparedBattleDamage(1, raw, 1, raw, 0, raw, 0, raw),
            flanking, formation, reduction, applied.remainingFormation,
            DamageResult(applied.damageToSoldiers, 0, 1, 1000, false)
        )
        return BattleAttackPreview(hit, hit, raw, 0, usesStoredRetaliation = stored)
    }

    @Test fun `broken formation shows the actual ten percent bonus`() {
        assertEquals(10.0, preview(160, 0).flankingBonusPercent(false), 0.0001)
    }

    @Test fun `intact formation shows five percent rather than the formation damage bonus`() {
        assertEquals(5.0, preview(200, 1000).flankingBonusPercent(false), 0.0001)
    }

    @Test fun `formation breaking during the hit uses both parts of the actual calculation`() {
        val p = preview(160, 80, 60)
        assertEquals(94, p.attack.result.damageDealt)
        assertEquals(17.5, p.flankingBonusPercent(false), 0.0001)
    }

    @Test fun `stored retaliation uses the damage actually spent without needing preparation`() {
        val p = preview(160, 0, stored = true).copy(retaliationBudget = 300, remainingRetaliationBudget = 140)
        assertNull(p.retaliation!!.preparation)
        assertEquals(10.0, p.flankingBonusPercent(true), 0.0001)
    }

    @Test fun `no flank no damage and exhausted retaliation give no bonus`() {
        assertEquals(0.0, preview(160, 0, flanking = FlankingCalculator.Result()).flankingBonusPercent(false), 0.0)
        assertEquals(0.0, preview(0, 0).flankingBonusPercent(false), 0.0)
        assertEquals(0.0, preview(160, 0).copy(retaliationBudget = 0).flankingBonusPercent(true), 0.0)
    }

    @Test fun `zero unflanked damage does not divide by zero`() {
        assertEquals(0.0, preview(10, 1000, 100).flankingBonusPercent(false), 0.0)
        assertEquals(Double.POSITIVE_INFINITY, preview(100, 100, 100).flankingBonusPercent(false), 0.0)
    }
}
