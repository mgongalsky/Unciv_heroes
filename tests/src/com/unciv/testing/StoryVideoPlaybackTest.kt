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

/** Проверяет настоящий декодер и его GL-текстуру на поставляемом ролике, без визуального эталона. */
class StoryVideoPlaybackTest {
    /** Ролик выдаёт кадры, заканчивается, затем повторно открывается и освобождается при пропуске. */
    @Test fun decodesCompletesAndSkipsIntro() {
        var failure: Throwable? = null
        var finished = false
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Story video integration")
            setWindowedMode(640, 360)
            setInitialVisible(false)
            setForegroundFPS(60)
        }
        Lwjgl3Application(object : ApplicationAdapter() {
            private var batch: SpriteBatch? = null
            private var decoder: VideoPlayerDesktop? = null
            private var session: StoryVideoSession? = null
            private var startedAt = 0L
            private var frames = 0
            private var lastTimestamp = -1
            private var progressed = false

            override fun create() {
                try {
                    batch = SpriteBatch()
                    startedAt = System.nanoTime()
                    session = StoryVideoSession {
                        GdxStoryVideoPlayer.create("cinematics/Audience_01.webm") {
                            VideoPlayerDesktop().also { decoder = it }
                        }.also { it.actor.setBounds(0f, 0f, 640f, 360f) }
                    }.also { it.start() }
                    assertEquals(StoryVideoSession.State.Playing, session!!.state)
                } catch (problem: Throwable) { failRun(problem) }
            }

            override fun render() {
                if (failure != null || finished) return
                try {
                    check(System.nanoTime() - startedAt < 30_000_000_000L) { "Video did not complete within 30 seconds" }
                    val active = session!!
                    active.update(Gdx.graphics.deltaTime)
                    assertNotEquals(StoryVideoSession.State.Fallback, active.state)
                    if (active.state == StoryVideoSession.State.Completed) {
                        assertTrue("No decoded video frames", frames > 10)
                        assertTrue("Video timestamp did not advance", progressed)
                        active.dispose()
                        val skipped = StoryVideoSession {
                            GdxStoryVideoPlayer.create("cinematics/Audience_01.webm") { VideoPlayerDesktop() }
                        }
                        session = skipped
                        skipped.start()
                        assertEquals(StoryVideoSession.State.Playing, skipped.state)
                        skipped.skip()
                        assertEquals(StoryVideoSession.State.Completed, skipped.state)
                        assertNull(skipped.player)
                        skipped.dispose()
                        skipped.dispose()
                        finished = true
                        Gdx.app.exit()
                        return
                    }
                    val nativePlayer = decoder!!
                    if (nativePlayer.texture != null) {
                        assertEquals(1280, nativePlayer.videoWidth)
                        assertEquals(704, nativePlayer.videoHeight)
                        val timestamp = nativePlayer.currentTimestamp
                        if (lastTimestamp >= 0 && timestamp > lastTimestamp) progressed = true
                        lastTimestamp = timestamp
                        frames++
                        batch!!.begin()
                        try { active.player!!.actor.draw(batch!!, 1f) }
                        finally { batch!!.end() }
                    }
                } catch (problem: Throwable) { failRun(problem) }
            }

            private fun failRun(problem: Throwable) {
                failure = problem
                Gdx.app.exit()
            }

            override fun dispose() {
                try { session?.dispose() }
                catch (problem: Throwable) { if (failure == null) failure = problem }
                finally { batch?.dispose() }
            }
        }, config)
        failure?.let { throw AssertionError("Real intro playback failed", it) }
        assertTrue("Playback check did not finish", finished)
    }
}
