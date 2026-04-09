package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.battle.PerformAttackUseCase
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformAttackUseCaseUnitTest {

    private val source = HardcodedTroopDefinitionSource(speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0)
    private lateinit var attacker: Troop
    private lateinit var defender: Troop
    private lateinit var currentTile: FakeBattleTile
    private lateinit var attackTile: FakeBattleTile
    private lateinit var targetTile: FakeBattleTile

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
        attacker = TroopFactory.create("Attacker", 10, source)
        defender = TroopFactory.create("Defender", 10, source)
        currentTile = FakeBattleTile(Vector2(0f, 0f))
        attackTile = FakeBattleTile(Vector2(1f, 0f))
        targetTile = FakeBattleTile(Vector2(2f, 0f))
    }

    private fun input(
        defenderTroop: Troop? = defender,
        attackTileArg: FakeBattleTile? = attackTile,
        currentTileArg: FakeBattleTile? = currentTile,
        isTargetOccupiedByEnemy: Boolean = true,
        isAttackTileAchievable: Boolean = true,
        isAttackTileFree: Boolean = true,
        isLuck: Boolean = false,
        isMorale: Boolean = false,
        defenderRemainingAmount: Int = 7,
        defenderDied: Boolean = false
    ) = PerformAttackUseCase.Input(
        attacker = attacker,
        defender = defenderTroop,
        attackTile = attackTileArg,
        currentTile = currentTileArg,
        isTargetOccupiedByEnemy = isTargetOccupiedByEnemy,
        isAttackTileAchievable = isAttackTileAchievable,
        isAttackTileFree = isAttackTileFree,
        isLuck = isLuck,
        isMorale = isMorale,
        defenderRemainingAmount = defenderRemainingAmount,
        defenderDied = defenderDied
    )

    // --- success ---

    @Test
    fun `successful attack returns success`() {
        assertTrue(PerformAttackUseCase.execute(input()).success)
    }

    @Test
    fun `successful attack returns movedFrom as currentTile`() {
        assertEquals(currentTile, PerformAttackUseCase.execute(input()).movedFrom)
    }

    @Test
    fun `successful attack returns movedTo as attackTile`() {
        assertEquals(attackTile, PerformAttackUseCase.execute(input()).movedTo)
    }

    @Test
    fun `successful attack has no errorId`() {
        assertNull(PerformAttackUseCase.execute(input()).errorId)
    }

    @Test
    fun `successful attack passes through isLuck`() {
        assertTrue(PerformAttackUseCase.execute(input(isLuck = true)).isLuck)
    }

    @Test
    fun `successful attack passes through isMorale`() {
        assertTrue(PerformAttackUseCase.execute(input(isMorale = true)).isMorale)
    }

    @Test
    fun `successful attack passes through defenderRemainingAmount`() {
        assertEquals(7, PerformAttackUseCase.execute(input(defenderRemainingAmount = 7)).defenderRemainingAmount)
    }

    @Test
    fun `successful attack passes through defenderDied`() {
        assertTrue(PerformAttackUseCase.execute(input(defenderDied = true)).defenderDied)
    }

    // --- INVALID_TARGET ---

    @Test
    fun `null defender returns failure`() {
        assertFalse(PerformAttackUseCase.execute(input(defenderTroop = null)).success)
    }

    @Test
    fun `null defender returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(defenderTroop = null)).errorId)
    }

    @Test
    fun `target not occupied by enemy returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(isTargetOccupiedByEnemy = false)).errorId)
    }

    @Test
    fun `null attackTile returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(attackTileArg = null)).errorId)
    }

    @Test
    fun `attackTile not achievable returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(isAttackTileAchievable = false)).errorId)
    }

    @Test
    fun `attackTile not free and not current tile returns INVALID_TARGET`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(isAttackTileFree = false)).errorId)
    }

    @Test
    fun `attackTile not free but is current tile — success`() {
        // атакующий уже стоит на attackTile — это валидная атака
        assertTrue(PerformAttackUseCase.execute(input(
            currentTileArg = attackTile,
            isAttackTileFree = false
        )).success)
    }

    // --- приоритет проверок ---

    @Test
    fun `null defender takes priority over other checks`() {
        assertEquals(ErrorId.INVALID_TARGET, PerformAttackUseCase.execute(input(
            defenderTroop = null,
            isTargetOccupiedByEnemy = false,
            attackTileArg = null
        )).errorId)
    }
}
