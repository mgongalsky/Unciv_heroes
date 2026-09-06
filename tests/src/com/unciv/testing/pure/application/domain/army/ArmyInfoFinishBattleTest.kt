package com.unciv.testing.pure.application.domain.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.pure.domain.troop.Formation
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArmyInfoFinishBattleTest {
    private val source = HardcodedTroopDefinitionSource(
        speed = 3,
        damage = 10,
        maxHealth = 100,
        rangedStrength = 0,
        formationHealthPercent = 10,
        formationDamageReductionPercent = 50
    )

    @Test
    fun `finishing battle commits survivor losses and resets temporary battle state`() {
        val army = ArmyInfo(FakeCivilizationInfo(), maxSlots = 2)
        val survivor = TroopFactory.create("Spearman", 10, source).apply {
            currentAmount = 6
            currentHealth = 35
            formation.current = 7
        }
        army.addTroop(survivor)

        army.finishBattle()

        assertEquals(6, survivor.amount)
        assertEquals(6, survivor.currentAmount)
        assertEquals(100, survivor.currentHealth)
        assertEquals(Formation.forSoldiers(6), survivor.formation)
    }

    @Test
    fun `finishing battle removes troops with no survivors`() {
        val army = ArmyInfo(FakeCivilizationInfo(), maxSlots = 2)
        val defeated = TroopFactory.create("Spearman", 10, source).apply {
            currentAmount = 0
            currentHealth = 100
            formation.current = 0
        }
        army.addTroop(defeated)

        army.finishBattle()

        assertNull(army.getTroopAt(0))
    }

    @Test
    fun `finishing battle does not grant formation to an unconfigured troop`() {
        val army = ArmyInfo(FakeCivilizationInfo(), maxSlots = 2)
        val peasant = TroopFactory.create(
            "Peasant", 10,
            HardcodedTroopDefinitionSource(4, 2, 5, 0)
        )
        army.addTroop(peasant)
        army.finishBattle()
        assertEquals(0, peasant.formation.current)
        assertEquals(0, peasant.formation.maximum)
    }
}
