package com.unciv.app.desktop

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.glutils.HdpiMode
import com.badlogic.gdx.utils.ScreenUtils
import com.unciv.UncivGame
import com.unciv.UncivGameParameters
import com.unciv.di.gameModule
import com.unciv.logic.UncivFiles
import com.unciv.ui.cityscreen.CitySandbox
import com.unciv.ui.cityscreen.CityScreen
import com.unciv.ui.utils.Fonts
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.File

/** Снимок настоящего городского экрана в скрытом GL-окне, без тестов и записи эталонов. */
object CityPreviewCapture {
    @JvmStatic
    fun main(args: Array<String>) {
        fun option(name: String) = args.firstOrNull { it.startsWith("--$name=") }?.substringAfter('=')
        val output = File(requireNotNull(option("output")) { "Specify --output=<PNG path>" }).absoluteFile
        val width = option("width")?.toInt() ?: 1568
        val height = option("height")?.toInt() ?: 690
        require(width in 320..4096 && height in 320..2160)
        System.setProperty("org.lwjgl.opengl.Display.allowSoftwareOpenGL", "true")
        System.setProperty("org.lwjgl.system.stackSize", "384")
        LogSetup.install()
        ImagePacker.packImages(false)
        startKoin { allowOverride(true); modules(gameModule) }
        val capture = CaptureApplication(output)
        try {
            Lwjgl3Application(capture, Lwjgl3ApplicationConfiguration().apply {
                setTitle("City preview capture")
                setWindowedMode(width, height)
                setHdpiMode(HdpiMode.Logical)
                setInitialVisible(false)
                setForegroundFPS(60)
                setIdleFPS(60)
                disableAudio(true)
            })
            check(capture.saved) { "City preview was not captured" }
        } finally {
            stopKoin()
        }
    }

    private object LogSetup {
        fun install() { com.unciv.utils.Log.backend = DesktopLogBackend() }
    }

    /** Ожидает асинхронный каталог; завершает приложение без сохранения пользовательских настроек. */
    private class CaptureApplication(private val output: File) : ApplicationAdapter() {
        private var popupPreviewOpened = false
        private var tilePreviewOpened = false
        private lateinit var game: UncivGame
        private var stableFrames = 0
        private val startedAt = System.nanoTime()
        var saved = false
            private set

        override fun create() {
            val settings = UncivFiles.getSettingsForPlatformLaunchers()
            game = UncivGame(
                UncivGameParameters(
                    fontImplementation = NativeFontDesktop(
                        (Fonts.ORIGINAL_FONT_SIZE * settings.fontSizeMultiplier).toInt(),
                        settings.fontFamily
                    ),
                    initialScreenFactory = {
                        // Настройки и отдельная модель захвата не сохраняются в пользовательскую партию.
                        UncivGame.Current.settings.citySoundsVolume = 0f
                        val armyPreview = System.getenv("CITY_PREVIEW_ARMY") == "1"
                        CitySandbox.createScreen(
                            previewBuildings = !armyPreview && System.getenv("CITY_PREVIEW_BUILDINGS") == "1",
                            previewArmy = armyPreview
                        )
                    }
                )
            )
            game.create()
        }

        override fun render() {
            Gdx.graphics.isContinuousRendering = true
            game.render()
            if ((System.nanoTime() - startedAt) / 1_000_000_000L > 45L) {
                System.err.println("City preview timed out waiting for the requested page")
                Gdx.app.exit()
                return
            }
            val screen = game.screen as? CityScreen ?: return
            if (capturePopupPreview(screen)) return
            val armyPreview = System.getenv("CITY_PREVIEW_ARMY") == "1"
            val buildingsPreview = !armyPreview && System.getenv("CITY_PREVIEW_BUILDINGS") == "1"
            val statsPreview =
                    !armyPreview && !buildingsPreview && System.getenv("CITY_PREVIEW_STATS") == "1"
            val tilePreview =
                    !armyPreview && !buildingsPreview && !statsPreview && System.getenv("CITY_PREVIEW_TILE") == "1"
            val pager =
                    screen.stage.root.findActor<com.unciv.ui.utils.TabbedPager>("cityCompactPager")
            val requestedPage = when {
                armyPreview -> "Army"
                buildingsPreview -> "Buildings"
                statsPreview -> "Stats"
                else -> null
            }
            if (requestedPage != null && stableFrames == 0) {
                if (pager == null || pager.getPageIndex(requestedPage) < 0) return
                pager.selectPage(requestedPage)
            }
            if (tilePreview && !tilePreviewOpened) {
                if (pager != null && pager.getPageIndex("Map") >= 0) pager.selectPage("Map")
                fun findTile(actor: com.badlogic.gdx.scenes.scene2d.Actor): com.unciv.ui.cityscreen.CityTileGroup? {
                    if (actor is com.unciv.ui.cityscreen.CityTileGroup && !actor.tileInfo.isCityCenter()) return actor
                    if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                        for (child in actor.children) findTile(child)?.let { return it }
                    return null
                }

                val tile = findTile(screen.stage.root) ?: return
                tilePreviewOpened = true
                game.replaceCurrentScreen(screen.createCityScreen(initSelectedTile = tile.tileInfo))
                stableFrames = 0
                return
            }
            val target = when {
                armyPreview -> "cityHeroSupplyTable"
                buildingsPreview -> "cityBuildingsTable"
                statsPreview -> "cityIncomeTable"
                tilePreview -> "cityTileSummary"
                else -> "cityConstruction:Worker"
            }
            val ready =
                    screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.Actor>(target) != null
            if (!ready) {
                stableFrames = 0
                return
            }
            if (++stableFrames < 20 || saved) return
            val pixmap = ScreenUtils.getFrameBufferPixmap(
                0, 0,
                Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight
            )
            try {
                output.parentFile.mkdirs()
                writeLayoutReport(screen)
                PixmapIO.writePNG(Gdx.files.absolute(output.path), pixmap, -1, true)
                saved = true
                println("CAPTURED ${output.path} (${pixmap.width}x${pixmap.height}), page=$target")
            } finally {
                pixmap.dispose()
            }
            Gdx.app.exit()
        }

