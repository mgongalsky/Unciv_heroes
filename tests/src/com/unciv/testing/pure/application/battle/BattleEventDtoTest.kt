package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleEventDtoTest {
    @Test
    fun `movement event contains ids coordinates and morale only`() {
        val event = BattleEvent.TroopMoved(7, Point(1, 2), Point(2, 2), isMorale = true)

        assertEquals(7, event.troopId)
        assertEquals(Point(1, 2), event.from)
        assertEquals(Point(2, 2), event.to)
        assertTrue(event.isMorale)
    }

    @Test
    fun `attack event has value equality`() {
        val first =
            BattleEvent.TroopAttacked(1, 2, 3, isLuck = true, isMorale = false, defenderDied = true)
        val second =
            BattleEvent.TroopAttacked(1, 2, 3, isLuck = true, isMorale = false, defenderDied = true)

        assertEquals(first, second)
    }
}
