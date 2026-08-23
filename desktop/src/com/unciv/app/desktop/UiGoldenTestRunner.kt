package com.unciv.app.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.utils.ScreenUtils
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.battlescreen.BattleResultPopup
import com.unciv.ui.playground.openBattleTroopPreview
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File
import kotlin.system.exitProcess
import com.unciv.ui.playground.openBattleThreatPreview

object UiGoldenTestRunner {
    private const val WIDTH = 1280
    private const val HEIGHT = 800
    private const val STABLE_FRAMES_BEFORE_CAPTURE = 8

    private data class Scenario(
        val name: String,
        val open: (BaseScreen) -> Unit
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val update = args.contains("--update")
        val requestedScenarios = args
            .filter { it.startsWith("--scenario=") }
            .map { it.substringAfter('=') }
            .toSet()
        val projectRoot = File(System.getProperty("golden.projectRoot") ?: "../..").canonicalFile
        val allScenarios = scenarios()
        val selectedScenarios = if (requestedScenarios.isEmpty()) allScenarios
        else allScenarios.filter { it.name in requestedScenarios }
        val unknownScenarios = requestedScenarios - allScenarios.map { it.name }.toSet()
        if (unknownScenarios.isNotEmpty()) {
            System.err.println("Unknown UI golden scenario(s): ${unknownScenarios.joinToString()}")
            exitProcess(2)
        }

        val result = GoldenResult()
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")
        ImagePacker.packImages(false)
        stopKoin()
        startKoin {
            allowOverride(true)
            modules(gameModule)
        }

        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Heroic Civs UI golden tests")
            setHdpiMode(HdpiMode.Logical)
            setWindowedMode(WIDTH, HEIGHT)
            setInitialVisible(false)
            setForegroundFPS(60)
            setIdleFPS(60)
            disableAudio(true)
        }
        Lwjgl3Application(GoldenApplication(projectRoot, update, selectedScenarios, result), config)
        stopKoin()
        if (!result.passed) exitProcess(1)
    }

    private fun scenarios() = listOf(
        Scenario("battle-troops") { screen ->
            openBattleTroopPreview(screen)
        },
        Scenario("battle-threat-preview") { screen ->
            openBattleThreatPreview(screen)
        },
        Scenario("battle-result-attacker-victory") { screen ->
            BattleResultPopup(
                screen,
                BattleReport(
                    winner = BattleSide.ATTACKER,
                    attackerLosses = listOf(
                        BattleLoss(1, "Peasant", 13),
                        BattleLoss(2, "Spearman", 8),
                        BattleLoss(3, "Archer", 6)
                    ),
                    defenderLosses = listOf(
                        BattleLoss(4, "Swordsman", 13),
                        BattleLoss(5, "Horseman", 12),
                        BattleLoss(6, "Crossbowman", 7)
                    )
                )
            ) {}.open(force = true)
        }
    )

    private class GoldenApplication(
        private val projectRoot: File,
        private val update: Boolean,
        private val scenarios: List<Scenario>,
        private val result: GoldenResult
    ) : ApplicationAdapter() {
        private lateinit var game: UncivGame
        private var scenarioIndex = 0
        private var stableFrames = 0
        private var allPassed = true
        private val startedAt = System.currentTimeMillis()

        override fun create() {
            Gdx.graphics.isContinuousRendering = true
            val settings = UncivFiles.getSettingsForPlatformLaunchers()
            game = UncivGame(
                UncivGameParameters(
                    fontImplementation = NativeFontDesktop(
                        Fonts.ORIGINAL_FONT_SIZE.toInt(),
                        settings.fontFamily
                    ),
                    initialScreenFactory = { scenarioScreen(scenarios.first()) }
                )
            )
            game.create()
        }

        override fun render() {
            Gdx.graphics.isContinuousRendering = true
            if (System.currentTimeMillis() - startedAt > 30_000L) {
                System.err.println("\u001B[31mTIMEOUT: UI golden scenarios did not finish within 30 seconds\u001B[0m")
                result.passed = false
                Gdx.app.exit()
                return
            }

            game.render()
            if (game.screen !is GoldenScenarioScreen) {
                stableFrames = 0
                return
            }
            stableFrames++
            if (stableFrames < STABLE_FRAMES_BEFORE_CAPTURE) return

            val framebuffer = ScreenUtils.getFrameBufferPixmap(0, 0, WIDTH, HEIGHT)
            val actual = flipVertically(framebuffer)
            framebuffer.dispose()
            allPassed = compareOrUpdate(
                scenario = scenarios[scenarioIndex].name,
                actual = actual,
                projectRoot = projectRoot,
                update = update
            ) && allPassed
            actual.dispose()

            scenarioIndex++
            stableFrames = 0
            if (scenarioIndex < scenarios.size) {
                game.replaceCurrentScreen(scenarioScreen(scenarios[scenarioIndex]))
            } else {
                result.passed = allPassed
                Gdx.app.exit()
            }
        }

        private fun scenarioScreen(scenario: Scenario) = GoldenScenarioScreen(scenario.open)

        override fun dispose() {
            if (::game.isInitialized) game.dispose()
        }
    }

    private class GoldenScenarioScreen(openScenario: (BaseScreen) -> Unit) : BaseScreen() {
        init {
            openScenario(this)
        }
    }

    private class GoldenResult(var passed: Boolean = false)

    private fun flipVertically(source: Pixmap): Pixmap {
        val flipped = Pixmap(source.width, source.height, source.format)
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                flipped.drawPixel(x, source.height - 1 - y, source.getPixel(x, y))
            }
        }
        return flipped
    }

    private fun compareOrUpdate(
        scenario: String,
        actual: Pixmap,
        projectRoot: File,
        update: Boolean
    ): Boolean {
        val platform = System.getProperty("os.name")
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        val baselineFile = File(projectRoot, "tests/golden/baseline/$platform/$scenario.png")
        val actualFile = File(projectRoot, "tests/golden/results/$platform/$scenario-actual.png")
        val diffFile = File(projectRoot, "tests/golden/results/$platform/$scenario-diff.png")

        if (update) {
            writePng(baselineFile, actual)
            actualFile.delete()
            diffFile.delete()
            println("\u001B[32mUPDATED $scenario -> ${baselineFile.relativeTo(projectRoot)}\u001B[0m")
            return true
        }
        if (!baselineFile.exists()) {
            writePng(actualFile, actual)
            println("\u001B[31mMISSING $scenario: run desktop:goldenUpdate to create the baseline\u001B[0m")
            return false
        }

        val expected = Pixmap(Gdx.files.absolute(baselineFile.absolutePath))
        try {
            if (expected.width != actual.width || expected.height != actual.height) {
                writePng(actualFile, actual)
                println("\u001B[31mFAIL $scenario: expected ${expected.width}x${expected.height}, actual ${actual.width}x${actual.height}\u001B[0m")
                return false
            }

            val diff = Pixmap(actual.width, actual.height, Pixmap.Format.RGBA8888)
            var changedPixels = 0L
            for (y in 0 until actual.height) {
                for (x in 0 until actual.width) {
                    val expectedPixel = expected.getPixel(x, y)
                    val actualPixel = actual.getPixel(x, y)
                    if (expectedPixel != actualPixel) {
                        changedPixels++
                        diff.drawPixel(x, y, 0xff0000ff.toInt())
                    } else {
                        diff.drawPixel(x, y, 0x000000ff)
                    }
                }
            }
            if (changedPixels == 0L) {
                actualFile.delete()
                diffFile.delete()
                println("\u001B[32mPASS $scenario (${actual.width}x${actual.height})\u001B[0m")
                diff.dispose()
                return true
            }

            writePng(actualFile, actual)
            writePng(diffFile, diff)
            diff.dispose()
            println("\u001B[31mFAIL $scenario: $changedPixels pixels changed\u001B[0m")
            println("  actual: ${actualFile.relativeTo(projectRoot)}")
            println("  diff:   ${diffFile.relativeTo(projectRoot)}")
            return false
        } finally {
            expected.dispose()
        }
    }

    private fun writePng(file: File, pixmap: Pixmap) {
        file.parentFile.mkdirs()
        PixmapIO.writePNG(Gdx.files.absolute(file.absolutePath), pixmap)
    }
}
