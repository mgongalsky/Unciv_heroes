package com.unciv.ui.story

import com.badlogic.gdx.scenes.scene2d.Actor

/** Платформенный видеоплеер. Все методы вызываются в GL-потоке.
 * Реализация владеет своими ресурсами; общий атлас интерфейса ей не принадлежит.
 * Ошибка загрузки или декодирования передаётся исключением либо состоянием Failed.
 */
interface StoryVideoPlayer {
    /** Актёр отображает видео в выделенной области с сохранением пропорций. */
    val actor: Actor
    fun update(delta: Float): StoryVideoStatus
    fun pause()
    fun resume()
    fun dispose()
}

/** Состояние декодера; неподдерживаемая платформа не создаёт проигрыватель. */
enum class StoryVideoStatus { Playing, Completed, Failed }
