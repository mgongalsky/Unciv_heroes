package com.unciv.testing

import com.unciv.pure.application.battle.recruitment.*
import org.junit.Assert.*
import org.junit.Test

class BattleArmyDraftTest {
    private fun draft() = BattleArmyDraft(BattleArmySelection(listOf(
        BattleRecruitmentUnit("Peasant", 1),
        BattleRecruitmentUnit("Archer", 3),
        BattleRecruitmentUnit("Horseman", 11)
    )))

    @Test fun additionsAndMaximumRespectMoneySpentInOtherSlots() {
        val draft = draft()
        draft.adjust(0, 495)
        draft.choose(1, "Archer")
        draft.adjust(1, 10)
        assertEquals(1, draft.count(1))
        assertEquals(2L, draft.remaining())
        draft.fill(1)
        assertEquals(1, draft.count(1))
        draft.adjust(0, -10)
        draft.fill(1)
        assertEquals(5, draft.count(1))
        assertEquals(500L, draft.cost())
        draft.adjust(1, Int.MAX_VALUE)
        assertEquals(5, draft.count(1))
        draft.adjust(1, Int.MIN_VALUE)
        assertEquals(0, draft.count(1))
        assertEquals(15L, draft.remaining())
    }

    @Test fun switchingToExpensiveUnitClampsCountWithoutChangingOtherStacks() {
        val draft = draft()
        draft.adjust(0, 100)
        draft.adjust(1, 400)
        draft.choose(0, "Horseman")
        assertEquals(9, draft.count(0))
        assertEquals(400, draft.count(1))
        assertEquals(499L, draft.cost())
        draft.clear(1)
        draft.fill(0)
        assertEquals(45, draft.count(0))
        assertEquals(5L, draft.remaining())
        draft.selection.validate(draft.stacks())
    }

    @Test fun lockedDraftIgnoresAllEditingActionsButCanRestoreServerState() {
        val draft = draft()
        val army = listOf(BattleArmyStack("Archer", 10), BattleArmyStack("Archer", 20))
        draft.restore(army)
        draft.locked = true
        draft.adjust(0, 10)
        draft.choose(0, "Horseman")
        draft.fill(1)
        draft.clear(1)
        assertEquals(army, draft.stacks())
        draft.restore(listOf(BattleArmyStack("Peasant", 500)))
        assertEquals(500L, draft.cost())
        assertTrue(draft.locked)
    }

    @Test fun invalidRestoreDoesNotDestroyTheExistingSelection() {
        val draft = draft()
        draft.adjust(0, 20)
        val before = draft.stacks()
        try {
            draft.restore(listOf(BattleArmyStack("Peasant", 501)))
            fail("Over-budget army accepted")
        } catch (_: IllegalArgumentException) { }
        assertEquals(before, draft.stacks())
        draft.clear(0)
        assertTrue(draft.stacks().isEmpty())
        assertEquals(500L, draft.remaining())
    }
}
