package com.unciv.testing

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.ui.battlescreen.BattleRoomCodeField
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.extensions.installShortcutDispatcher
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference

class BattleClipboardTest {
    @Test
    fun keyboardAndButtonPasteReachFocusedFields() {
        val failure = AtomicReference<Throwable?>()
        var completed = false
        val config = Lwjgl3ApplicationConfiguration().apply {
            setInitialVisible(false)
            setWindowedMode(640, 480)
            disableAudio(true)
        }
        Lwjgl3Application(object : ApplicationAdapter() {
            override fun create() {
                val originalInput = Gdx.input
                val clipboard = Gdx.app.clipboard
                val previous = clipboard.contents
                val stage = object : Stage() {
                    private val modifiers = com.unciv.ui.utils.KeyboardEventModifiers()
                    override fun keyDown(keyCode: Int): Boolean =
                        modifiers.dispatch(keyCode, true) { super.keyDown(keyCode) }
                    override fun keyUp(keyCode: Int): Boolean =
                        modifiers.dispatch(keyCode, false) { super.keyUp(keyCode) }
                }
                val skin = Skin()
                try {
                    val nativeData = com.unciv.ui.utils.NativeBitmapFontData(
                        com.unciv.app.desktop.NativeFontDesktop(50, "")
                    )
                    val nativeFont = BitmapFont(nativeData, nativeData.regions, false)
                    skin.add("default", TextField.TextFieldStyle(nativeFont, Color.WHITE, null, null, null))
                    stage.installShortcutDispatcher()
                    Gdx.input = Proxy.newProxyInstance(Input::class.java.classLoader, arrayOf(Input::class.java)) { _, method, args ->
                        if (method.name == "isKeyPressed") {
                            when (args!![0] as Int) {
                                Input.Keys.CONTROL_LEFT, Input.Keys.CONTROL_RIGHT -> false
                                Input.Keys.SHIFT_LEFT, Input.Keys.SHIFT_RIGHT -> false
                                else -> method.invoke(originalInput, *args)
                            }
                        } else method.invoke(originalInput, *(args ?: emptyArray()))
                    } as Input
                    val baseline = Stage()
                    try {
                        val field = TextField("unchanged", skin)
                        baseline.addActor(field)
                        baseline.keyboardFocus = field
                        field.selectAll()
                        clipboard.contents = "https://heroic-civs.duckdns.org"
                        baseline.keyDown(Input.Keys.CONTROL_LEFT)
                        baseline.keyDown(Input.Keys.V)
                        baseline.keyUp(Input.Keys.V)
                        baseline.keyUp(Input.Keys.CONTROL_LEFT)
                        assertEquals("Standard scene misses queued fast paste", "unchanged", field.text)
                    } finally { baseline.dispose() }
                    val address = TextField("old address", skin)
                    stage.addActor(AutoScrollPane(address))
                    stage.keyboardFocus = address
                    address.selectAll()
                    clipboard.contents = "https://heroic-civs.duckdns.org"
                    stage.keyDown(Input.Keys.CONTROL_LEFT)
                    stage.keyDown(Input.Keys.V)
                    stage.keyUp(Input.Keys.V)
                    stage.keyUp(Input.Keys.CONTROL_LEFT)
                    assertEquals("Ctrl+V must replace the selected address", "https://heroic-civs.duckdns.org", address.text)
                    address.selectAll()
                    address.setClipboard(object : com.badlogic.gdx.utils.Clipboard {
                        override fun hasContents() = clipboard.hasContents()
                        override fun setContents(content: String) { clipboard.contents = content }
                        override fun getContents(): String? {
                            val content = clipboard.contents
                            println("SHIFT_PASTE_READ: content=$content ctrl=${Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)} shift=${Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)}")
                            assertEquals("Clipboard read by TextField during Shift+Insert", "https://example.org", content)
                            return content
                        }
                    })
                    address.addCaptureListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
                        override fun keyDown(event: com.badlogic.gdx.scenes.scene2d.InputEvent, keycode: Int): Boolean {
                            if (keycode == Input.Keys.INSERT) {
                                assertTrue("Shift present during Insert dispatch", Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT))
                                assertFalse("Ctrl released before Insert dispatch", Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
                            }
                            return false
                        }
                    })
                    clipboard.contents = "https://example.org"
                    stage.keyDown(Input.Keys.SHIFT_LEFT)
                    stage.keyDown(Input.Keys.INSERT)
                    stage.keyUp(Input.Keys.INSERT)
                    stage.keyUp(Input.Keys.SHIFT_LEFT)
                    assertEquals("Shift+Insert must replace selection", "https://example.org", address.text)
                    val code = BattleRoomCodeField(skin) {}
                    stage.addActor(code)
                    stage.keyboardFocus = code
                    clipboard.contents = "Ab12_-xy"
                    stage.keyDown(Input.Keys.CONTROL_LEFT)
                    stage.keyDown(Input.Keys.V)
                    stage.keyUp(Input.Keys.V)
                    stage.keyUp(Input.Keys.CONTROL_LEFT)
                    assertEquals("Room-code Ctrl+V must insert exactly once", "Ab12_-xy", code.text)
                    code.text = ""
                    clipboard.contents = " Zz98_-ab\n"
                    code.pasteCode()
                    assertEquals("Paste button path must trim the copied code", "Zz98_-ab", code.text)
                    code.isDisabled = true
                    clipboard.contents = "Aa00_-bb"
                    code.pasteCode()
                    assertEquals("Disabled code must remain unchanged", "Zz98_-ab", code.text)
                    completed = true
                } catch (ex: Throwable) {
                    failure.set(ex)
                } finally {
                    Gdx.input = originalInput
                    clipboard.contents = previous ?: ""
                    stage.dispose()
                    skin.dispose()
                    Gdx.app.exit()
                }
            }
        }, config)
        failure.get()?.let { throw AssertionError("Clipboard regression", it) }
        assertTrue("Clipboard checks completed", completed)
    }
}
