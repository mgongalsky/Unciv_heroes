package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

/** Проверяет, что фасад CityInfo и его ядро используют один активный гарнизон, в том числе после JSON. */
class CityGarrisonStateTest {
    /** Изменения через ArmyInfo и напрямую через Army видны друг другу; clone получает другой адаптер. */
    @Test
    fun foundingAndAdapterMutationsShareTheCoreArmy() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val army = city.state.garrison!!
            assertSame(army, city.garrisonInfo.army)
            val guard = Troop(unitName = "Guard", amount = 12, id = 987)
            city.garrisonInfo.setTroopAt(0, guard)
            assertSame(guard, army.getTroopAt(0))
            army.setTroopAt(1, null)
            assertNull(city.garrisonInfo.getTroopAt(1))
            assertNotSame(city.garrisonInfo, city.clone().garrisonInfo)
        }
    }

    /** Замена армии через любую границу обновляет другую, сохраняя размер слотов и привязку владельца. */
    @Test
    fun replacingEitherEntryPointKeepsTheAdapterOnTheCurrentArmy() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val oldAdapter = city.garrisonInfo
            val replacement = Army(2)
            val guard = Troop(unitName = "Guard", amount = 12, id = 987)
            replacement.setTroopAt(1, guard)
            city.state.garrison = replacement
            val adapted = city.garrisonInfo
            assertNotSame(oldAdapter, adapted)
            assertSame(replacement, adapted.army)
            assertEquals(2, adapted.maxSlots)
            assertSame(oldAdapter.civInfo, adapted.civInfo)
            assertSame(guard, adapted.getTroopAt(1))
            assertSame(adapted, city.garrisonInfo)

            val assigned = ArmyInfo(fixture.civilization, 3)
            city.garrisonInfo = assigned
            assertSame(assigned, city.garrisonInfo)
            assertSame(assigned.army, city.state.garrison)
            assertEquals(3, city.state.garrison!!.maxSlots)
        }
    }

    /** JSON хранит слоты в прежнем поле garrisonInfo, не дублируя state и transient-адаптер. */
    @Test
    fun jsonRestoresOneArmyAndDoesNotWriteAdapterOrCoreFields() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val serialized = json().toJson(city)
            val tree = com.badlogic.gdx.utils.JsonReader().parse(serialized)
            assertTrue(tree.get("garrisonInfo").has("slots"))
            assertFalse(tree.has("state"))
            assertFalse(tree.has("garrisonAdapter"))
            val restored = json().fromJson(CityInfo::class.java, serialized)
            assertSame(restored.state.garrison, restored.garrisonInfo.army)
            assertNotSame(city.state.garrison, restored.state.garrison)
            restored.state.garrison!!.setTroopAt(0, null)
            assertNull(restored.garrisonInfo.getTroopAt(0))
            assertNotNull(city.garrisonInfo.getTroopAt(0))
        }
    }
}
