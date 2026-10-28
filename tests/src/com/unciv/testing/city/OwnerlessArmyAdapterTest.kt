package com.unciv.testing.city

import com.unciv.logic.army.ArmyInfo
import com.unciv.pure.domain.army.Army
import org.junit.Assert.*
import org.junit.Test

class OwnerlessArmyAdapterTest {
    @Test
    fun explicitOwnerlessArmyNeedsNeitherCivilizationNorGame() {
        val state = Army(2)
        val adapter = ArmyInfo(null, state.maxSlots, state)
        assertNull(adapter.ownerOrNull)
        assertSame(state, adapter.army)
        assertEquals(2, adapter.getAllTroops().size)
        assertEquals(0, adapter.getBattleMorale())
        assertEquals(1, adapter.getBattleLuck())
        val copy = adapter.clone()
        assertNull(copy.ownerOrNull)
        assertNotSame(state, copy.army)
        try {
            adapter.civInfo
            fail("An ownerless army must not manufacture a civilization")
        } catch (expected: UninitializedPropertyAccessException) {
            assertTrue(expected.message!!.contains("civilization"))
        }
    }

    @Test
    fun bindingAndClearingOwnerPreservesArmy() {
        CityTestFixture().use { fixture ->
            val adapter = ArmyInfo(null, 2)
            val state = adapter.army
            adapter.setTransients(fixture.civilization, null)
            assertSame(fixture.civilization, adapter.civInfo)
            adapter.setTransients(null, null)
            assertNull(adapter.ownerOrNull)
            assertSame(state, adapter.army)
            assertNull(adapter.hero)
        }
    }
}
