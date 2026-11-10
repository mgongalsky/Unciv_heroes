package com.unciv.app.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.ui.cityscreen.CitySandbox
import com.unciv.ui.utils.Fonts
import com.unciv.utils.Log
import org.koin.core.context.startKoin

/** Отдельный desktop-вход в город; рабочий каталог — android/assets, как у боевого запуска. */
internal object DesktopLauncherCity {
    @JvmStatic
    fun main(args: Array<String>) {
        Log.backend = DesktopLogBackend()
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")
        val isRunFromJar = DesktopLauncherCity.javaClass.`package`.specificationVersion != null
        ImagePacker.packImages(isRunFromJar)
        val config = Lwjgl3ApplicationConfiguration().apply {
            setWindowIcon("ExtraImages/Icon.png")
            setTitle("Heroic Civs - City Sandbox")
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
            initialScreenFactory = { CitySandbox.createScreen() }
        )
        Lwjgl3Application(UncivGame(parameters), config)
    }
}
