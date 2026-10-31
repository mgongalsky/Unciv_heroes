package com.unciv.testing.story

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.MapSaver
import com.unciv.logic.scenario.InstantiateScenarioHeroesUseCase
import com.unciv.pure.domain.city.CityPosition
import com.unciv.pure.domain.city.ScenarioPlayerSlot
import com.unciv.pure.domain.hero.ScenarioHero
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет адаптер расстановки в настоящем мире отдельно от чистых правил подготовки. */
class ScenarioHeroInstantiationTest {
    private fun configure(fixture: CityTestFixture) {
        fixture.game.tileMap.scenarioPlayerSlots.add(ScenarioPlayerSlot("main"))
        fixture.game.scenarioVictoryParticipants["main"] = fixture.civilization.civName
        fixture.game.tileMap.scenarioHeroes.add(
            ScenarioHero("guide", "Проводник", "Warrior", "main", CityPosition(0f, 0f))
        )
    }

    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Ожидался отказ расстановки")
        } catch (_: IllegalArgumentException) { }
    }

    /** Герой занимает точную клетку, регистрируется один раз и сохраняет личность в копии. */
    @Test
    fun placesNamedHeroExactlyOnce() {
        CityTestFixture().use { fixture ->
            configure(fixture)
            val unit = InstantiateScenarioHeroesUseCase.execute(fixture.game).single()
            assertEquals(Vector2.Zero, unit.currentTile.position)
            assertSame(unit, fixture.game.tileMap[Vector2.Zero].militaryUnit)
            assertEquals(1, fixture.civilization.getCivUnits().count())
            assertEquals("guide", unit.scenarioHeroId)
            assertEquals("Проводник", unit.instanceName)
            val copy = unit.clone()
            assertEquals(unit.scenarioHeroId, copy.scenarioHeroId)
            copy.instanceName = "Другое имя"
            assertEquals("Проводник", unit.instanceName)
            rejects { InstantiateScenarioHeroesUseCase.execute(fixture.game) }
            assertEquals(1, fixture.civilization.getCivUnits().count())
        }
    }

    /** Ошибка второго размещения не публикует первого героя и не изменяет чужую занятую клетку. */
    @Test
    fun occupiedSecondPositionLeavesWorldUnchanged() {
        CityTestFixture().use { fixture ->
            configure(fixture)
            val occupied = fixture.civilization.placeUnitNearTile(Vector2(1f, 0f), "Warrior")!!
            fixture.game.tileMap.scenarioHeroes.add(
                ScenarioHero("other", "Спутник", "Warrior", "main", CityPosition(1f, 0f))
            )
            rejects { InstantiateScenarioHeroesUseCase.execute(fixture.game) }
            assertNull(fixture.game.tileMap[Vector2.Zero].militaryUnit)
            assertSame(occupied, fixture.civilization.getCivUnits().single())
            assertSame(occupied, fixture.game.tileMap[Vector2(1f, 0f)].militaryUnit)
        }
    }

    /** Оба формата карты сохраняют описания без создания юнитов; копия карты отделяет черновик героя. */
    @Test
    fun mapRoundTripAndCloneKeepDescriptionsSeparate() {
        CityTestFixture().use { fixture ->
            configure(fixture)
            val map = fixture.game.tileMap
            val oldZip = MapSaver.saveZipped
            try {
                for (zipped in listOf(false, true)) {
                    MapSaver.saveZipped = zipped
                    val restored = MapSaver.mapFromSavedString(MapSaver.mapToSavedString(map), false)
                    assertEquals(map.scenarioHeroes, restored.scenarioHeroes)
                    assertTrue(restored.values.all { it.getUnits().none() })
                }
            } finally {
                MapSaver.saveZipped = oldZip
            }
            val copy = map.clone()
            copy.scenarioHeroes.single().name = "Изменено"
            assertEquals("Проводник", map.scenarioHeroes.single().name)
        }
    }

    /** Карта предыдущей версии получает пустой список героев, не меняя прежние юниты. */
    @Test
    fun oldMapHasNoScenarioHeroes() {
        val old = """{"mapParameters":{"mapFormatVersion":3},"tileList":[]}"""
        assertTrue(MapSaver.mapFromSavedString(old, false).scenarioHeroes.isEmpty())
    }

    /** Замена юнита использует тот же перенос характеристик, что улучшение, и сохраняет сюжетную ссылку. */
    @Test
    fun replacementKeepsScenarioIdentityAndName() {
        CityTestFixture().use { fixture ->
            configure(fixture)
            val hero = InstantiateScenarioHeroesUseCase.execute(fixture.game).single()
            val replacement = fixture.civilization.placeUnitNearTile(Vector2(1f, 0f), "Spearman")!!
            hero.copyStatisticsTo(replacement)
            assertEquals("guide", replacement.scenarioHeroId)
            assertEquals("Проводник", replacement.instanceName)
        }
    }

    /** Старт в нейтральном городе отклоняется до публикации героя и не превращается в захват. */
    @Test
    fun neutralCityPlacementDoesNotAcquireCity() {
        CityTestFixture().use { fixture ->
            configure(fixture)
            fixture.game.tileMap.scenarioCities.add(
                com.unciv.pure.domain.city.ScenarioCity(
                    id = "neutral", name = "Нейтральный город",
                    typeMode = com.unciv.pure.domain.city.ScenarioCityTypeMode.Fixed,
                    fixedType = "Default"
                )
            )
            val town = com.unciv.logic.city.InstantiateScenarioCitiesUseCase.execute(
                fixture.game, mapOf("main" to fixture.civilization)
            ).single()
            rejects { InstantiateScenarioHeroesUseCase.execute(fixture.game) }
            assertNull(town.ownerOrNull)
            assertTrue(fixture.civilization.getCivUnits().none())
            assertNull(fixture.game.tileMap[Vector2.Zero].militaryUnit)
        }
    }
}
