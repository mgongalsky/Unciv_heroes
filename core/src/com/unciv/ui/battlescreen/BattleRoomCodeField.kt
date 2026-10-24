package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextField

/** Room-code paste is shared by the keyboard shortcut and the visible Paste button. */
class BattleRoomCodeField(skin: Skin, private val notify: (String) -> Unit) : TextField("", skin) {
    init {
        setOnlyFontChars(false)
        addCaptureListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                val control = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
                val shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                    Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                if ((control && keycode == Input.Keys.V) || (shift && keycode == Input.Keys.INSERT)) {
                    event.stop()
                    pasteCode()
                    return true
                }
                return false
            }
        })
    }

    fun pasteCode() {
        if (isDisabled) return
        val code = Gdx.app.clipboard.contents.orEmpty().trim()
        if (!code.matches(Regex("[A-Za-z0-9_-]{8}"))) {
            notify("Clipboard must contain an 8-character room code.")
            return
        }
        text = code
        clearSelection()
        cursorPosition = code.length
        stage?.keyboardFocus = this
        notify("Room code pasted. Press Join room.")
    }
}
