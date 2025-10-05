package smoke

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testsupport.GdxHeadless
import testsupport.TestBootstrap
import testsupport.TestGameFactory
import com.unciv.clean.application.usecases.NextTurnUseCase
import com.unciv.clean.adapters.time.SystemClock
import com.unciv.clean.adapters.music.MusicAdapter
import com.unciv.clean.adapters.ai.AiAdapter
import com.unciv.clean.adapters.barbarians.BarbariansAdapter
import com.unciv.clean.adapters.session.UserSessionAdapter
import com.unciv.clean.adapters.log.LoggerAdapter
import com.unciv.clean.domain.events.DomainEvent

class NextTurnUseCaseTest {
    @BeforeEach
    fun boot() {
        GdxHeadless.ensure()
        TestBootstrap.installUncivStubs()
    }

    @Test
    fun nextTurn_emits_TurnAdvanced() {
        val game = TestGameFactory.minimalSingleHumanGame()
        val useCase = NextTurnUseCase(
            clock = SystemClock(),
            music = MusicAdapter(),
            ai = AiAdapter(),
            barbarians = BarbariansAdapter(game.barbarians),
            session = UserSessionAdapter(),
            logger = LoggerAdapter()
        )
        useCase.execute(game)
        val events = game.domainEvents.drain()
        assertTrue(events.any { it is DomainEvent.TurnAdvanced })
    }
}
