package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Фиксирует наблюдаемое поведение основания, которое должен сохранять рефакторинг. */
class CityFoundingCharacterizationTest {
    /** Проверяет первый и следующий город: население, столицу, территорию, гарнизон и регистрацию. */
    @Test
    fun firstAndSecondFoundingPreserveObservedStateAndEntryPointEffects() {
        CityTestFixture().use { fixture ->
            // Состав армии задаётся явно: от него зависит начальное население города.
            fun founder(vararg troops: Pair<String, Int>) = MapUnit().apply {
                civInfo = fixture.civilization
                owner = civInfo.civName
                army = ArmyInfo(civInfo, *troops)
                army.setTransients(civInfo, this)
            }

            fixture.game.turns = 7
            val firstFounder = founder("Peasant" to 29)
            // Совместимый конструктор основывает город, но не выбирает производство.
            val first = CityInfo(fixture.civilization, Vector2(0f, 0f), firstFounder)

            assertEquals("Rome", first.name)
            assertEquals(1, first.population.population)
            assertTrue(first.isCapital())
            assertTrue(first.isOriginalCapital)
            assertEquals(setOf(Vector2(0f, 0f), Vector2(1f, 0f)), first.tiles)
            assertEquals(listOf("Palace"), first.cityConstructions.builtBuildings.sorted())
            assertTrue(first.cityConstructions.constructionQueue.isEmpty())

            val secondFounder = founder("Peasant" to 15, "Archer" to 15)
            // addCity дополнительно выбирает производство; это различие точек входа сохраняется.
            fixture.civilization.addCity(Vector2(4f, 0f), secondFounder)
            val second = fixture.civilization.cities.last()

            assertEquals("Antium", second.name)
            assertEquals(2, second.population.population)
            assertFalse(second.isCapital())
            assertFalse(second.isOriginalCapital)
            assertEquals(setOf(Vector2(3f, 0f), Vector2(4f, 0f), Vector2(5f, 0f)), second.tiles)
            assertTrue(second.cityConstructions.builtBuildings.isEmpty())
            assertEquals(listOf("Warrior"), second.cityConstructions.constructionQueue)

            assertEquals(listOf(first, second), fixture.civilization.cities)
            assertNotEquals(first.id, second.id)
            assertEquals(2, fixture.civilization.citiesCreated)
            assertTrue(fixture.civilization.hasEverOwnedOriginalCapital == true)
            assertFalse(fixture.civilization.policies.shouldOpenPolicyPicker)
            assertEquals(0, firstFounder.calculateArmyPopulation())
            // Дробные вклады разных слотов складываются до усечения до целого числа.
            assertEquals(1, secondFounder.calculateArmyPopulation())
            // Уничтожение основателя относится к полному действию, а не к созданию города.
            assertFalse(firstFounder.isDestroyed)
            assertFalse(secondFounder.isDestroyed)

            for (city in listOf(first, second)) {
                assertEquals(0, city.population.foodStored)
                assertEquals("Rome", city.foundingCiv)
                assertEquals("", city.previousOwner)
                assertEquals(7, city.turnAcquired)
                // Старый список и активная армия пока имеют разный состав; тест фиксирует оба.
                assertEquals(listOf("Archer:30", "Spearman:20", "Horseman:10", "Swordsman:15"),
                    city.garrison.map { "${it.unitName}:${it.amount}" })
                assertEquals(listOf("Archer:35", "Peasant:20", null, null),
                    city.garrisonInfo.getAllTroops().map { it?.let { "${it.unitName}:${it.amount}" } })
                assertSame(fixture.civilization, city.garrisonInfo.civInfo)
                assertNull(city.getCenterTile().improvement)
                assertEquals(RoadStatus.None, city.getCenterTile().roadStatus)
                assertSame(city, city.getCenterTile().getCity())
                for (position in city.tiles) {
                    assertSame(city, fixture.game.tileMap[position].owningCity)
                }
            }
        }
    }
}
