package com.unciv.ui.popup

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.utils.concurrency.Concurrency
import com.unciv.utils.concurrency.launchOnGLThread
import kotlinx.coroutines.delay

/** Уведомление, закрываемое нажатием или по истечении заданного времени в миллисекундах. */
class ToastPopup(message: String, stageToShowOn: Stage, val time: Long = 2000) : Popup(stageToShowOn) {
    private var timerJob: kotlinx.coroutines.Job? = null

    constructor(message: String, screen: BaseScreen, time: Long = 2000) : this(message, screen.stage, time)

    init {
        setFillParent(false)
        onClick { close() }
        if (UncivGame.Current.settings.skin == "Minimal") {
            background = null
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
            val scroll = children.filterIsInstance<AutoScrollPane>().single()
            scroll.setOverscroll(false, false)
            val scrollStyle = scroll.style
            val barWidth = maxOf(scrollStyle.vScroll?.minWidth ?: 0f, scrollStyle.vScrollKnob?.minWidth ?: 0f)
            val horizontalInsets = (scrollStyle.background?.leftWidth ?: 0f) + (scrollStyle.background?.rightWidth ?: 0f)
            val verticalInsets = (scrollStyle.background?.topHeight ?: 0f) + (scrollStyle.background?.bottomHeight ?: 0f)
            val label = message.toLabel(fontColor = Color.valueOf("453925")).apply {
                setWrap(true)
                setAlignment(Align.center)
            }
            val cell = add(label)
            // Учитываем рамку, отступы ячейки и полосу до первой раскладки ScrollPane.
            val maxTextWidth = stageToShowOn.width - 24f - innerTable.padLeft - innerTable.padRight -
                cell.padLeft - cell.padRight - barWidth - horizontalInsets
            cell.width(minOf(stageToShowOn.width / 2f, maxTextWidth).coerceAtLeast(1f))
            innerTable.pack()
            getCell(scroll).width(innerTable.prefWidth + barWidth + horizontalInsets)
                .height(minOf(innerTable.prefHeight + verticalInsets, (stageToShowOn.height - 40f).coerceAtLeast(1f)))
        } else {
            addGoodSizedLabel(message)
        }
        open()
        y = stageToShowOn.height - (height + 20f)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = Concurrency.run("ResponsePopup") {
            delay(time)
            launchOnGLThread {
                if (parent != null && isVisible) this@ToastPopup.close()
            }
        }
    }

    override fun setVisible(visible: Boolean) {
        val becomingVisible = visible && !isVisible
        super.setVisible(visible)
        if (becomingVisible) startTimer()
        else if (!visible) {
            timerJob?.cancel()
            timerJob = null
        }
    }

    /** Закрытие отменяет таймер; снятое уведомление не продвигает очередь повторно. */
    override fun close() {
        timerJob?.cancel()
        timerJob = null
        if (parent != null) super.close()
    }
}
