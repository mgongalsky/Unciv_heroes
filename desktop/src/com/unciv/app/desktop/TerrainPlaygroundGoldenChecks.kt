package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.app.desktop.terrain.TerrainExperimentExport
import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.ClimateTerrainRange
import com.unciv.pure.domain.mapgeneration.VegetationRule
import com.unciv.ui.mapeditor.TerrainPlaygroundScreen
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

/** Выполняет реальные обработчики экрана и ждёт публикации каждого фонового результата. */
object TerrainPlaygroundGoldenChecks {
    private var phase = 0
    private lateinit var initialResult: TerrainExperimentResult
    private lateinit var expected: TerrainExperimentResult
    private lateinit var imported: TerrainExperimentResult
    private lateinit var report: File
    private var originalSetup: Any? = null
    private var originalParameters: String? = null

    fun screen(): TerrainPlaygroundScreen {
        phase = 0
        val rules = TerrainRuleSnapshot(setOf("Grassland", "Forest"), emptySet(), emptySet(), "Grassland",
            "Grassland", null, null, emptySet(),
            listOf(ClimateTerrainRange.inclusiveMinimums("Grassland", -1f, 1f, 0f, 1f)),
            listOf(VegetationRule("Forest", listOf("Grassland"))))
        val input = TerrainExperimentInput(TerrainGridFactory.hexagonal(5, "Grassland"), rules, TerrainExperimentSettings(seed = 0L))
        initialResult = TerrainGenerationPipeline.generate(input)
        expected = initialResult
        imported = TerrainGenerationPipeline.generate(input.copy(settings = input.settings.copy(seed = -17L, vegetationScale = 8.0)))
        report = File.createTempFile("terrain-playground-", ".json").apply { deleteOnExit() }
        TerrainExperimentFiles.writeResult(report, imported)
        val screen = TerrainPlaygroundScreen(input,
            readInput = { path ->
                require(path != "invalid") { "Invalid fixture path" }
                TerrainExperimentFiles.readInput(File(path)).also { report.delete() }
            },
            exportResult = { _, _ -> error("No export action expected in this scenario") },
            renderPng = { result, layer ->
                ByteArrayOutputStream().use { output ->
                    check(ImageIO.write(TerrainExperimentExport.render(result,
                        TerrainExperimentExport.Layer.valueOf(layer)), "png", output))
                    output.toByteArray()
                }
            })
        originalSetup = screen.game.settings.lastGameSetup
        originalParameters = screen.game.settings.lastGameSetup?.mapParameters?.toString()
        // Конструктор уже захватил вход для рабочего потока; меняется только черновик формы.
        field(screen, "terrain-seed").text = "42"
        return screen
    }

