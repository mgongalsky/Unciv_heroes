package com.unciv.ui.story

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.video.VideoPlayer
import java.util.concurrent.FutureTask

/** Подготавливает сжатый файл в фоне; декодером и GL-ресурсами владеет только поток отрисовки. */
class GdxStoryVideoPlayer private constructor(
    source: FileHandle,
    private val factory: () -> VideoPlayer
) : StoryVideoPlayer {
    private var preparation: FutureTask<FileHandle>? = FutureTask {
        StoryVideoFilePreparation.prepare(source)
    }
    private var decoder: VideoPlayer? = null
    @Volatile private var completed = false
    private var disposed = false
    private var paused = false
    private var bufferStartedAt = 0L
    override var isPrepared = false
        private set

    init {
        Thread(preparation, "Story video file preparation").apply { isDaemon = true; start() }
        Gdx.graphics.requestRendering()
    }

    override val actor: Actor = object : Actor() {
        override fun draw(batch: Batch, parentAlpha: Float) {
            if (disposed || completed || !isPrepared) return
            val active = decoder ?: return
            val texture = active.texture ?: return
            val videoWidth = active.videoWidth
            val videoHeight = active.videoHeight
            if (videoWidth <= 0 || videoHeight <= 0) return
            val scale = minOf(width / videoWidth, height / videoHeight)
            val drawWidth = videoWidth * scale
            val drawHeight = videoHeight * scale
            val previousColor = batch.packedColor
            try {
                batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
                batch.draw(texture, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f,
                    drawWidth, drawHeight, 0, 0, videoWidth, videoHeight, false, false)
            } finally { batch.packedColor = previousColor }
        }
    }

    override fun update(delta: Float): StoryVideoStatus {
        if (disposed) return StoryVideoStatus.Failed
        if (completed) return StoryVideoStatus.Completed
        if (paused) return StoryVideoStatus.Playing
        // В режиме экономии энергии следующий кадр нужен и при подготовке, и при воспроизведении.
        Gdx.graphics.requestRendering()
        try {
            val pending = preparation
            if (pending != null) {
                if (!pending.isDone) return StoryVideoStatus.Playing
                val file = pending.get()
                preparation = null
                val created = factory()
                decoder = created
                created.setOnCompletionListener { completed = true }
                created.setLooping(false)
                check(created.load(file)) { "Could not load video: ${file.path()}" }
                bufferStartedAt = System.nanoTime()
            }
            val active = checkNotNull(decoder)
            if (!isPrepared) {
                // Короткий ролик может закончиться раньше заполнения буфера: ожидание ограничено.
                if (!active.isBuffered && System.nanoTime() - bufferStartedAt < 2_000_000_000L)
                    return StoryVideoStatus.Playing
                active.play()
                isPrepared = true
            }
            active.update()
            return if (completed) StoryVideoStatus.Completed else StoryVideoStatus.Playing
        } catch (failure: LinkageError) {
            throw IllegalStateException("Video backend is unavailable", failure)
        }
    }

    override fun pause() {
        if (disposed || completed || paused) return
        if (isPrepared) decoder?.pause()
        paused = true
    }

    override fun resume() {
        if (disposed || completed || !paused) return
        if (isPrepared) decoder?.resume()
        paused = false
        Gdx.graphics.requestRendering()
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        // Отмена не ждёт рабочий поток и не позволяет позднему результату создать декодер.
        preparation?.cancel(true)
        preparation = null
        actor.remove()
        val owned = decoder
        decoder = null
        owned?.dispose()
    }

    companion object {
        /** Возвращает управляемую подготовку сразу; ошибки чтения и загрузки обрабатывает видеосессия. */
        fun create(assetPath: String, factory: () -> VideoPlayer): GdxStoryVideoPlayer {
            val source = Gdx.files.internal(assetPath)
            require(source.exists()) { "Could not find video: $assetPath" }
            return GdxStoryVideoPlayer(source, factory)
        }
    }
}
