package com.unciv.ui.story

/** Управляет ресурсом ролика без доступа к партии: завершение сюжета подтверждает экран.
 * Повторные update/dispose и ошибка освобождения не вызывают повторного dispose плеера.
 */
class StoryVideoSession(private val createPlayer: () -> StoryVideoPlayer?) {
    enum class State { Ready, Playing, Completed, Fallback, Closed }

    var state = State.Ready
        private set
    var player: StoryVideoPlayer? = null
        private set

    /** Создание отложено до первого кадра уже открытого экрана. */
    fun start() {
        if (state != State.Ready) return
        try {
            player = createPlayer()
            state = if (player == null) State.Fallback else State.Playing
        } catch (_: Exception) {
            fail()
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
                StoryVideoStatus.Failed -> fail()
            }
        } catch (_: Exception) {
            fail()
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

    /** Уход с экрана сохраняет сюжетное событие для следующего открытия. */
    fun dispose() {
        if (state == State.Closed) return
        state = State.Closed
        release()
    }

    private fun control(action: (StoryVideoPlayer) -> Unit) {
        if (state != State.Playing) return
        try {
            action(player!!)
        } catch (_: Exception) {
            fail()
        }
    }

    private fun fail() {
        state = State.Fallback
        release()
    }

    private fun release() {
        val owned = player ?: return
        player = null
        try {
            owned.dispose()
        } catch (_: Exception) {
            // Ошибка очистки не должна блокировать fallback или возврат к партии.
        }
    }
}
