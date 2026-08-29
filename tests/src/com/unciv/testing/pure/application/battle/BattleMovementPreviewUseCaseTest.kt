package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleMovementPreviewUseCase
import com.unciv.pure.application.battle.BattleMovementPreviewUseCase.TileStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class BattleMovementPreviewUseCaseTest {
    @Test
    fun `unreachable tile is hidden`() {
        assertEquals(TileStyle.HIDDEN, classify(false, 1, 4))
    }

    @Test
    fun `tile at half movement is safe`() {
        assertEquals(TileStyle.SAFE, classify(true, 2, 4))
    }

    @Test
    fun `tile beyond half movement shows formation penalty`() {
        assertEquals(TileStyle.FORMATION_PENALTY, classify(true, 3, 4))
    }

    private fun classify(reachable: Boolean, distance: Int, movement: Int) =
        BattleMovementPreviewUseCase.execute(reachable, distance, movement)
}
