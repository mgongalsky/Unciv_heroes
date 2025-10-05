package smoke

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.GdxHeadless
import testsupport.TestBootstrap
import testsupport.TestGameFactory
import com.unciv.logic.GameInfo
import com.unciv.json.json

class GameInfoTurnTest {
    @BeforeEach
    fun boot() {
        GdxHeadless.ensure()
        TestBootstrap.installUncivStubs()
    }

    @Test
    fun `nextTurn increments turn and keeps same player in single-civ game`() {
        val game = TestGameFactory.minimalSingleHumanGame()
        val beforeTurn = game.turns
        val beforePlayer = game.currentPlayer

        game.nextTurn()

        assertEquals(beforeTurn + 1, game.turns)
        assertEquals(beforePlayer, game.currentPlayer)
    }

    @Test
    fun `save-load roundtrip keeps basic fields`() {
        val game: GameInfo = TestGameFactory.minimalSingleHumanGame()
        val js = json()
        val jsonText = js.toJson(game)
        val loaded = js.fromJson(GameInfo::class.java, jsonText)
        assertEquals(game.currentPlayer, loaded.currentPlayer)
        assertEquals(game.turns, loaded.turns)
        assertEquals(game.civilizations.size, loaded.civilizations.size)
    }
}