        override fun dispose() {
            if (!::game.isInitialized) return
            Gdx.input.inputProcessor = null
            game.screenStack.toList().forEach { it.dispose() }
            game.screenStack.clear()
            com.unciv.ui.utils.ApplicationSkinResources.dispose()
            com.unciv.ui.audio.SoundPlayer.clearCache()
            if (game.isInitialized) game.musicController.gracefulShutdown()
        }

        /** Диагностика геометрии снятого кадра; не заменяет просмотр изображения и не запускает тесты. */
        private fun writeLayoutReport(screen: CityScreen) {
            val report = StringBuilder()
            report.append("stage=${screen.stage.width}x${screen.stage.height}\n")
            report.append("kind\tname/text\tx\ty\twidth\theight\tprefWidth\tprefHeight\n")
            fun visit(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                if (!actor.isVisible) return
                val label = actor as? com.badlogic.gdx.scenes.scene2d.ui.Label
                if (label != null || actor.name?.startsWith("city") == true) {
                    val position = actor.localToStageCoordinates(com.badlogic.gdx.math.Vector2())
                    val caption = (label?.text?.toString() ?: actor.name ?: "")
                        .replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
                    report.append(actor.javaClass.simpleName).append('\t').append(caption)
                        .append('\t').append(position.x).append('\t').append(position.y)
                        .append('\t').append(actor.width).append('\t').append(actor.height)
                        .append('\t').append(label?.prefWidth ?: 0f)
                        .append('\t').append(label?.prefHeight ?: 0f).append('\n')
                }
                if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                    for (child in actor.children) visit(child)
            }
            visit(screen.stage.root)
            File(
                output.parentFile,
                output.nameWithoutExtension + ".layout.tsv"
            ).writeText(report.toString(), Charsets.UTF_8)
        }

        /** Открывает производственные окна в автономной сцене без выполнения покупок и изменения армии. */
        private fun capturePopupPreview(screen: CityScreen): Boolean {
            val mode =
                    System.getenv("CITY_PREVIEW_POPUP")?.takeIf { it.isNotBlank() } ?: return false
            if (!popupPreviewOpened) {
                val pager =
                        screen.stage.root.findActor<com.unciv.ui.utils.TabbedPager>("cityCompactPager")
                            ?: return true
                pager.selectPage("Army")
                pager.validate()
                val popup: com.unciv.ui.popup.Popup = when (mode) {
                    "confirm" -> com.unciv.ui.popup.ConfirmPopup(
                        screen,
                        "Currently you have 0 ${Fonts.gold} Gold.\n\nWould you like to purchase Tile for 0 ${Fonts.gold}?",
                        "Purchase", true
                    ) {}

                    "rename" -> com.unciv.ui.popup.AskTextPopup(
                        screen,
                        label = "Please enter a new name for your city",
                        defaultText = "City Sandbox"
                    )

                    "stats" -> com.unciv.ui.cityscreen.DetailedStatsPopup(screen)
                    "fantasy" -> com.unciv.ui.cityscreen.TestFantasyPopup(screen)
                    "failure" -> com.unciv.ui.cityscreen.ConstructionPurchaseFailurePopup(
                        screen.stage,
                        "Worker",
                        "City Sandbox"
                    )

                    "split" -> {
                        val army = checkNotNull(
                            screen.stage.root.findActor<com.unciv.ui.army.ArmyView>("cityHeroSlots")
                        )
                        val slots =
                                army.children.filterIsInstance<com.unciv.ui.army.TroopArmyView>()
                        com.unciv.ui.popup.SplitTroopPopup(
                            screen,
                            slots.first { !it.isEmptySlot() },
                            slots.first { it.isEmptySlot() }) { _, _ -> }
                    }

                    "unit" -> {
                        val troop = com.unciv.pure.domain.troop.TroopFactory.create(
                            "Spearman", 10,
                            com.unciv.pure.domain.troop.RulesetTroopDefinitionSource(com.unciv.ui.images.ImageGetter.ruleset)
                        )
                        com.unciv.ui.army.UnitInfoPopup(screen, troop)
                    }

                    "toast" -> com.unciv.ui.popup.ToastPopup(
                        "Please select a tile for this building's [Farm]",
                        screen,
                        60000L
                    )

                    else -> error("Unknown CITY_PREVIEW_POPUP: $mode")
                }
                popup.name = "cityPopupPreview"
                popup.innerTable.name = "cityPopupPreviewContent"
                if (popup.parent == null) popup.open()
                popupPreviewOpened = true
                stableFrames = 0
                return true
            }
            if (++stableFrames < 20 || saved) return true
            val pixmap = ScreenUtils.getFrameBufferPixmap(
                0, 0,
                Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight
            )
            try {
                output.parentFile.mkdirs()
                writeLayoutReport(screen)
                PixmapIO.writePNG(Gdx.files.absolute(output.path), pixmap, -1, true)
                saved = true
                println("CAPTURED ${output.path} (${pixmap.width}x${pixmap.height}), popup=$mode")
            } finally {
                pixmap.dispose()
            }
            Gdx.app.exit()
            return true
        }
    }
}