    fun ready(screen: TerrainPlaygroundScreen): Boolean {
        val status = screen.stage.root.findActor<Label>("terrain-status").text.toString()
        if (phase != 3) check(!status.startsWith("Error:")) { status }
        if (!screen.isResultReady) return false
        when (phase) {
            0 -> {
                check(screen.displayedResultSnapshot() == initialResult) { "Worker read mutable form state" }
                check(screen.snapshotForRun().settings.seed == 42L)
                val fields = listOf(
                    "terrain-climate-scale",
                    "terrain-vegetation-scale",
                    "terrain-elevation-scale"
                )
                val baseline = screen.snapshotForRun()
                for ((index, name) in fields.withIndex()) {
                    val field = field(screen, name)
                    val original = field.text
                    field.text = "9.5"
                    val settings = when (index) {
                        0 -> baseline.settings.copy(climateScale = 9.5)
                        1 -> baseline.settings.copy(vegetationScale = 9.5)
                        else -> baseline.settings.copy(elevationScale = 9.5)
                    }
                    check(screen.snapshotForRun() == baseline.copy(settings = settings)) { "Scale controls are coupled" }
                    field.text = original
                }
                field(screen, "terrain-seed").text = "0"
                check(screen.snapshotForRun() == initialResult.input)
                phase = 1
                click(screen, "Generate / repeat")
            }

            1 -> {
                check(screen.displayedResultSnapshot() == initialResult) { "Repeat changed the result" }
                field(screen, "terrain-vegetation-scale").text = "9.5"
                expected = TerrainGenerationPipeline.generate(screen.snapshotForRun())
                phase = 2
                click(screen, "Generate / repeat")
            }

            2 -> {
                check(screen.displayedResultSnapshot() == expected) { "Scale change did not use the shared pipeline" }
                pathField(screen).text = "invalid"
                phase = 3
                click(screen, "Open snapshot / report")
            }

            3 -> {
                check(status.startsWith("Error:")) { "Expected an opening error" }
                check(screen.displayedResultSnapshot() == expected) { "Failed open replaced the displayed result" }
                pathField(screen).text = report.absolutePath
                phase = 4
                click(screen, "Open snapshot / report")
            }

            4 -> {
                check(screen.displayedResultSnapshot() == imported) { "Opened report differs from the CLI result" }
                check(screen.snapshotForRun() == imported.input) { "Opened report did not restore source and settings" }
                phase = 5
                click(screen, "Generate / repeat")
            }

            5 -> {
                check(screen.displayedResultSnapshot() == imported) { "Imported source accumulated generated layers" }
                pathField(screen).text = ""
                phase = 6
                click(screen, "Show game terrain")
            }

            6 -> {
                verifyGamePreview(screen)
                phase = 7
                click(screen, "Show selected layer")
            }

            7 -> {
                check(screen.displayedResultSnapshot() == imported) { "PNG view changed the result" }
                check(screen.stage.root.findActor<Actor>("terrain-game-preview") == null) { "PNG view did not replace game view" }
                check(status.startsWith("Displayed: BASE")) { status }
                phase = 8
                click(screen, "Show game terrain")
            }

            8 -> {
                verifyGamePreview(screen)
                check(screen.game.settings.lastGameSetup === originalSetup) { "Playground replaced game setup" }
                check(screen.game.settings.lastGameSetup?.mapParameters?.toString() == originalParameters) { "Playground changed game parameters" }
                check(screen.stage.root.findActor<Label>("terrain-metrics").text.isNotEmpty())
                check(screen.stage.width > 370f && screen.stage.height > 0f)
                phase = 9
                return true
            }

            else -> return true
        }
        return false
    }

    private fun field(screen: TerrainPlaygroundScreen, name: String): TextField = screen.stage.root.findActor(name)

    private fun actors(actor: Actor): List<Actor> = listOf(actor) +
        if (actor is Group) actor.children.flatMap { actors(it) } else emptyList()

    private fun pathField(screen: TerrainPlaygroundScreen) = actors(screen.stage.root).filterIsInstance<TextField>()
        .single { it.messageText == "Snapshot file or new export directory" }

    /** Вызывает установленные ClickListener настоящей кнопки, сохраняя её защиту от повторного запуска. */
    private fun click(screen: TerrainPlaygroundScreen, text: String) {
        val button = actors(screen.stage.root).filterIsInstance<TextButton>().single { it.text.toString() == text }
        check(!button.isDisabled) { "Action is unexpectedly disabled: $text" }
        val listeners = button.listeners.filterIsInstance<ClickListener>()
        check(listeners.isNotEmpty()) { "Missing click listener: $text" }
        val event = InputEvent().apply { target = button }
        listeners.forEach { it.clicked(event, 0f, 0f) }
    }

    /** Сверяет игровые клетки с результатом CLI, включая признаки рельефа и климат. */
    private fun verifyGamePreview(screen: TerrainPlaygroundScreen) {
        val holder =
                requireNotNull(screen.stage.root.findActor<com.unciv.ui.mapeditor.EditorMapHolder>("terrain-game-preview"))
        check(holder.editorScreen == null) { "Game preview enabled map editing" }
        check(holder.touchable == com.badlogic.gdx.scenes.scene2d.Touchable.enabled)
        check(screen.stage.scrollFocus === holder)
        check(holder.width > 0f && holder.height > 0f)
        check(screen.displayedResultSnapshot() == imported) { "Game view changed experiment result" }
        check(holder.tileGroups.size == imported.cells.size)
        val tiles =
                holder.tileGroups.keys.associateBy { it.position.x.toInt() to it.position.y.toInt() }
        for (cell in imported.cells) {
            val tile = tiles.getValue(cell.column to cell.row)
            check(tile.baseTerrain == cell.baseTerrain && tile.terrainFeatures == cell.features)
            check(tile.temperature == cell.temperature && tile.humidity == cell.humidity)
            check(
                holder.tileGroups.getValue(tile)
                    .all { it.showEntireMap && it.tileBaseImages.isNotEmpty() })
        }
    }
}
