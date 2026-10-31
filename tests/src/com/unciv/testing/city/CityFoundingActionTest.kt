package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import com.unciv.models.ruleset.tile.TileImprovement
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.ui.worldscreen.unit.UnitActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

/** Проверяет полное действие основания через UnitActions, включая удаление основателя. */
class CityFoundingActionTest {
    /** Подменяет только ввод LibGDX и возвращает прежнее значение после каждого теста. */
    @get:Rule
    val inputEnvironment = object : ExternalResource() {
        private var previous: Input? = null
        override fun before() {
            previous = Gdx.input
            Gdx.input = MockInput()
        }
        override fun after() { Gdx.input = previous }
    }

    /** Для человека и AI обучение предшествует основанию, а обновление UI видит готовый мир. */
    @Test
    fun humanAndAiFoundingApplyWorldEffectsBeforeUpdatingUi() {
        for (playerType in listOf(PlayerType.Human, PlayerType.AI)) {
            CityTestFixture().use { fixture ->
                fixture.civilization.playerType = playerType
                // Явно включаем ветвь улучшения центра, зависящую от состава правил.
                fixture.ruleset.tileImprovements.putIfAbsent("City center", TileImprovement().apply {
                    name = "City center"
                })
                val tile = fixture.game.tileMap[Vector2(0f, 0f)]
                val definition = fixture.ruleset.units.values.first { it.hasUnique(UniqueType.FoundCity) }
                val unit = MapUnit().apply {
                    civInfo = fixture.civilization
                    owner = civInfo.civName
                    name = definition.name
                    currentTile = tile
                    setTransients(fixture.ruleset)
                    assignOwner(fixture.civilization)
                    putInTile(tile)
                    currentMovement = 2f
                }
                tile.improvement = "Small village"
                tile.roadStatus = RoadStatus.Road
                val events = mutableListOf<String>()
                val action = UnitActions.getFoundCityAction(
                    unit, tile,
                    // Обработчики проверяют порядок без записи настроек и создания WorldScreen.
                    onFoundingTutorial = {
                        assertTrue(fixture.civilization.cities.isEmpty())
                        assertFalse(unit.isDestroyed)
                        events.add("tutorial")
                    },
                    onWorldUpdated = {
                        val city = fixture.civilization.cities.single()
                        assertSame(city, tile.getCity())
                        assertEquals("City center", tile.improvement)
                        assertEquals(RoadStatus.None, tile.roadStatus)
                        assertTrue(unit.isDestroyed)
                        assertFalse(tile.getUnits().any { it === unit })
                        assertFalse(fixture.civilization.getCivUnits().any { it === unit })
                        events.add("world")
                    },
                    confirmBrokenPromise = { _, _ -> fail("No diplomatic promise exists in this scenario") }
                )
                assertNotNull(action?.action)
                action!!.action!!.invoke()
                assertEquals(listOf("tutorial", "world"), events)
                assertEquals(1, fixture.civilization.citiesCreated)
                assertEquals(0f, unit.currentMovement, 0f)
            }
        }
    }
}
