package com.unciv.testing.pure.application.army

import com.unciv.pure.application.army.AddUnitsUseCase
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AddUnitsUseCaseTest {

    private val source = HardcodedTroopDefinitionSource(
        speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0
    )

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
    }

    @Test
    fun `returns false when amount is zero`() {
        val army = Army(3)
        assertFalse(AddUnitsUseCase.execute(army, "Spearman", 0, source))
    }

    @Test
    fun `returns false when amount is negative`() {
        val army = Army(3)
        assertFalse(AddUnitsUseCase.execute(army, "Spearman", -1, source))
    }

    @Test
    fun `adds troop to empty slot`() {
        val army = Army(3)
        assertTrue(AddUnitsUseCase.execute(army, "Spearman", 10, source))
        assertNotNull(army.getAllTroops()[0])
        assertEquals("Spearman", army.getAllTroops()[0]?.unitName)
        assertEquals(10, army.getAllTroops()[0]?.currentAmount)
    }

    @Test
    fun `increases existing troop amount when same unit exists`() {
        val army = Army(3)
        AddUnitsUseCase.execute(army, "Spearman", 10, source)
        AddUnitsUseCase.execute(army, "Spearman", 5, source)
        assertEquals(15, army.getAllTroops()[0]?.currentAmount)
        assertTrue(army.getAllTroops()[1] == null)
    }

    @Test
    fun `adds to new slot when different unit exists`() {
        val army = Army(3)
        AddUnitsUseCase.execute(army, "Spearman", 10, source)
        AddUnitsUseCase.execute(army, "Archer", 5, source)
        assertNotNull(army.getAllTroops()[0])
        assertNotNull(army.getAllTroops()[1])
        assertEquals("Archer", army.getAllTroops()[1]?.unitName)
    }

    @Test
    fun `returns false when army is full and no matching unit`() {
        val army = Army(2)
        AddUnitsUseCase.execute(army, "Spearman", 10, source)
        AddUnitsUseCase.execute(army, "Archer", 5, source)
        assertFalse(AddUnitsUseCase.execute(army, "Knight", 3, source))
    }
}
