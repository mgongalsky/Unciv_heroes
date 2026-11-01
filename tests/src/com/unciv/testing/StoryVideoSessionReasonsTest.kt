package com.unciv.testing

import com.badlogic.gdx.scenes.scene2d.Actor
import com.unciv.ui.story.StoryVideoPlayer
import com.unciv.ui.story.StoryVideoSession
import com.unciv.ui.story.StoryVideoStatus
import org.junit.Assert.*
import org.junit.Test

/** Проверяет диагностику адаптера видео без запуска GL и настоящего декодера. */
class StoryVideoSessionReasonsTest {
    private class Player : StoryVideoPlayer {
        var disposals = 0
        var pauseFails = false
        override val actor: Actor get() = error("Session must not access UI")
        override fun update(delta: Float) = StoryVideoStatus.Failed
        override fun pause() { if (pauseFails) error("pause failure") }
        override fun resume() = Unit
        override fun dispose() { disposals++ }
    }

    /** Отсутствие плеера отличается от ошибки его создания; повторной подготовки нет. */
    @Test fun distinguishesUnavailableAndStartupFailure() {
        var attempts = 0
        val unsupported = StoryVideoSession { attempts++; null }
        assertNull(unsupported.fallbackReason)
        unsupported.start()
        unsupported.start()
        assertEquals(1, attempts)
        assertEquals(StoryVideoSession.FallbackReason.Unavailable, unsupported.fallbackReason)
        val startup = StoryVideoSession { error("open failure") }
        startup.start()
        assertEquals(StoryVideoSession.FallbackReason.StartupFailed, startup.fallbackReason)
    }

    /** Ошибка декодирования сохраняет свою причину после освобождения и закрытия. */
    @Test fun playbackFailureRetainsReasonAfterClose() {
        val player = Player()
        val session = StoryVideoSession { player }
        session.start()
        session.update(0f)
        session.dispose()
        assertEquals(StoryVideoSession.FallbackReason.PlaybackFailed, session.fallbackReason)
        assertEquals(1, player.disposals)
    }

    /** Ошибка управления объясняется отдельно и освобождает плеер ровно один раз. */
    @Test fun controlFailureReleasesPlayerOnce() {
        val player = Player().apply { pauseFails = true }
        val session = StoryVideoSession { player }
        session.start()
        session.pause()
        session.resume()
        session.dispose()
        assertEquals(StoryVideoSession.FallbackReason.ControlFailed, session.fallbackReason)
        assertEquals(1, player.disposals)
    }
}
