package com.unciv.testing.victory

import com.unciv.logic.automation.civilization.NextTurnAutomation
import com.unciv.logic.civilization.PlayerType
import com.unciv.testing.city.CityTestFixture
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.*
import org.junit.Test

/** Проверяет границу остановки выигранной партии без открытия экранов или сброса движения. */
class VictoryTurnFlowTest {
    /** Повторный вызов хода после результата не запускает действия и не меняет текущего игрока. */
    @Test fun recordedVictoryStopsTurnAndAutomation() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val unit = fixture.civilization.placeUnitNearTile(com.badlogic.gdx.math.Vector2(2f, 0f), "Warrior")!!
            unit.currentMovement = 0.75f
            val position = unit.currentTile.position.cpy()
            assertTrue(fixture.game.recordVictory("Rome", "CaptureCity"))
            val turn = fixture.game.turns
            val currentPlayer = fixture.game.currentPlayer
            NextTurnAutomation.automateCivMoves(fixture.civilization)
            fixture.game.nextTurn()
            assertEquals(turn, fixture.game.turns)
            assertEquals(currentPlayer, fixture.game.currentPlayer)
            assertEquals(position, unit.currentTile.position)
            assertEquals(0.75f, unit.currentMovement, 0f)
        }
    }

    /** Продолжение разрешает автоматизацию, но симуляция до победы всё равно завершается на результате. */
    @Test fun continuationAndSimulationHaveExplicitStopRules() {
        CityTestFixture().use { fixture ->
            fixture.game.recordVictory("Rome", "CaptureCity")
            assertTrue(fixture.game.shouldStopForVictory)
            fixture.game.oneMoreTurnMode = true
            assertFalse(fixture.game.shouldStopForVictory)
            fixture.game.simulateUntilWin = true
            assertTrue(fixture.game.shouldStopForVictory)
            fixture.game.nextTurn()
            assertFalse(fixture.game.simulateUntilWin)
            assertEquals("Rome", fixture.game.victoryWinner)
        }
    }

    /** Остановленный ход AI сохраняет исполнителя, но результат просматривается от лица человека. */
    @Test fun pausedAiVictoryKeepsHumanView() {
        CityTestFixture().use { fixture ->
            val ai = FakeCivilizationInfo().apply {
                civName = "Greece"
                nation = fixture.ruleset.nations.getValue(civName)
                gameInfo = fixture.game
                playerType = PlayerType.AI
            }
            fixture.game.civilizations.add(ai)
            ai.setTransients()
            fixture.game.recordVictory("Greece", "CaptureCity")
            fixture.game.currentPlayer = "Greece"
            fixture.game.currentPlayerCiv = ai
            assertSame(fixture.civilization, fixture.game.getPlayerToViewAs())
            fixture.game.nextTurn()
            assertSame(ai, fixture.game.currentPlayerCiv)
            assertEquals("Greece", fixture.game.victoryWinner)
        }
    }

    /** Второй участник и повторные проверки не заменяют первый результат даже в режиме продолжения. */
    @Test fun firstResultCannotBeReplaced() {
        CityTestFixture().use { fixture ->
            assertTrue(fixture.game.recordVictory("Rome", "CaptureCity"))
            fixture.game.oneMoreTurnMode = true
            assertFalse(fixture.game.recordVictory("Rome", "Scientific"))
            assertEquals("CaptureCity", fixture.civilization.victoryManager.getVictoryTypeAchieved())
            assertEquals("CaptureCity", fixture.game.clone().victoryType)
        }
    }
}
