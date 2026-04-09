package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.PerformShootUseCase
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ErrorId

class PerformShootUseCaseUnitTest {

    private val source = HardcodedTroopDefinitionSource(speed = 5, damage = 10, maxHealth = 100, rangedStrength = 10)
    private lateinit var attacker: Troop
    private lateinit var defender: Troop

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
        attacker = TroopFactory.create("Archer", 10, source)
        defender = TroopFactory.create("Spearman", 10, source)
    }

    private fun input(
        defenderTroop: Troop? = defender,
        canShoot: Boolean = true,
        isTargetOccupiedByEnemy: Boolean = true,
        isLuck: Boolean = false,
        isMorale: Boolean = false,
        defenderRemainingAmount: Int = 7,
        defenderDied: Boolean = false
    ) = PerformShootUseCase.Input(
        attacker = attacker,
        defender = defenderTroop,
        canShoot = canShoot,
        isTargetOccupiedByEnemy = isTargetOccupiedByEnemy,
        isLuck = isLuck,
        isMorale = isMorale,
        defenderRemainingAmount = defenderRemainingAmount,
        defenderDied = defenderDied
    )

    // --- success ---

    @Test
    fun `successful shoot returns success`() {
        assertTrue(PerformShootUseCase.execute(input()).success)
    }

    @Test
    fun `successful shoot has no errorId`() {
        assertNull(PerformShootUseCase.execute(input()).errorId)
    }

    @Test
    fun `successful shoot has no movedFrom or movedTo`() {
        val result = PerformShootUseCase.execute(input())
        // shoot не двигает атакующего — полей movedFrom/movedTo в Output нет
        assertTrue(result.success)
    }

    @Test
    fun `successful shoot passes through isLuck`() {
        assertTrue(PerformShootUseCase.execute(input(isLuck = true)).isLuck)
    }

    @Test
    fun `successful shoot passes through isMorale`() {
        assertTrue(PerformShootUseCase.execute(input(isMorale = true)).isMorale)
    }

    @Test
    fun `successful shoot passes through defenderRemainingAmount`() {
        assertEquals(7, PerformShootUseCase.execute(input(defenderRemainingAmount = 7)).defenderRemainingAmount)
    }

    @Test
    fun `successful shoot passes through defenderDied`() {
        assertTrue(PerformShootUseCase.execute(input(defenderDied = true)).defenderDied)
    }

    // --- ошибки ---

    @Test
    fun `null defender returns failure`() {
        assertFalse(PerformShootUseCase.execute(input(defenderTroop = null)).success)
    }

    @Test
    fun `null defender returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformShootUseCase.execute(input(defenderTroop = null)).errorId)
    }

    @Test
    fun `cannot shoot returns failure`() {
        assertFalse(PerformShootUseCase.execute(input(canShoot = false)).success)
    }

    @Test
    fun `cannot shoot returns NOT_IMPLEMENTED`() {
        assertEquals(ErrorId.NOT_IMPLEMENTED, PerformShootUseCase.execute(input(canShoot = false)).errorId)
    }

    @Test
    fun `target not occupied by enemy returns failure`() {
        assertFalse(PerformShootUseCase.execute(input(isTargetOccupiedByEnemy = false)).success)
    }

    @Test
    fun `target not occupied by enemy returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformShootUseCase.execute(input(isTargetOccupiedByEnemy = false)).errorId)
    }

    // --- приоритет проверок ---

    @Test
    fun `null defender takes priority over canShoot`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformShootUseCase.execute(input(
            defenderTroop = null,
            canShoot = false
        )).errorId)
    }

    @Test
    fun `canShoot takes priority over isTargetOccupiedByEnemy`() {
        assertEquals(ErrorId.NOT_IMPLEMENTED, PerformShootUseCase.execute(input(
            canShoot = false,
            isTargetOccupiedByEnemy = false
        )).errorId)
    }
}
