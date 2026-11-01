package com.unciv.ui.story

/** Управляет ресурсом ролика; завершение сюжета подтверждает экран, освобождение выполняется однократно. */
class StoryVideoSession(private val createPlayer: () -> StoryVideoPlayer?) {
    enum class State { Ready, Playing, Completed, Fallback, Closed }
    /** Причина замены ролика отделена от состояния, чтобы UI мог объяснить результат подготовки. */
    enum class FallbackReason { Unavailable, StartupFailed, PlaybackFailed, ControlFailed }

    var state = State.Ready
        private set
    var player: StoryVideoPlayer? = null
        private set
    var fallbackReason: FallbackReason? = null
        private set

    /** Экран вызывает создание после отображения кадра подготовки. */
    fun start() {
        if (state != State.Ready) return
        try {
            player = createPlayer()
            if (player == null) fail(FallbackReason.Unavailable)
            else state = State.Playing
        } catch (_: Exception) {
            fail(FallbackReason.StartupFailed)
        }
    }

    fun update(delta: Float) {
        if (state != State.Playing) return
        try {
            when (player!!.update(delta)) {
                StoryVideoStatus.Playing -> Unit
                StoryVideoStatus.Completed -> {
                    state = State.Completed
                    release()
                }
                StoryVideoStatus.Failed -> fail(FallbackReason.PlaybackFailed)
            }
        } catch (_: Exception) {
            fail(FallbackReason.PlaybackFailed)
        }
    }

    fun pause() = control { it.pause() }
    fun resume() = control { it.resume() }

    /** Пропуск разрешает экран согласно описанию сцены. */
    fun skip() {
        if (state != State.Playing) return
        state = State.Completed
        release()
    }

    /** Закрытие не подтверждает сюжетное событие и не повторяет освобождение ресурса. */
    fun dispose() {
        if (state == State.Closed) return
        state = State.Closed
        release()
    }

    private fun control(action: (StoryVideoPlayer) -> Unit) {
        if (state != State.Playing) return
        try { action(player!!) }
        catch (_: Exception) { fail(FallbackReason.ControlFailed) }
    }

    private fun fail(reason: FallbackReason) {
        fallbackReason = reason
        state = State.Fallback
        release()
    }

    private fun release() {
        val owned = player ?: return
        player = null
        try { owned.dispose() }
        catch (_: Exception) {
            // Ошибка очистки не скрывает исходную причину и не блокирует возврат к партии.
        }
    }
}
