package com.unciv.testing.pure.application.battle

import com.unciv.logic.battle.LegacyBattleEventAdapter
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyBattleEventAdapterTest {
    @Test
    fun `maps every application event to equivalent legacy event`() {
        val applicationEvents = listOf(
            BattleEvent.TroopMoved(1, Point(0, 0), Point(1, 0), true),
            BattleEvent.TroopAttacked(1, 2, 3, true, false, true),
            BattleEvent.TroopShot(1, 2, 4, false, true, false),
            BattleEvent.TurnAdvanced(7),
            BattleEvent.BattleEnded(true),
            BattleEvent.TurnSkipped
        )

        val legacyEvents = applicationEvents.map(LegacyBattleEventAdapter::map)

        assertEquals(
            listOf(
                com.unciv.pure.domain.battle.BattleEvent.TroopMoved(
                    1, Point(0, 0), Point(1, 0), true
                ),
                com.unciv.pure.domain.battle.BattleEvent.TroopAttacked(
                    1, 2, 3, true, false, true
                ),
                com.unciv.pure.domain.battle.BattleEvent.TroopShot(
                    1, 2, 4, false, true, false
                ),
                com.unciv.pure.domain.battle.BattleEvent.TurnAdvanced(7),
                com.unciv.pure.domain.battle.BattleEvent.BattleEnded(true),
                com.unciv.pure.domain.battle.BattleEvent.TurnSkipped
            ),
            legacyEvents
        )
    }
}
