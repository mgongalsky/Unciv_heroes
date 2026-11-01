package com.unciv.ui.story

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.video.VideoPlayer

/** Адаптирует декодер к сюжетному экрану; владеет плеером и сохраняет пропорции кадра. */
class GdxStoryVideoPlayer private constructor(private val decoder: VideoPlayer) : StoryVideoPlayer {
    // Callback библиотеки может прийти из другого потока; ресурсы освобождаются только в GL-потоке.
    @Volatile private var completed = false
    private var disposed = false
    private var paused = false

    override val actor: Actor = object : Actor() {
        override fun draw(batch: Batch, parentAlpha: Float) {
            if (disposed || completed) return
            val texture = decoder.texture ?: return
            val videoWidth = decoder.videoWidth
            val videoHeight = decoder.videoHeight
            if (videoWidth <= 0 || videoHeight <= 0) return
            val scale = minOf(width / videoWidth, height / videoHeight)
            val drawWidth = videoWidth * scale
            val drawHeight = videoHeight * scale
            val previousColor = batch.packedColor
            try {
                batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
                batch.draw(texture, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f,
                    drawWidth, drawHeight, 0, 0, videoWidth, videoHeight, false, false)
            } finally {
                batch.packedColor = previousColor
            }
        }
    }

    override fun update(delta: Float): StoryVideoStatus {
        if (disposed) return StoryVideoStatus.Failed
        if (!completed && !paused) decoder.update()
        return if (completed) StoryVideoStatus.Completed else StoryVideoStatus.Playing
    }

    override fun pause() {
        if (disposed || completed || paused) return
        decoder.pause()
        paused = true
    }

    override fun resume() {
        if (disposed || completed || !paused) return
        decoder.play()
        paused = false
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        // Актёр не должен обращаться к уже освобождённой текстуре даже при повторном draw экрана.
        actor.remove()
        decoder.dispose()
    }

    companion object {
        /** При отказе загрузки фабрика освобождает частично созданный плеер. */
        fun create(assetPath: String, factory: () -> VideoPlayer): GdxStoryVideoPlayer {
            val result = GdxStoryVideoPlayer(factory())
            try {
                result.decoder.setOnCompletionListener { result.completed = true }
                result.decoder.setLooping(false)
                check(result.decoder.load(Gdx.files.internal(assetPath))) { "Could not load video: $assetPath" }
                result.decoder.play()
                return result
            } catch (failure: Exception) {
                try { result.dispose() } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
                throw failure
            } catch (failure: LinkageError) {
                // Отсутствующая native-библиотека должна приводить к существующей видеозаглушке.
                try { result.dispose() } catch (cleanup: Exception) { failure.addSuppressed(cleanup) }
                throw IllegalStateException("Video backend is unavailable", failure)
            }
        }
    }
}
