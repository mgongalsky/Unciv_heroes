package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.map.MapUnit
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CityLegacyFixtureTest {
    @Test
    fun preRegistryCityWithLegacyArmySlotsLoadsAndRuns() {
        CityTestFixture().use { fixture ->
            val previousMonsterGame = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val root = System.getProperty("golden.projectRoot")
                    ?: error("golden.projectRoot must identify the repository")
                val text = File(root, "tests/resources/city/pre-registry-city.json").readText()
                val city = json().fromJson(CityInfo::class.java, text)
                assertEquals("legacy-city-rome-001", city.id)
                assertEquals("Greece", city.previousOwner)
                assertEquals(2, city.population.population)
                assertEquals(3, city.population.foodStored)
                assertEquals(17, city.currConscription)
                assertTrue(city.garrison.isEmpty())
                assertEquals(listOf("Archer" to 35, "Peasant" to 20, null, null),
                    city.garrisonInfo.getAllTroops().map { it?.let { it.unitName to it.amount } })

                val source = fixture.game.clone()
                source.version = GameInfo.CURRENT_COMPATIBILITY_VERSION
                source.turns = 7
                source.civilizations.single().cities = arrayListOf(city)
                source.civilizations.single().hasEverOwnedOriginalCapital = true
                val loaded = UncivFiles.gameInfoFromString(UncivFiles.gameInfoToString(source, false))
                val restored = loaded.getCities().single()
                assertEquals(city.id, restored.id)
                assertEquals("Greece", restored.previousOwner)
                assertEquals(city.tiles, restored.tiles)
                assertEquals(listOf("Scout"), restored.cityConstructions.constructionQueue)
                assertSame(loaded.currentPlayerCiv, restored.civInfo)
                assertSame(restored, loaded.tileMap[restored.location].getCity())
                assertSame(MapUnit.monsterCivInfo, restored.garrisonInfo.civInfo)
                restored.setFlag(CityFlags.ResourceDemand, 20)
                loaded.nextTurn()
                assertEquals(8, loaded.turns)
                assertSame(restored, loaded.getCities().single())
            } catch (failure: Exception) {
                // Some game exceptions format their message using Current, which cleanup restores.
                throw AssertionError(failure.stackTraceToString())
            } finally {
                CivilizationInfo.monsterGameInfo = previousMonsterGame
            }
        }
    }
}
