package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonWriter
import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.logic.civilization.CivilizationInfo
import org.junit.Assert.*
import org.junit.Test

/** Проверяет явный столичный статус, независимый от сохранённого здания Palace. */
class CityCapitalStatusTest {
    /** Возвращение бывшей столицы не отменяет новую столицу и не удаляет здания ни одного города. */
    @Test
    fun acquiringFormerCapitalPreservesCurrentCapitalAndAllBuildingsAcrossSaves() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                // Пустой гарнизон позволяет вернуть нейтральный город без отдельного боевого сценария.
                val formerCapital = fixture.foundCity().apply {
                    garrisonInfo = ArmyInfo(fixture.civilization, 2)
                }
                val currentCapital = fixture.foundCity(Vector2(4f, 0f))
                val formerBuildings = formerCapital.cityConstructions.builtBuildings.toSet()
                NeutralCityOwnership.neutralize(formerCapital)
                assertFalse(formerCapital.isCapital())
                assertSame(currentCapital, fixture.civilization.getCapital())
                val currentBuildings = currentCapital.cityConstructions.builtBuildings.toSet()

                NeutralCityOwnership.acquireUndefended(formerCapital, fixture.civilization)

                assertSame(currentCapital, fixture.civilization.getCapital())
                assertFalse(formerCapital.isCapital())
                assertTrue(currentCapital.isCapital())
                assertEquals(formerBuildings, formerCapital.cityConstructions.builtBuildings.toSet())
                assertEquals(currentBuildings, currentCapital.cityConstructions.builtBuildings.toSet())
                assertEquals(1, fixture.civilization.cities.count { it.isCapital() })
                // Явное false должно пережить копирование и оба формата записи даже при наличии Palace.
                for (compressed in listOf(false, true)) {
                    val copy = fixture.game.clone()
                    assertFalse(copy.cityRegistry.byId(formerCapital.state.id)!!.isCapital())
                    val loaded = UncivFiles.gameInfoFromString(UncivFiles.gameInfoToString(copy, compressed))
                    loaded.setTransients()
                    assertEquals(currentCapital.id, loaded.civilizations.single().getCapital()!!.id)
                    val restored = loaded.cityRegistry.byId(formerCapital.state.id)!!
                    assertFalse(restored.isCapital())
                    assertEquals(false, restored.state.isCurrentCapital)
                    assertEquals(formerBuildings, restored.cityConstructions.builtBuildings.toSet())
                    assertEquals(currentBuildings, loaded.cityRegistry.byId(currentCapital.state.id)!!
                        .cityConstructions.builtBuildings.toSet())
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }

    /** Только отсутствующий в старом JSON статус однократно выводится из Palace при восстановлении. */
    @Test
    fun legacySaveWithoutCapitalStatusInfersItFromPalaceOnce() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val capital = fixture.foundCity()
                val other = fixture.foundCity(Vector2(4f, 0f))
                val tree = JsonReader().parse(json().toJson(fixture.game.clone()))
                for (entry in tree.get("cities")) entry.get("city").remove("isCurrentCapital")
                val loaded = UncivFiles.gameInfoFromString(tree.toJson(JsonWriter.OutputType.json))
                assertEquals(capital.id, loaded.civilizations.single().getCapital()!!.id)
                assertEquals(true, loaded.cityRegistry.byId(capital.state.id)!!.state.isCurrentCapital)
                assertEquals(false, loaded.cityRegistry.byId(other.state.id)!!.state.isCurrentCapital)
                loaded.setTransients()
                assertEquals(capital.id, loaded.civilizations.single().getCapital()!!.id)
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }

    /** Если у владельца нет столицы, первый полученный город назначается ею независимо от сохранённого Palace. */
    @Test
    fun firstAcquiredCityBecomesCapitalEvenWhenPalaceWasRetained() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity().apply { garrisonInfo = ArmyInfo(fixture.civilization, 2) }
            val buildings = city.cityConstructions.builtBuildings.toSet()
            NeutralCityOwnership.neutralize(city)
            assertFalse(city.isCapital())
            assertNull(fixture.civilization.getCapital())
            NeutralCityOwnership.acquireUndefended(city, fixture.civilization)
            assertSame(city, fixture.civilization.getCapital())
            assertTrue(city.isCapital())
            assertEquals(buildings, city.cityConstructions.builtBuildings.toSet())
        }
    }
}
