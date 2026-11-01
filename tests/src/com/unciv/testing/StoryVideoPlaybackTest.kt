package com.unciv.testing

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.video.VideoPlayerDesktop
import com.unciv.ui.story.GdxStoryVideoPlayer
import com.unciv.ui.story.StoryVideoSession
import org.junit.Assert.*
import org.junit.Test

/** Проверяет настоящий декодер без постоянной отрисовки, подготовку и освобождение при пропуске. */
class StoryVideoPlaybackTest {
    /** Кадры продвигаются без ввода; отменённая подготовка не создаёт декодер, повторный запуск допускает пропуск. */
    @Test fun decodesCompletesAndSkipsIntro() {
        var failure: Throwable? = null
        var finished = false
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Story video integration")
            setWindowedMode(640, 360)
            setInitialVisible(false)
            setForegroundFPS(60)
            setIdleFPS(60)
        }
        Lwjgl3Application(object : ApplicationAdapter() {
            private var batch: SpriteBatch? = null
            private var decoder: VideoPlayerDesktop? = null
            private var session: StoryVideoSession? = null
            private var watchdog: java.util.Timer? = null
            private var startedAt = 0L
            private var frames = 0
            private var lastTimestamp = -1
            private var phase = 0

            private fun startSession(): StoryVideoSession = StoryVideoSession {
                GdxStoryVideoPlayer.create("cinematics/Audience_01.webm") {
                    VideoPlayerDesktop().also { decoder = it }
                }.also { it.actor.setBounds(0f, 0f, 640f, 360f) }
            }.also { it.start() }

            override fun create() {
                try {
                    batch = SpriteBatch()
                    Gdx.graphics.isContinuousRendering = false
                    // Срабатывает только при зависании; не подталкивает обычный цикл кадрами.
                    watchdog = java.util.Timer("Video test timeout", true).apply {
                        schedule(object : java.util.TimerTask() {
                            override fun run() {
                                Gdx.app.postRunnable { if (!finished) failRun(AssertionError("Video stalled without input")) }
                            }
                        }, 30000L)
                    }
                    var cancelledFactoryCalls = 0
                    val cancelled = StoryVideoSession {
                        GdxStoryVideoPlayer.create("cinematics/Audience_01.webm") {
                            cancelledFactoryCalls++
                            VideoPlayerDesktop()
                        }
                    }
                    cancelled.start()
                    assertFalse(cancelled.player!!.isPrepared)
                    cancelled.skip()
                    cancelled.update(0f)
                    cancelled.dispose()
                    assertEquals(0, cancelledFactoryCalls)
                    startedAt = System.nanoTime()
                    session = startSession()
                    assertEquals(StoryVideoSession.State.Playing, session!!.state)
                    assertFalse(session!!.player!!.isPrepared)
                } catch (problem: Throwable) { failRun(problem) }
            }

            override fun render() {
                if (failure != null || finished) return
                try {
                    assertFalse("Player changed the user's rendering setting", Gdx.graphics.isContinuousRendering)
                    val active = session!!
                    active.update(Gdx.graphics.deltaTime)
                    assertNotEquals(StoryVideoSession.State.Fallback, active.state)
                    if (active.state == StoryVideoSession.State.Completed) {
                        assertEquals(0, phase)
                        assertTrue("Too few distinct video timestamps: $frames", frames > 30)
                        assertTrue("Intro playback took too long", System.nanoTime() - startedAt < 20_000_000_000L)
                        active.dispose()
                        phase = 1
                        decoder = null
                        session = startSession()
                        return
                    }
                    if (active.player?.isPrepared != true) return
                    val nativePlayer = checkNotNull(decoder)
                    if (nativePlayer.texture == null) return
                    assertEquals(1280, nativePlayer.videoWidth)
                    assertEquals(704, nativePlayer.videoHeight)
                    val timestamp = nativePlayer.currentTimestamp
                    if (timestamp != lastTimestamp) frames++
                    lastTimestamp = timestamp
                    batch!!.begin()
                    try { active.player!!.actor.draw(batch!!, 1f) }
                    finally { batch!!.end() }
                    if (phase == 1) {
                        active.skip()
                        assertEquals(StoryVideoSession.State.Completed, active.state)
                        assertNull(active.player)
                        active.dispose()
                        active.dispose()
                        finished = true
                        Gdx.app.exit()
                    }
                } catch (problem: Throwable) { failRun(problem) }
            }

            private fun failRun(problem: Throwable) {
                failure = problem
                Gdx.app.exit()
            }

            override fun dispose() {
                watchdog?.cancel()
                try { session?.dispose() }
                catch (problem: Throwable) { if (failure == null) failure = problem }
                finally { batch?.dispose() }
            }
        }, config)
        failure?.let { throw AssertionError("Real intro playback failed", it) }
        assertTrue("Playback check did not finish", finished)
    }
}
