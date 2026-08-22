package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleScreenCommandMapper
import com.unciv.pure.domain.battle.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BattleScreenCommandMapperTest {
    private val target = Point(2, 3)
    private val attackFrom = Point(1, 3)

    private fun input(
        canShoot: Boolean = false,
        enemy: Boolean = false,
        reachable: Boolean = true,
        from: Point? = attackFrom
    ) = BattleScreenCommandMapper.Input(
        troopId = 7,
        target = target,
        canShoot = canShoot,
        targetIsEnemy = enemy,
        targetIsReachable = reachable,
        attackFrom = from
    )

    @Test
    fun `ranged enemy target maps to SHOOT before ATTACK`() {
        assertEquals(
            BattleCommand.Shoot(7, target),
            BattleScreenCommandMapper.map(input(canShoot = true, enemy = true))
        )
    }

    @Test
    fun `enemy target maps to ATTACK even when movement target is unreachable`() {
        assertEquals(
            BattleCommand.Attack(7, target, attackFrom),
            BattleScreenCommandMapper.map(input(enemy = true, reachable = false))
        )
    }

    @Test
    fun `ATTACK preserves missing attack origin`() {
        assertEquals(
            BattleCommand.Attack(7, target, null),
            BattleScreenCommandMapper.map(input(enemy = true, from = null))
        )
    }

    @Test
    fun `reachable empty target maps to MOVE`() {
        assertEquals(
            BattleCommand.Move(7, target),
            BattleScreenCommandMapper.map(input(reachable = true))
        )
    }

    @Test
    fun `unreachable non-enemy target produces no command`() {
        assertNull(BattleScreenCommandMapper.map(input(reachable = false)))
    }

    @Test
    fun `shoot capability without enemy still maps reachable target to MOVE`() {
        assertEquals(
            BattleCommand.Move(7, target),
            BattleScreenCommandMapper.map(input(canShoot = true, enemy = false))
        )
    }
}
