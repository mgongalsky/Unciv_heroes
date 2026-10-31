package com.unciv.testing.city

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Загружает исторический JSON города, чтобы новый сериализатор не маскировал ошибки совместимости. */
class CityLegacyFixtureTest {
    /** Старые слоты армии и вложенный в цивилизацию город мигрируют в рабочую игру следующего формата. */
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
                // Вставляем исходный старый JSON без предварительной перезаписи слотов новым сериализатором.
                val legacyGame = JsonReader().parse(UncivFiles.gameInfoToString(source, false))
                legacyGame.remove("cities")
                val legacyCities = JsonValue(JsonValue.ValueType.array)
                legacyCities.addChild(JsonReader().parse(text))
                legacyGame.get("civilizations").get(0).addChild("cities", legacyCities)
                legacyGame.get("version").apply {
                    remove("number")
                    addChild("number", JsonValue(4L))
                }
                val loaded = UncivFiles.gameInfoFromString(legacyGame.toJson(JsonWriter.OutputType.json))
                assertEquals(GameInfo.CURRENT_COMPATIBILITY_VERSION, loaded.version)
                val restored = loaded.getCities().single()
                assertEquals(city.id, restored.id)
                assertEquals("Greece", restored.previousOwner)
                assertEquals(city.tiles, restored.tiles)
                assertEquals(listOf("Scout"), restored.cityConstructions.constructionQueue)
                assertSame(loaded.currentPlayerCiv, restored.civInfo)
                assertSame(restored, loaded.tileMap[restored.location].getCity())
                assertSame(loaded.currentPlayerCiv, restored.garrisonInfo.civInfo)
                restored.setFlag(CityFlags.ResourceDemand, 20)
                loaded.nextTurn()
                assertEquals(8, loaded.turns)
                assertSame(restored, loaded.getCities().single())
            } catch (failure: Exception) {
                // Форматирование некоторых игровых ошибок использует Current; сохраняем текст до очистки окружения.
                throw AssertionError(failure.stackTraceToString())
            } finally {
                CivilizationInfo.monsterGameInfo = previousMonsterGame
            }
        }
    }
}
