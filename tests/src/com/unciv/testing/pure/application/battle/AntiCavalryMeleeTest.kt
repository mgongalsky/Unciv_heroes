package com.unciv.testing.pure.application.battle

import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase
import com.unciv.pure.application.battle.CalculateFormationMeleeExchangeUseCase.TroopSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class AntiCavalryMeleeTest {
    private val pikes = TroopSnapshot(10, 10, 100, 100, 0, antiCavalryDamageBonusPercent = 50)
    private val cavalry = TroopSnapshot(10, 0, 100, 100, 0, isCavalry = true)

    @Test fun attackBonusDependsOnTargetClassification() {
        val mounted = CalculateFormationMeleeExchangeUseCase.execute(pikes, cavalry, false, false)
        val infantry = CalculateFormationMeleeExchangeUseCase.execute(
            pikes, cavalry.copy(isCavalry = false), false, false
        )
        assertEquals(150, mounted.damageToDefender.damageDealt)
        assertEquals(100, infantry.damageToDefender.damageDealt)
    }

    @Test fun retaliationBonusDoesNotIncreaseStoredReserve() {
        val result = CalculateFormationMeleeExchangeUseCase.execute(cavalry, pikes, false, false)
        assertEquals(150, result.damageToAttacker.damageDealt)
        assertEquals(100, result.availableRetaliationDamage)
        assertEquals(100, result.appliedRetaliationDamage)
        assertEquals(0, result.remainingRetaliationDamage)
    }

    @Test fun unusedRetaliationRemainsUnmodifiedForNextInfantryTarget() {
        val fragileCavalry = cavalry.copy(amount = 1, health = 15, maxHealth = 15)
        val first = CalculateFormationMeleeExchangeUseCase.execute(fragileCavalry, pikes, false, false)
        assertEquals(15, first.damageToAttacker.damageDealt)
        assertEquals(10, first.appliedRetaliationDamage)
        assertEquals(90, first.remainingRetaliationDamage)
        val second = CalculateFormationMeleeExchangeUseCase.execute(
            cavalry.copy(isCavalry = false), pikes, false, false, first.remainingRetaliationDamage
        )
        assertEquals(90, second.damageToAttacker.damageDealt)
        assertEquals(0, second.remainingRetaliationDamage)
    }
}
