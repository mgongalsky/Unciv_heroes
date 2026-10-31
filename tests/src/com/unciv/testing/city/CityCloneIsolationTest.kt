package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import org.junit.Assert.*
import org.junit.Test

/** Проверяет независимость изменяемых данных CityInfo.clone фактическими изменениями копии. */
class CityCloneIsolationTest {
    /** Правки войск, строя, вложенных координат и истории копии не затрагивают исходный город. */
    @Test
    fun changingCopiedArmyTerritoryAndHistoryDoesNotChangeOriginal() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            city.previousOwner = "Greece"
            city.workedTiles = hashSetOf(Vector2(1f, 0f))
            city.lockedTiles = hashSetOf(Vector2(1f, 0f))
            val originalTroop = city.garrisonInfo.getTroopAt(0)!!
            val originalAmount = originalTroop.currentAmount
            val originalFormation = originalTroop.formation.copy()
            val originalTiles = city.tiles.map { it.cpy() }.toSet()
            val copy = city.clone()

            assertEquals("Greece", copy.previousOwner)
            assertEquals(city.tiles, copy.tiles)
            assertEquals(city.workedTiles, copy.workedTiles)
            assertEquals(city.lockedTiles, copy.lockedTiles)
            assertNotSame(city.garrisonInfo.army, copy.garrisonInfo.army)
            val copiedTroop = copy.garrisonInfo.getTroopAt(0)!!
            assertEquals(originalTroop.id, copiedTroop.id)
            assertNotSame(originalTroop, copiedTroop)
            assertNotSame(originalTroop.formation, copiedTroop.formation)
            copiedTroop.currentAmount = 1
            copiedTroop.formation.current = 123
            copy.garrisonInfo.setTroopAt(1, null)
            // Одного нового набора недостаточно: каждый изменяемый Vector2 тоже должен быть скопирован.
            copy.tiles.first().set(99f, 99f)
            copy.tiles.clear()
            copy.workedTiles.first().set(98f, 98f)
            copy.lockedTiles.clear()
            copy.previousOwner = "Egypt"

            assertEquals(originalAmount, originalTroop.currentAmount)
            assertEquals(originalFormation, originalTroop.formation)
            assertNotNull(city.garrisonInfo.getTroopAt(1))
            assertEquals(originalTiles, city.tiles)
            assertEquals(setOf(Vector2(1f, 0f)), city.workedTiles)
            assertEquals(setOf(Vector2(1f, 0f)), city.lockedTiles)
            assertEquals("Greece", city.previousOwner)
        }
    }
}
