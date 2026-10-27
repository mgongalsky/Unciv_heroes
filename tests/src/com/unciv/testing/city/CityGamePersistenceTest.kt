package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.city.CityFlags
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import org.junit.Assert.*
import org.junit.Test

class CityGamePersistenceTest {
    @Test
    fun gameCopyAndRealJsonRestoreCityAndAllowAnotherTurn() {
        CityTestFixture().use { fixture ->
            val previousMonsterGame = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val original = fixture.foundCity()
                original.previousOwner = "Greece"
                original.population.foodStored = 3
                original.currConscription = 17
                original.setFlag(CityFlags.Resistance, 4)
                original.cityConstructions.constructionQueue.clear()
                original.cityConstructions.constructionQueue.add("Scout")
                fixture.game.turns = 1

                val copy = fixture.game.clone()
                val copiedCity = copy.getCities().single()
                assertEquals(CivilizationInfo::class.java, copy.civilizations.single().javaClass)
                assertNotSame(fixture.game.tileMap, copy.tileMap)
                assertNotSame(original, copiedCity)
                assertSame(fixture.game.gameParameters, copy.gameParameters)
                assertNotSame(original.garrisonInfo, copiedCity.garrisonInfo)
                assertEquals("Greece", copiedCity.previousOwner)

                val encoded = json().toJson(copy)
                assertFalse(encoded.contains("FakeCivilizationInfo"))
                val loaded = json().fromJson(GameInfo::class.java, encoded)
                loaded.setTransients()
                val city = loaded.getCities().single()
                val owner = loaded.getCivilization("Rome")
                assertSame(owner, city.civInfo)
                assertSame(loaded, owner.gameInfo)
                assertSame(owner, loaded.currentPlayerCiv)
                assertSame(city, loaded.tileMap[city.location].getCity())
                assertEquals(original.id, city.id)
                assertEquals(original.location, city.location)
                assertEquals(original.foundingCiv, city.foundingCiv)
                assertEquals("Greece", city.previousOwner)
                assertEquals(original.turnAcquired, city.turnAcquired)
                assertEquals(original.population.population, city.population.population)
                assertEquals(3, city.population.foodStored)
                assertEquals(17, city.currConscription)
                assertEquals(4, city.getFlag(CityFlags.Resistance))
                assertEquals(original.tiles, city.tiles)
                assertEquals(original.cityConstructions.builtBuildings, city.cityConstructions.builtBuildings)
                assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
                assertEquals(original.garrison.map { it.unitName to it.amount },
                    city.garrison.map { it.unitName to it.amount })
                assertEquals(original.garrisonInfo.getAllTroops().map { it?.let { it.unitName to it.amount } },
                    city.garrisonInfo.getAllTroops().map { it?.let { it.unitName to it.amount } })
                // Characterize the missing city army ownership restoration independently of tile ownership.
                assertSame(MapUnit.monsterCivInfo, city.garrisonInfo.civInfo)
                assertNotSame(owner, city.garrisonInfo.civInfo)

                val territoryBefore = city.tiles.size
                assertNotNull(city.expansion.addNewTileForFree())
                assertEquals(territoryBefore + 1, city.tiles.size)
                loaded.nextTurn()
                assertEquals(2, loaded.turns)
                assertSame(city, loaded.getCities().single())
                assertSame(city, loaded.tileMap[city.location].getCity())
            } finally {
                CivilizationInfo.monsterGameInfo = previousMonsterGame
            }
        }
    }
}
