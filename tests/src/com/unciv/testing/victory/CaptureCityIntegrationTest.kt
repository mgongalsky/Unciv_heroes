package com.unciv.testing.victory

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.*
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.scenario.ScenarioVictorySettings
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет производственные пути захвата и сохраняемый результат на настоящем игровом мире. */
class CaptureCityIntegrationTest {
    private fun target(fixture: CityTestFixture): CityInfo {
        fixture.foundCity()
        val city = CityInfo().apply {
            state.ownership = CityOwnership.Neutral
            location = Vector2(4f, 0f)
            tiles.add(location)
            garrisonInfo = ArmyInfo(null, 2)
            bindNeutralWorld(fixture.game)
            fixture.game.cityRegistry.register(this)
            setTransients()
        }
        fixture.game.tileMap.scenarioVictorySettings = ScenarioVictorySettings(captureObjectives = arrayListOf(
            CaptureCityObjective(city.id, arrayListOf("one"))
        ))
        fixture.game.scenarioVictoryParticipants["one"] = fixture.civilization.civName
        fixture.game.gameParameters.victoryTypes = arrayListOf("CaptureCity")
        return city
    }

    /** Пустые milestones встроенного типа не дают победу до фактического захвата. */
    @Test fun registeredCaptureVictoryDoesNotWinBeforeCapture() {
        CityTestFixture().use { fixture ->
            target(fixture)
            assertTrue("CaptureCity" in fixture.ruleset.victories)
            assertNull(fixture.civilization.victoryManager.getVictoryTypeAchieved())
            assertEquals("", fixture.game.victoryWinner)
        }
    }

    /** Реальный вход в пустой центр даёт победу человеку и AI; ввод изолирован как в тестах запуска. */
    @Test
    fun emptyNeutralEntryWinsForHumanAndAi() {
        val previousInput = com.badlogic.gdx.Gdx.input
        com.badlogic.gdx.Gdx.input = com.badlogic.gdx.backends.headless.mock.input.MockInput()
        try {
            for (playerType in listOf(PlayerType.Human, PlayerType.AI)) {
                CityTestFixture().use { fixture ->
                    val city = target(fixture)
                    fixture.civilization.playerType = playerType
                    val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
                    hero.currentMovement = 2f
                    hero.movement.moveToTile(city.getCenterTile())
                    assertSame(fixture.civilization, city.ownerOrNull)
                    assertEquals("Rome", fixture.game.victoryWinner)
                    assertEquals(
                        "CaptureCity",
                        fixture.civilization.victoryManager.getVictoryTypeAchieved()
                    )
                    assertFalse(NeutralCityEntry.tryAcquireOnEntry(hero, city.getCenterTile()))
                    assertEquals("Rome", fixture.game.victoryWinner)
                }
            }
        } finally {
            com.badlogic.gdx.Gdx.input = previousInput
        }
    }

    /** Результат нейтрального боя фиксируется после переноса героя в захваченный город. */
    @Test fun battleCaptureWinsAfterHeroArrives() {
        CityTestFixture().use { fixture ->
            val city = target(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            BattleWorldOutcomeHandler(MapUnitCombatant(hero), CityCombatant(city)).apply(true)
            assertSame(city.getCenterTile(), hero.currentTile)
            assertEquals("Rome", fixture.game.victoryWinner)
            assertEquals("CaptureCity", fixture.game.victoryType)
            fixture.game.oneMoreTurnMode = true
            city.state.ownership = CityOwnership.Neutral
            assertEquals("CaptureCity", fixture.civilization.victoryManager.getVictoryTypeAchieved())
        }
    }

    /** Получение владения через общую операцию не заменяет событие захвата назначенной цели. */
    @Test
    fun ownershipWithoutCaptureEventDoesNotWin() {
        CityTestFixture().use { fixture ->
            val city = target(fixture)
            com.unciv.logic.city.NeutralCityOwnership.acquireUndefended(city, fixture.civilization)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertNull(fixture.civilization.victoryManager.getVictoryTypeAchieved())
            fixture.civilization.victoryManager.onCityCaptured("unrelated-city")
            assertEquals("", fixture.game.victoryWinner)
        }
    }
}
