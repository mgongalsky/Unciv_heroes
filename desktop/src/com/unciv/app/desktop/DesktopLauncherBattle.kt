package com.unciv.app.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.di.gameModule
import com.unciv.logic.army.ArmyInfo
import com.unciv.ui.battlescreen.BattleScreen
import com.unciv.utils.Log
import org.koin.core.context.startKoin
import com.unciv.logic.UncivFiles
import com.unciv.ui.utils.Fonts

internal object DesktopLauncherBattle {
    @JvmStatic
    fun main(arg: Array<String>) {
        Log.backend = DesktopLogBackend()
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")

        ImagePacker.packImages(false)

        val config = Lwjgl3ApplicationConfiguration()
        config.setWindowIcon("ExtraImages/Icon.png")
        config.setTitle("Heroic Civs - Battle Test")
        config.setHdpiMode(HdpiMode.Logical)
        config.setWindowedMode(1280, 800)
        config.disableAudio(true)

        startKoin {
            allowOverride(true)
            modules(gameModule)
        }

        val settings = UncivFiles.getSettingsForPlatformLaunchers()

        val desktopParameters = UncivGameParameters(
            fontImplementation = NativeFontDesktop(
                (Fonts.ORIGINAL_FONT_SIZE * settings.fontSizeMultiplier).toInt(),
                settings.fontFamily
            ),
            initialScreenFactory = {
                BattleScreen.forTesting(
                    attackerArmy = buildTestArmy(isAttacker = true),
                    defenderArmy = buildTestArmy(isAttacker = false)
                )
            }
        )
        Lwjgl3Application(UncivGame(desktopParameters), config)
    }

    private fun buildTestArmy(isAttacker: Boolean): ArmyInfo {
        val army = ArmyInfo()
        if (isAttacker) {
            army.fillArmy("Spearman", 50)
        } else {
            army.fillArmy("Archer", 40)
        }
        return army
    }
}
