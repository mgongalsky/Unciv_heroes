package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.BattleCommandResult
import com.unciv.pure.application.battle.ShouldAdvanceTurnUseCase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShouldAdvanceTurnUseCaseTest {

    @Test
    fun `morale keeps the current troop turn`() {
        val result = BattleCommandResult(success = true, isMorale = true)

        assertFalse(ShouldAdvanceTurnUseCase.execute(result))
    }

    @Test
    fun `ordinary successful action advances the turn`() {
        val result = BattleCommandResult(success = true, isMorale = false)

        assertTrue(ShouldAdvanceTurnUseCase.execute(result))
    }

    @Test
    fun `AI without an available action yields the turn`() {
        assertTrue(ShouldAdvanceTurnUseCase.execute(null))
    }

    @Test
    fun `short movement keeps turn for a shot without morale`() {
        val result = BattleCommandResult(success = true, hasFollowUpShot = true)
        assertFalse(result.isMorale)
        assertFalse(ShouldAdvanceTurnUseCase.execute(result))
        assertTrue(ShouldAdvanceTurnUseCase.execute(result.copy(hasFollowUpShot = false)))
        assertTrue(ShouldAdvanceTurnUseCase.execute(result.copy(success = false)))
    }
}
