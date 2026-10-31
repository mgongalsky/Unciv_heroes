package com.unciv.testing.city

import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет сохранение нейтрального города в общем реестре независимо от списков цивилизаций. */
class NeutralCityRegistryPersistenceTest {
    /** Чтение, восстановление связей, копирование и повторная запись не назначают нейтральному городу владельца. */
    @Test
    fun canonicalNeutralCityReadsAndClonesWithoutEnteringCivilizationLists() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val original = fixture.foundCity()
                original.health = 91
                original.population.foodStored = 17
                val tree = JsonReader().parse(json().toJson(fixture.game.clone()))
                tree.get("cities").get(0).get("ownership").apply {
                    get("type").set("Neutral")
                    remove("civName")
                }
                val loaded = json().fromJson(GameInfo::class.java, tree.toJson(JsonWriter.OutputType.json))
                val city = loaded.getCities().single()
                assertEquals(CityOwnership.Neutral, city.state.ownership)
                assertNull(city.ownerOrNull)
                assertNull(city.garrisonInfo.ownerOrNull)
                assertSame(loaded, city.gameInfo)
                assertSame(city, loaded.cityRegistry.byId(city.state.id))
                assertSame(city, loaded.cityRegistry.at(city.state.position))
                assertTrue(loaded.civilizations.single().cities.isEmpty())

                // Копируем полностью восстановленный мир с менеджерами цивилизации и нейтральной территорией.
                loaded.setTransients()
                assertSame(city, loaded.tileMap[city.location].getCity())
                assertTrue(loaded.tileMap[city.location].isCityCenter())
                assertNull(loaded.tileMap[city.location].getOwner())
                val copy = loaded.clone()
                val copiedCity = copy.getCities().single()
                assertNotSame(city, copiedCity)
                assertNotSame(city.state.garrison, copiedCity.state.garrison)
                assertSame(copy, copiedCity.gameInfo)
                assertEquals(city.id, copiedCity.id)
                assertEquals(91, copiedCity.health)
                assertEquals(17, copiedCity.population.foodStored)
                assertNull(copiedCity.garrisonInfo.ownerOrNull)
                assertTrue(copy.civilizations.single().cities.isEmpty())
                val encoded = JsonReader().parse(json().toJson(copy))
                // Город записывается один раз в корне, без дублирующего контейнера у цивилизации.
                assertEquals(1, encoded.get("cities").size)
                assertEquals("Neutral", encoded.get("cities").get(0).get("ownership").getString("type"))
                assertFalse(encoded.get("civilizations").get(0).has("cities"))
                val restored = json().fromJson(GameInfo::class.java, encoded.toJson(JsonWriter.OutputType.json))
                assertEquals(city.id, restored.getCities().single().id)
                assertNull(restored.getCities().single().garrisonInfo.ownerOrNull)
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
