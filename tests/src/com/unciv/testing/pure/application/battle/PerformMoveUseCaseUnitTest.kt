package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.pure.application.battle.PerformMoveUseCase
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeBattleTile
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformMoveUseCaseUnitTest {

    private val source = HardcodedTroopDefinitionSource(speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0)
    private lateinit var troop: Troop
    private lateinit var currentTile: FakeBattleTile
    private lateinit var targetTile: FakeBattleTile

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
        troop = TroopFactory.create("Spearman", 10, source)
        currentTile = FakeBattleTile(Vector2(0f, 0f))
        targetTile = FakeBattleTile(Vector2(1f, 0f))
    }

    private fun input(
        isTileAchievable: Boolean = true,
        isTileOccupiedByAlly: Boolean = false,
        isTileFree: Boolean = true,
        current: FakeBattleTile = currentTile,
        target: FakeBattleTile = targetTile
    ) = PerformMoveUseCase.Input(
        troop = troop,
        targetTile = target,
        currentTile = current,
        isTileAchievable = isTileAchievable,
        isTileOccupiedByAlly = isTileOccupiedByAlly,
        isTileFree = isTileFree
    )

    // --- success ---

    @Test
    fun `successful move returns success`() {
        val result = PerformMoveUseCase.execute(input())
        assertTrue(result.success)
    }

    @Test
    fun `successful move returns correct movedFrom`() {
        val result = PerformMoveUseCase.execute(input())
        assertEquals(currentTile, result.movedFrom)
    }

    @Test
    fun `successful move returns correct movedTo`() {
        val result = PerformMoveUseCase.execute(input())
        assertEquals(targetTile, result.movedTo)
    }

    @Test
    fun `successful move has no errorId`() {
        val result = PerformMoveUseCase.execute(input())
        assertNull(result.errorId)
    }

    @Test
    fun `successful move with null currentTile — movedFrom is null`() {
        val result = PerformMoveUseCase.execute(input(current = null as? FakeBattleTile ?: currentTile).copy(currentTile = null))
        assertNull(result.movedFrom)
    }

    // --- TOO_FAR ---

    @Test
    fun `tile not achievable returns failure`() {
        val result = PerformMoveUseCase.execute(input(isTileAchievable = false))
        assertFalse(result.success)
    }

    @Test
    fun `tile not achievable returns TOO_FAR`() {
        val result = PerformMoveUseCase.execute(input(isTileAchievable = false))
        assertEquals(ErrorId.TOO_FAR, result.errorId)
    }

    @Test
    fun `tile not achievable — movedFrom is null`() {
        val result = PerformMoveUseCase.execute(input(isTileAchievable = false))
        assertNull(result.movedFrom)
    }

    @Test
    fun `tile not achievable — movedTo is null`() {
        val result = PerformMoveUseCase.execute(input(isTileAchievable = false))
        assertNull(result.movedTo)
    }

    // --- OCCUPIED_BY_ALLY ---

    @Test
    fun `tile occupied by ally returns failure`() {
        val result = PerformMoveUseCase.execute(input(isTileOccupiedByAlly = true))
        assertFalse(result.success)
    }

    @Test
    fun `tile occupied by ally returns OCCUPIED_BY_ALLY`() {
        val result = PerformMoveUseCase.execute(input(isTileOccupiedByAlly = true))
        assertEquals(ErrorId.OCCUPIED_BY_ALLY, result.errorId)
    }

    @Test
    fun `tile occupied by ally — movedFrom is null`() {
        val result = PerformMoveUseCase.execute(input(isTileOccupiedByAlly = true))
        assertNull(result.movedFrom)
    }

    // --- HEX_OCCUPIED ---

    @Test
    fun `tile not free returns failure`() {
        val result = PerformMoveUseCase.execute(input(isTileFree = false))
        assertFalse(result.success)
    }

    @Test
    fun `tile not free returns HEX_OCCUPIED`() {
        val result = PerformMoveUseCase.execute(input(isTileFree = false))
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
    }

    @Test
    fun `tile not free — movedTo is null`() {
        val result = PerformMoveUseCase.execute(input(isTileFree = false))
        assertNull(result.movedTo)
    }

    // --- приоритет проверок ---

    @Test
    fun `not achievable takes priority over occupied by ally`() {
        val result = PerformMoveUseCase.execute(input(isTileAchievable = false, isTileOccupiedByAlly = true))
        assertEquals(ErrorId.TOO_FAR, result.errorId)
    }

    @Test
    fun `occupied by ally takes priority over not free`() {
        val result = PerformMoveUseCase.execute(input(isTileOccupiedByAlly = true, isTileFree = false))
        assertEquals(ErrorId.OCCUPIED_BY_ALLY, result.errorId)
    }
}
