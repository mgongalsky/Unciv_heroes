package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.RoadStatus
import org.junit.Assert.*
import org.junit.Test

/** Проверяет копирование и восстановление учёта дорог за пределами городской территории. */
class CityRoadPersistenceTest {
    /** Копия цивилизации независимо хранит и набор neutralRoads, и вложенные изменяемые координаты. */
    @Test
    fun clonedNeutralRoadCoordinatesAndCollectionAreIndependent() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                fixture.foundCity()
                val road = fixture.game.tileMap.values.first { it.getCity() == null }
                road.addRoad(RoadStatus.Road, fixture.civilization)
                val position = road.position.cpy()
                val copy = fixture.game.clone().civilizations.single()
                assertNotSame(fixture.civilization.neutralRoads, copy.neutralRoads)
                val copiedPosition = copy.neutralRoads.single()
                assertNotSame(road.position, copiedPosition)
                copy.neutralRoads.clear()
                copiedPosition.set(99f, 99f)
                assertEquals(setOf(position), fixture.civilization.neutralRoads)
                assertEquals(position, road.position)
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }

    /** Повторный setTransients учитывает только существующие дороги вне городов, не дублируя основание. */
    @Test
    fun repeatedRestorationTracksOnlyExistingUnownedRoads() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val city = fixture.foundCity()
                val ownedRoad = city.getTiles().first { it.position != city.location }
                ownedRoad.addRoad(RoadStatus.Road, fixture.civilization)
                val outside = fixture.game.tileMap.values.filter { it.getCity() == null }.take(2)
                outside[0].addRoad(RoadStatus.Road, fixture.civilization)
                outside[1].addRoad(RoadStatus.Road, fixture.civilization)
                // Удалённая дорога не должна возвращаться в учёт по старому признаку владельца.
                outside[1].removeRoad()
                val loaded = json().fromJson(GameInfo::class.java, json().toJson(fixture.game.clone()))
                repeat(2) {
                    loaded.setTransients()
                    val owner = loaded.getCivilization("Rome")
                    val restored = loaded.getCities().single()
                    assertEquals(setOf(outside[0].position), owner.neutralRoads)
                    assertSame(restored, loaded.tileMap[city.location].getCity())
                    assertTrue(loaded.tileMap[city.location].isCityCenter())
                    assertSame(restored, loaded.tileMap[ownedRoad.position].getCity())
                    assertSame(owner, loaded.tileMap[ownedRoad.position].getRoadOwner())
                    assertSame(owner, restored.garrisonInfo.civInfo)
                    assertEquals(1, owner.citiesCreated)
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
