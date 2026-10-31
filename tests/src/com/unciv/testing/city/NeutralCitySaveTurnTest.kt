package com.unciv.testing.city

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.city.CityFlags
import com.unciv.logic.civilization.CivilizationInfo
import org.junit.Assert.*
import org.junit.Test

/** Проверяет сохранность нейтрального города при чередовании ходов, копирования и загрузки игры. */
class NeutralCitySaveTurnTest {
    /** Обычный и сжатый форматы сохраняют статичное состояние города и отсутствие владельца после трёх ходов. */
    @Test
    fun loadedNeutralCitySurvivesTurnsAndBothSaveFormats() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val original = fixture.foundCity().apply {
                    health = 91
                    population.foodStored = 17
                    currConscription = 13
                    setFlag(CityFlags.Resistance, 4)
                    cityConstructions.constructionQueue.clear()
                    cityConstructions.constructionQueue.add("Scout")
                    cityConstructions.inProgressConstructions["Scout"] = 23
                }
                fixture.game.turns = 1
                val tree = JsonReader().parse(json().toJson(fixture.game.clone()))
                // Формируем входной JSON нейтрального города напрямую: здесь проверяется загрузка, не операция владения.
                tree.get("cities").get(0).get("ownership").apply {
                    get("type").set("Neutral")
                    remove("civName")
                }
                val source = tree.toJson(JsonWriter.OutputType.json)
                for (compressed in listOf(false, true)) {
                    val game = UncivFiles.gameInfoFromString(source)
                    repeat(2) { game.nextTurn() }
                    val saved = UncivFiles.gameInfoToString(game.clone(), compressed)
                    val loaded = UncivFiles.gameInfoFromString(saved)
                    loaded.setTransients()
                    loaded.nextTurn()
                    val city = loaded.getCities().single()
                    assertEquals(4, loaded.turns)
                    assertEquals(original.id, city.id)
                    assertEquals(91, city.health)
                    assertEquals(17, city.population.foodStored)
                    assertEquals(13, city.currConscription)
                    assertEquals(4, city.getFlag(CityFlags.Resistance))
                    assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
                    assertEquals(mapOf("Scout" to 23), city.cityConstructions.inProgressConstructions)
                    assertEquals(original.tiles, city.tiles)
                    assertEquals(original.garrisonInfo.getAllTroops().map { it?.amount },
                        city.garrisonInfo.getAllTroops().map { it?.amount })
                    assertNull(city.ownerOrNull)
                    assertNull(city.garrisonInfo.ownerOrNull)
                    assertSame(city, loaded.tileMap[city.location].getCity())
                    assertNull(loaded.tileMap[city.location].getOwner())
                    assertTrue(loaded.civilizations.single().cities.isEmpty())
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
