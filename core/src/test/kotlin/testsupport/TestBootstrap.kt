package testsupport

import com.unciv.UncivGame
import com.unciv.models.metadata.GameSettings
import com.unciv.ui.audio.MusicController

object TestBootstrap {
    fun installUncivStubs() {
        // Ensure GDX is initialized in headless mode
        GdxHeadless.ensure()

        // Install a minimal UncivGame.Current with no-IO stubs
        UncivGame.Current = UncivGame()
        UncivGame.Current.settings = GameSettings().apply {
            multiplayer.userId = "test-user"
        }
        // Real MusicController is fine here, nextTurn won't call chooseTrack for the first few turns
        UncivGame.Current.musicController = MusicController()
    }
}