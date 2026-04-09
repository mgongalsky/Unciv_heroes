package com.unciv.testing.pure.application.army

import com.unciv.pure.application.army.FillArmyUseCase
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FillArmyUseCaseTest {

    private val source = HardcodedTroopDefinitionSource(
        speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0
    )

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
    }

    @Test
    fun `even distribution fills all slots equally`() {
        val army = Army(4)
        FillArmyUseCase.execute(army, "Spearman", 40, source)
        army.getAllTroops().forEach { assertEquals(10, it?.currentAmount) }
    }

    @Test
    fun `uneven distribution puts remainder in first slots`() {
        val army = Army(4)
        FillArmyUseCase.execute(army, "Spearman", 10, source)
        assertEquals(3, army.getAllTroops()[0]?.currentAmount)
        assertEquals(3, army.getAllTroops()[1]?.currentAmount)
        assertEquals(2, army.getAllTroops()[2]?.currentAmount)
        assertEquals(2, army.getAllTroops()[3]?.currentAmount)
    }

    @Test
    fun `totalCount less than slots leaves some null`() {
        val army = Army(4)
        FillArmyUseCase.execute(army, "Spearman", 3, source)
        assertEquals(1, army.getAllTroops()[0]?.currentAmount)
        assertEquals(1, army.getAllTroops()[1]?.currentAmount)
        assertEquals(1, army.getAllTroops()[2]?.currentAmount)
        assertNull(army.getAllTroops()[3])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero totalCount throws`() {
        FillArmyUseCase.execute(Army(4), "Spearman", 0, source)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank unitName throws`() {
        FillArmyUseCase.execute(Army(4), "", 10, source)
    }

    @Test
    fun `clears existing troops before filling`() {
        val army = Army(4)
        FillArmyUseCase.execute(army, "Spearman", 40, source)
        FillArmyUseCase.execute(army, "Archer", 4, source)
        army.getAllTroops().forEach { assertEquals("Archer", it?.unitName) }
    }
}
