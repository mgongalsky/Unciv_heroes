package com.unciv.testing

import com.unciv.pure.application.battle.recruitment.*
import org.junit.Assert.*
import org.junit.Test

class BattleArmySelectionTest {
    private val selection = BattleArmySelection(listOf(
        BattleRecruitmentUnit("Peasant", 1), BattleRecruitmentUnit("Archer", 4),
        BattleRecruitmentUnit("Spearman", 8), BattleRecruitmentUnit("Swordsman", 11),
        BattleRecruitmentUnit("Horseman", 16)
    ))

    private fun rejects(stacks: List<BattleArmyStack>) {
        try {
            selection.validate(stacks)
            fail("Invalid army was accepted: $stacks")
        } catch (_: IllegalArgumentException) { }
    }

    @Test
    fun acceptsFiveStacksAtExactlyFiveHundredAndDetachesReadyArmy() {
        assertEquals(500, selection.budget)
        assertEquals(7, selection.maxStacks)
        val draft = mutableListOf(
            BattleArmyStack("Swordsman", 10),
            BattleArmyStack("Spearman", 10), BattleArmyStack("Archer", 10),
            BattleArmyStack("Horseman", 10), BattleArmyStack("Peasant", 110)
        )
        assertEquals(500L, selection.cost(draft))
        val ready = selection.validate(draft)
        draft.clear()
        assertEquals(5, ready.size)
        assertEquals(500L, selection.cost(ready))
    }

    @Test
    fun rejectsEmptyOverBudgetAndUnavailableArmies() {
        rejects(emptyList())
        rejects(listOf(BattleArmyStack("Peasant", 501)))
        rejects(listOf(BattleArmyStack("Worker", 1)))
        rejects(listOf(BattleArmyStack("Peasant", 0)))
        rejects(listOf(BattleArmyStack("Peasant", -1)))
        rejects(List(8) { BattleArmyStack("Peasant", 1) })
        rejects(List(7) { BattleArmyStack("Horseman", Int.MAX_VALUE) })
    }

    @Test fun permitsRepeatedTypesAndUnspentCoins() {
        val army = List(5) { BattleArmyStack("Archer", 5) }
        assertEquals(army, selection.validate(army))
        assertEquals(100L, selection.cost(army))
    }

    @Test fun rejectsCostOverflowEvenWithExtremeRulesetPrices() {
        val expensive = BattleArmySelection(listOf(BattleRecruitmentUnit("Expensive", Int.MAX_VALUE)))
        try {
            expensive.validate(List(5) { BattleArmyStack("Expensive", Int.MAX_VALUE) })
            fail("Overflowing army cost was accepted")
        } catch (_: IllegalArgumentException) { }
    }
    @Test
    fun acceptsSevenStacksAtBudgetAndRejectsAnEighth() {
        val army = List(6) { BattleArmyStack("Peasant", 70) } + BattleArmyStack("Archer", 20)
        assertEquals(500L, selection.cost(army))
        assertEquals(army, selection.validate(army))
        rejects(army + BattleArmyStack("Peasant", 1))
        rejects(List(8) { BattleArmyStack("Peasant", 1) })
        val draft = BattleArmyDraft(selection)
        draft.restore(army)
        assertEquals(20, draft.count(6))
        draft.adjust(6, -1)
        assertEquals(4L, draft.remaining())
        assertEquals(7, draft.stacks().size)
    }
}
