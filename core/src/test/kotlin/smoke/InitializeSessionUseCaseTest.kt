package smoke

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.GdxHeadless
import testsupport.TestBootstrap
import testsupport.TestGameFactory
import com.unciv.clean.application.usecases.InitializeSessionUseCase
import com.unciv.clean.adapters.ruleset.RulesetCacheProvider

class InitializeSessionUseCaseTest {
    @BeforeEach
    fun boot() {
        GdxHeadless.ensure()
        TestBootstrap.installUncivStubs()
    }

    @Test
    fun initializeSession_setsDifficultySpeedAndCurrentPlayer() {
        val game = TestGameFactory.minimalSingleHumanGame()
        InitializeSessionUseCase(RulesetCacheProvider()).execute(game)
        assertNotNull(game.difficultyObject)
        assertNotNull(game.speed)
        assertEquals(game.currentPlayer, game.currentPlayerCiv.civName)
    }
}
