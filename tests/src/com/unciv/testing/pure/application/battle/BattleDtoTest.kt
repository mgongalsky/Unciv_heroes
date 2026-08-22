package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleDtoTest {
    @Test
    fun `commands retain troop ids and domain coordinates`() {
        assertEquals(BattleCommand.Move(7, Point(1, 2)), BattleCommand.Move(7, Point(1, 2)))
        assertEquals(Point(4, 5), BattleCommand.Attack(7, Point(3, 4), Point(4, 5)).attackFrom)
        assertEquals(Point(6, 7), BattleCommand.Shoot(7, Point(6, 7)).target)
        assertEquals(7, BattleCommand.Skip(7).troopId)
    }

    @Test
    fun `successful result carries value-only movement and battle flags`() {
        val result = BattleCommandResult(
            success = true,
            movedFrom = Point(1, 2),
            movedTo = Point(2, 2),
            isLuck = true,
            isMorale = false,
            battleEnded = true
        )

        assertTrue(result.success)
        assertNull(result.rejection)
        assertEquals(Point(1, 2), result.movedFrom)
        assertEquals(Point(2, 2), result.movedTo)
        assertTrue(result.isLuck)
        assertFalse(result.isMorale)
        assertTrue(result.battleEnded)
    }

    @Test
    fun `rejected result carries machine-readable reason`() {
        val result = BattleCommandResult(success = false, rejection = BattleRejection.HEX_OCCUPIED)

        assertFalse(result.success)
        assertEquals(BattleRejection.HEX_OCCUPIED, result.rejection)
        assertNull(result.movedFrom)
        assertNull(result.movedTo)
    }
}
