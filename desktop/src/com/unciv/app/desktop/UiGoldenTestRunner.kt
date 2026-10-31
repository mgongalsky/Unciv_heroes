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
import com.unciv.ui.playground.openBattleThreatPreview
import com.unciv.ui.playground.openBattleTurnQueuePreview
import com.unciv.ui.playground.openBattleTroopInfoPreview

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
        val captureOnly = args.contains("--capture-only")
        require(!(update && captureOnly)) { "Capture-only cannot update baselines" }
        val requestedScenarios = args.filter { it.startsWith("--scenario=") }
            .map { it.substringAfter('=') }.toSet()
        val armySetup = requestedScenarios == setOf("army-setup-laptop")
        require("army-setup-laptop" !in requestedScenarios || armySetup) {
            "Run army-setup-laptop separately at its laptop resolution"
        }
        val projectRoot = File(System.getProperty("golden.projectRoot") ?: "../..").canonicalFile
        val allScenarios = if (armySetup) listOf(Scenario("army-setup-laptop") {}) else scenarios()
        val selectedScenarios = if (requestedScenarios.isEmpty()) allScenarios
        else allScenarios.filter { it.name in requestedScenarios }
        val unknownScenarios = requestedScenarios - allScenarios.map { it.name }.toSet()
        require(unknownScenarios.isEmpty()) {
            "Unknown UI golden scenario(s): ${unknownScenarios.joinToString()}"
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
        try {
            val config = Lwjgl3ApplicationConfiguration().apply {
                setTitle("Heroic Civs UI golden tests")
                setHdpiMode(HdpiMode.Logical)
                setWindowedMode(if (armySetup) 1366 else WIDTH, if (armySetup) 768 else HEIGHT)
                setInitialVisible(false)
                setForegroundFPS(60)
                setIdleFPS(60)
                disableAudio(true)
            }
            Lwjgl3Application(
                GoldenApplication(projectRoot, update, captureOnly, selectedScenarios, result),
                config
            )
            check(result.passed) {
                "UI golden verification failed. See console and tests/golden/results for missing baselines or differences."
            }
        } finally {
            stopKoin()
        }
    }

    private fun scenarios() = listOf(
        Scenario("network-room") {},
        Scenario("network-battle") {},
        arenaScenario("arena-start", 0),
        arenaScenario("arena-retry", 2, defeat = true),
        arenaScenario("arena-complete", 3),
        Scenario("split-troop-empty") { com.unciv.ui.playground.openSplitTroopPreview(it) },
        Scenario("split-troop-occupied") {
            com.unciv.ui.playground.openSplitTroopPreview(it, targetAmount = 12)
        },
        Scenario("army-troop-info") { com.unciv.ui.playground.openArmyTroopInfoPreview(it) },
        Scenario("city-editor") { com.unciv.ui.playground.openScenarioCityEditorPreview(it) },
        Scenario("neutral-city-minimap") { com.unciv.ui.playground.openNeutralCityMinimapPreview(it) },
        Scenario("city-editor-tabs") {},
        Scenario("scenario-player-slots") {},
        Scenario("battle-troops") { screen -> openBattleTroopPreview(screen) },
        Scenario("battle-turn-queue") { screen -> openBattleTurnQueuePreview(screen) },
        Scenario("battle-troop-info") { screen -> openBattleTroopInfoPreview(screen) },
        Scenario("battle-threat-preview") { screen -> openBattleThreatPreview(screen) },
        Scenario("battle-tile-highlights") { screen ->
            com.unciv.ui.playground.openBattleTileHighlightPreview(screen)
        },
        Scenario("battle-flanking") { screen ->
            com.unciv.ui.playground.openBattleFlankingPreview(screen)
        },
        Scenario("battle-result-attacker-victory") { screen ->
            BattleResultPopup(
                screen, BattleReport(
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
        private val captureOnly: Boolean,
        private val scenarios: List<Scenario>,
        private val result: GoldenResult
    ) : ApplicationAdapter() {
        private var expectedScreen: BaseScreen? = null
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

        /** Ждёт готовности нужного экрана, снимает кадр и выполняет выбранный режим: захват, сравнение или обновление. */
        override fun render() {
            Gdx.graphics.isContinuousRendering = true
            if (System.currentTimeMillis() - startedAt > 30_000L) {
                System.err.println("TIMEOUT: UI golden scenarios did not finish within 30 seconds")
                result.passed = false
                Gdx.app.exit()
                return
            }
            game.render()
            if (expectedScreen == null || game.screen !== expectedScreen) {
                stableFrames = 0
                return
            }
            stableFrames++
            if (stableFrames < STABLE_FRAMES_BEFORE_CAPTURE) return
            val framebuffer = ScreenUtils.getFrameBufferPixmap(
                0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight
            )
            val actual = flipVertically(framebuffer)
            framebuffer.dispose()
            if (captureOnly) {
                // Сценарий уже выполнил встроенные проверки, но этот режим не сравнивает и не утверждает изображение.
                // Пишется только actual в results; эталоны baseline остаются прежними.
                val platform = System.getProperty("os.name").lowercase()
                    .replace(Regex("[^a-z0-9]+"), "-").trim('-')
                val name = scenarios[scenarioIndex].name
                writePng(
                    File(projectRoot, "tests/golden/results/$platform/$name-actual.png"),
                    actual
                )
                println("RENDERED $name: form/layout checks passed; visual comparison not performed")
            } else {
                allPassed = compareOrUpdate(
                    scenario = scenarios[scenarioIndex].name,
                    actual = actual,
                    projectRoot = projectRoot,
                    update = update
                ) && allPassed
            }
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

        /** Сценарии навигации получают настоящий экран; отдельные формы открываются в общем контейнере предпросмотра. */
        private fun scenarioScreen(scenario: Scenario): BaseScreen = when (scenario.name) {
            "network-room" -> NetworkBattleGoldenScreens.room()
            "network-battle" -> NetworkBattleGoldenScreens.battle()
            "army-setup-laptop" -> ArmySetupGoldenChecks.screen()
            "city-editor-tabs" -> com.unciv.ui.playground.createScenarioCityEditorTabsPreview()
            "scenario-player-slots" -> com.unciv.ui.playground.createScenarioPlayerPickerPreview()
            else -> GoldenScenarioScreen(scenario.open)
        }.also { expectedScreen = it }

        override fun dispose() {
            if (!::game.isInitialized) return
            // UncivGame.dispose() exits the JVM with code 0, hiding golden failures from JUnit.
            // Release the test application's resources without saving user settings or exiting.
            Gdx.input.inputProcessor = null
            game.screenStack.toList().forEach { it.dispose() }
            game.screenStack.clear()
            com.unciv.ui.audio.SoundPlayer.clearCache()
            if (game.isInitialized) game.musicController.gracefulShutdown()
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
    private fun arenaScenario(name: String, victories: Int, defeat: Boolean = false) =
            Scenario(name) { screen ->
                val run = com.unciv.logic.arena.ArenaBattleSetup.newRun(42L)
                repeat(victories) {
                    run.finishBattle(
                        run.beginBattle()!!,
                        com.unciv.pure.domain.arena.ArenaRun.Outcome.VICTORY
                    )
                }
                if (defeat) run.finishBattle(
                    run.beginBattle()!!,
                    com.unciv.pure.domain.arena.ArenaRun.Outcome.DEFEAT
                )
                screen.stage.addActor(
                    com.unciv.ui.utils.AutoScrollPane(
                        com.unciv.ui.arena.createArenaContent(run, {}, {}, {})
                    ).apply { setFillParent(true) })
            }
}
