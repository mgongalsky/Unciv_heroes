package com.unciv.app.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.ui.playground.UiPlaygroundScreen
import com.unciv.ui.utils.Fonts
import com.unciv.utils.Log
import org.koin.core.context.startKoin

internal object DesktopLauncherUiPlayground {
    @JvmStatic
    fun main(arg: Array<String>) {
        Log.backend = DesktopLogBackend()
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")

        ImagePacker.packImages(false)

        val config = Lwjgl3ApplicationConfiguration().apply {
            setWindowIcon("ExtraImages/Icon.png")
            setTitle("Heroic Civs - UI Playground")
            setHdpiMode(HdpiMode.Logical)
            setWindowedMode(1280, 800)
            disableAudio(true)
        }

        startKoin {
            allowOverride(true)
            modules(gameModule)
        }

        val settings = UncivFiles.getSettingsForPlatformLaunchers()
        val parameters = UncivGameParameters(
            fontImplementation = NativeFontDesktop(
                (Fonts.ORIGINAL_FONT_SIZE * settings.fontSizeMultiplier).toInt(),
                settings.fontFamily
            ),
            initialScreenFactory = { UiPlaygroundScreen() }
        )

        Lwjgl3Application(UncivGame(parameters), config)
    }
}
