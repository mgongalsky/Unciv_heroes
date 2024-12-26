package com.example.devtools

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("JSON Editor")
            setWindowedMode(1600, 1200)
            setResizable(true)
        }
        Lwjgl3Application(JsonEditorApp(), config)
    }
}
