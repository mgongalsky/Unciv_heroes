package com.unciv.ui.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input

/** Preserve modifier state at event time when the backend drains queued key events. */
class KeyboardEventModifiers {
    private val pressed = mutableSetOf<Int>()
    private val modifiers = setOf(
        Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT,
        Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT, Input.Keys.SYM
    )

    fun <T> dispatch(keycode: Int, down: Boolean, action: () -> T): T {
        if (keycode in modifiers) {
            if (down) pressed.add(keycode) else pressed.remove(keycode)
        }
        val original = Gdx.input
        Gdx.input = object : Input by original {
            override fun isKeyPressed(key: Int): Boolean =
                if (key in modifiers) key in pressed else original.isKeyPressed(key)
        }
        return try { action() } finally { Gdx.input = original }
    }
}
