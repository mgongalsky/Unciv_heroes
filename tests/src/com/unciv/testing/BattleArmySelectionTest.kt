package com.unciv.testing

import com.unciv.pure.application.battle.recruitment.*
import org.junit.Assert.*
import org.junit.Test

class BattleArmySelectionTest {
    private val selection = BattleArmySelection(listOf(
        BattleRecruitmentUnit("Peasant", 1),
        BattleRecruitmentUnit("Archer", 3),
        BattleRecruitmentUnit("Spearman", 8),
        BattleRecruitmentUnit("Swordsman", 11),
        BattleRecruitmentUnit("Horseman", 11)
    ))

    private fun rejects(stacks: List<BattleArmyStack>) {
        try {
            selection.validate(stacks)
            fail("Invalid army was accepted: $stacks")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun acceptsExactBudgetAndDetachesReadyArmy() {
        val draft = mutableListOf(
            BattleArmyStack("Swordsman", 10), BattleArmyStack("Spearman", 10),
            BattleArmyStack("Archer", 10), BattleArmyStack("Peasant", 80)
        )
        assertEquals(300L, selection.cost(draft))
        val ready = selection.validate(draft)
        draft.clear()
        assertEquals(4, ready.size)
        assertEquals(300L, selection.cost(ready))
    }

    @Test fun rejectsEmptyOverBudgetAndUnavailableArmies() {
        rejects(emptyList())
        rejects(listOf(BattleArmyStack("Peasant", 301)))
        rejects(listOf(BattleArmyStack("Worker", 1)))
        rejects(listOf(BattleArmyStack("Peasant", 0)))
        rejects(listOf(BattleArmyStack("Peasant", -1)))
        rejects(List(5) { BattleArmyStack("Peasant", 1) })
        rejects(List(4) { BattleArmyStack("Horseman", Int.MAX_VALUE) })
    }

    @Test fun permitsRepeatedTypesAndUnspentCoins() {
        val army = List(4) { BattleArmyStack("Archer", 5) }
        assertEquals(army, selection.validate(army))
        assertEquals(60L, selection.cost(army))
    }
    @Test
    fun rejectsCostOverflowEvenWithExtremeRulesetPrices() {
        val expensive =
                BattleArmySelection(listOf(BattleRecruitmentUnit("Expensive", Int.MAX_VALUE)))
        try {
            expensive.validate(List(4) { BattleArmyStack("Expensive", Int.MAX_VALUE) })
            fail("Overflowing army cost was accepted")
        } catch (_: IllegalArgumentException) {
        }
    }
}
