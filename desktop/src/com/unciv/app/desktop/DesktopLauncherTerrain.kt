package com.unciv.app.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.app.desktop.terrain.TerrainExperimentExport
import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.app.desktop.terrain.TerrainExperimentSource
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.ui.mapeditor.TerrainPlaygroundScreen
import com.unciv.ui.utils.Fonts
import com.unciv.utils.Log
import org.koin.core.context.startKoin
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** Отдельная графическая оболочка; вычисления, файлы и картинки совпадают с CLI. */
object DesktopLauncherTerrain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size <= 1 && args.all { it.startsWith("--input=") || it.startsWith("--terrains=") }) {
            "Use no arguments, --input=<snapshot-or-report.json>, or --terrains=<Terrains.json>"
        }
        val argument = args.singleOrNull()
        val initial = when {
            argument?.startsWith("--input=") == true -> TerrainExperimentFiles.readInput(File(argument.substringAfter('=')))
            else -> TerrainExperimentSource.create(TerrainExperimentSource.loadRules(File(
                argument?.substringAfter('=') ?: "jsons/Civ V - Gods & Kings/Terrains.json")))
        }
        Log.backend = DesktopLogBackend()
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")
        ImagePacker.packImages(false)
        val config = Lwjgl3ApplicationConfiguration().apply {
            setWindowIcon("ExtraImages/Icon.png")
            setTitle("Heroic Civs - Terrain experiments")
            setHdpiMode(HdpiMode.Logical)
            setWindowedMode(1280, 800)
            disableAudio(true)
        }
        startKoin { allowOverride(true); modules(gameModule) }
        val settings = UncivFiles.getSettingsForPlatformLaunchers()
        val parameters = UncivGameParameters(
            fontImplementation = NativeFontDesktop(
                (Fonts.ORIGINAL_FONT_SIZE * settings.fontSizeMultiplier).toInt(), settings.fontFamily),
            initialScreenFactory = {
                TerrainPlaygroundScreen(initial,
                    readInput = { TerrainExperimentFiles.readInput(File(it)) },
                    exportResult = { path, result -> TerrainExperimentExport.write(File(path), result) },
                    renderPng = { result, layer ->
                        ByteArrayOutputStream().use { output ->
                            check(ImageIO.write(TerrainExperimentExport.render(result,
                                TerrainExperimentExport.Layer.valueOf(layer)), "png", output)) { "PNG writer unavailable" }
                            output.toByteArray()
                        }
                    })
            })
        Lwjgl3Application(UncivGame(parameters), config)
    }
}
