package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateBattleScoreUseCase
import com.unciv.pure.application.battle.CalculateBattleScoreUseCase.Stack
import com.unciv.pure.domain.battle.BattleScore
import org.junit.Assert.*
import org.junit.Test

class CalculateBattleScoreUseCaseTest {
    @Test fun valuesSurvivorsAndDestroyedStacksAtTheirOwnPrices() {
        val score = requireNotNull(CalculateBattleScoreUseCase.execute(listOf(
            Stack("Horseman", 20, 15), Stack("Archer", 40, 15), Stack("Peasant", 20, 0)
        )) { mapOf("Horseman" to 16, "Archer" to 4, "Peasant" to 1)[it] })
        assertEquals(500L, score.initialValue)
        assertEquals(300L, score.remainingValue)
        assertEquals(60.0, score.percent, 0.0001)
    }

    @Test fun usesActualInitialValueInsteadOfBudget() {
        val score = requireNotNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Horseman", 30, 15))) { 16 })
        assertEquals(480L, score.initialValue)
        assertEquals(50.0, score.percent, 0.0001)
    }

    @Test fun handlesFullSurvivalAndZeroSurvival() {
        assertEquals(100.0, requireNotNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Archer", 5, 5))) { 4 }).percent, 0.0)
        assertEquals(0.0, requireNotNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Archer", 5, 0))) { 4 }).percent, 0.0)
        assertEquals(0.0, BattleScore(0, 0).percent, 0.0)
    }

    @Test fun unknownPriceMakesWholeScoreUnavailable() {
        assertNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Unknown", 5, 0))) { null })
        assertNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Unknown", 5, 5))) { 0 })
        assertNull(CalculateBattleScoreUseCase.execute(emptyList()) { 1 })
    }

    @Test fun usesLongArithmetic() {
        val score = requireNotNull(CalculateBattleScoreUseCase.execute(listOf(Stack("Unit", 100000, 50000))) { 100000 })
        assertEquals(10000000000L, score.initialValue)
        assertEquals(50.0, score.percent, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSurvivorsAboveInitialCount() {
        CalculateBattleScoreUseCase.execute(listOf(Stack("Unit", 5, 6))) { 1 }
    }
}
