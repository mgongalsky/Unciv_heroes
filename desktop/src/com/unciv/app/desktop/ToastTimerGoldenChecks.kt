package com.unciv.app.desktop

import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.ui.UncivStage
import com.unciv.ui.popup.ToastPopup

/** Проверяет реальные таймеры между кадрами, не блокируя GL-диспетчер. */
internal object ToastTimerGoldenChecks {
    private class State {
        val stage = UncivStage(FitViewport(360f, 480f))
        val first = ToastPopup("Hidden then shown notification", stage, time = 150L)
        var firstClosed = 0
        var secondClosed = 0
        var secondShown = 0
        val second = ToastPopup("Queued notification", stage, time = 200L)
        var phase = 0
        var phaseStarted = System.nanoTime()

        init {
            first.closeListeners.add { firstClosed++ }
            second.closeListeners.add { secondClosed++ }
            second.showListeners.add { secondShown++ }
            check(first.isVisible && !second.isVisible)
            first.isVisible = false
        }

        fun dispose() {
            // Сначала снимаем очередь, чтобы очистка не запускала следующий таймер.
            second.close()
            first.close()
            stage.dispose()
        }
    }

    private var state: State? = null
    private var completed = false

    fun reset() {
        state?.dispose()
        state = null
        completed = false
    }

    fun ready(): Boolean {
        if (completed) return true
        val current = state ?: State().also { state = it }
        try {
            val elapsedMs = (System.nanoTime() - current.phaseStarted) / 1_000_000L
            check(elapsedMs < 5_000L) { "Таймер ToastPopup не завершил фазу ${current.phase}" }
            when (current.phase) {
                0 -> {
                    // Оба исходных срока истекли; скрытый и ожидающий toast должны оставаться на сцене.
                    if (elapsedMs < 400L) return false
                    check(current.first.parent != null && !current.first.isVisible && current.firstClosed == 0) {
                        "Скрытый ToastPopup закрылся по отменённому таймеру"
                    }
                    check(current.second.parent != null && !current.second.isVisible && current.secondClosed == 0) {
                        "Ожидающий ToastPopup запустил таймер до показа"
                    }
                    current.first.isVisible = true
                    current.phase = 1
                    current.phaseStarted = System.nanoTime()
                }
                1 -> {
                    if (current.first.parent != null) return false
                    check(current.firstClosed == 1) { "Автоматическое закрытие вызвано повторно" }
                    check(current.second.parent != null && current.second.isVisible && current.secondShown == 1) {
                        "Автоматическое закрытие не показало следующее уведомление"
                    }
                    current.first.close()
                    check(current.firstClosed == 1 && current.secondShown == 1)
                    current.phase = 2
                    current.phaseStarted = System.nanoTime()
                }
                2 -> {
                    if (current.second.parent != null) return false
                    check(current.secondClosed == 1 && current.secondShown == 1) {
                        "Таймер уведомления из очереди нарушил однократность закрытия"
                    }
                    current.second.close()
                    check(current.secondClosed == 1)
                    current.dispose()
                    state = null
                    completed = true
                    println("Toast timer checks passed: hide, show, queued expiry and repeated close")
                    return true
                }
            }
            return false
        } catch (failure: Throwable) {
            current.dispose()
            state = null
            throw failure
        }
    }
}
