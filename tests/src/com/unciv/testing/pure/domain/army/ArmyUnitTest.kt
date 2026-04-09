package com.unciv.testing.pure.domain.army

import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ArmyUnitTest {

    @Before
    fun setUp() {
        TroopFactory.resetIdCounter()
    }

    private val source = HardcodedTroopDefinitionSource(
        speed = 5, damage = 10, maxHealth = 100, rangedStrength = 0
    )

    private fun makeTroop(name: String = "Spearman", amount: Int = 10) =
            TroopFactory.create(name, amount, source)

    @Test
    fun `getAllTroops returns array of maxSlots size`() {
        val army = Army(7)
        assertEquals(7, army.getAllTroops().size)
    }

    @Test
    fun `addTroop returns true and places in first slot`() {
        val army = Army(3)
        val troop = makeTroop()
        assertTrue(army.addTroop(troop))
        assertEquals(troop.id, army.getAllTroops()[0]?.id)
    }

    @Test
    fun `addTroop returns false when full`() {
        val army = Army(2)
        army.addTroop(makeTroop())
        army.addTroop(makeTroop())
        assertFalse(army.addTroop(makeTroop()))
    }

    @Test
    fun `removeTroop returns true and nulls the slot`() {
        val army = Army(3)
        val troop = makeTroop()
        army.addTroop(troop)
        assertTrue(army.removeTroop(troop))
        assertNull(army.getAllTroops()[0])
    }

    @Test
    fun `removeTroop returns false for foreign troop`() {
        val army = Army(3)
        army.addTroop(makeTroop())
        assertFalse(army.removeTroop(makeTroop()))
    }

    @Test
    fun `removeTroop middle slot does not affect neighbours`() {
        val army = Army(3)
        val t1 = makeTroop(); val t2 = makeTroop(); val t3 = makeTroop()
        army.addTroop(t1); army.addTroop(t2); army.addTroop(t3)
        army.removeTroop(t2)
        assertEquals(t1.id, army.getAllTroops()[0]?.id)
        assertNull(army.getAllTroops()[1])
        assertEquals(t3.id, army.getAllTroops()[2]?.id)
    }

    @Test
    fun `contains returns true for troop in army`() {
        val army = Army(3)
        val troop = makeTroop()
        army.addTroop(troop)
        assertTrue(army.contains(troop))
    }

    @Test
    fun `isEmpty returns true for empty army`() {
        assertTrue(Army(3).isEmpty())
    }

    @Test
    fun `isEmpty returns false when troop has positive amount`() {
        val army = Army(3)
        army.addTroop(makeTroop(amount = 5))
        assertFalse(army.isEmpty())
    }

    @Test
    fun `aliveTroops returns only troops with positive amount`() {
        val army = Army(3)
        val dead = makeTroop(amount = 0)
        val alive = makeTroop(amount = 5)
        army.addTroop(dead)
        army.addTroop(alive)
        val result = army.aliveTroops()
        assertEquals(1, result.size)
        assertEquals(alive.id, result[0].id)
    }
}
