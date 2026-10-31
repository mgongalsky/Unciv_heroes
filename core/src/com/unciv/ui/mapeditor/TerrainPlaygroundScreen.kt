package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Scaling
import com.unciv.pure.application.mapgeneration.*
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import java.util.concurrent.Executors
import kotlin.random.Random

/** Экран эксперимента без GameInfo. Файлы и PNG обслуживает JVM-адаптер, переданный launcher. */
class TerrainPlaygroundScreen(
    initial: TerrainExperimentInput,
    private val readInput: (String) -> TerrainExperimentInput,
    private val exportResult: (String, TerrainExperimentResult) -> Unit,
    private val renderPng: (TerrainExperimentResult, String) -> ByteArray
) : BaseScreen() {
    private var source = initial.snapshot()
    private var result: TerrainExperimentResult? = null
    private var texture: Texture? = null
    private var disposed = false
    private var busy = false
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "terrain-playground").apply { isDaemon = true }
    }
    private val seed = TextField("", skin).apply { name = "terrain-seed" }
    private val climate = TextField("", skin).apply { name = "terrain-climate-scale" }
    private val vegetation = TextField("", skin).apply { name = "terrain-vegetation-scale" }
    private val elevation = TextField("", skin).apply { name = "terrain-elevation-scale" }
    private val mode = SelectBox<String>(skin).apply { setItems(*TerrainGenerationMode.values().map { it.name }.toTypedArray()) }
    private val layer = SelectBox<String>(skin).apply {
        name = "terrain-layer"
        setItems("BASE", "TEMPERATURE", "HUMIDITY", "ELEVATION", "VEGETATION")
    }
    private val shape = SelectBox<String>(skin).apply { setItems("hexagonal", "rectangular") }
    private val radius = TextField("12", skin)
    private val gridWidth = TextField("30", skin)
    private val gridHeight = TextField("20", skin)
    private val path = TextField("", skin).apply { messageText = "Snapshot file or new export directory" }
    private val status = Label("Ready", skin).apply { setWrap(true); name = "terrain-status" }
    private val sourceInfo = Label("", skin).apply { setWrap(true) }
    private val metrics = Label("", skin).apply { setWrap(true); name = "terrain-metrics" }
    private val preview = Table(skin)
    private val actionButtons = mutableListOf<TextButton>()

    init {
        TerrainGenerationPipeline.validate(source)
        val controls = Table(skin).apply { defaults().pad(4f).growX() }
        controls.add(Label("Terrain experiments", skin)).colspan(2).row()
        fun field(label: String, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            controls.add(Label(label, skin)).left()
            controls.add(actor).width(180f).row()
        }
        field("Seed", seed)
        field("Climate scale", climate)
        field("Vegetation scale", vegetation)
        field("Elevation scale", elevation)
        field("Generation mode", mode)
        field("View layer", layer)
        fun button(text: String, action: () -> Unit) {
            val button = TextButton(text, skin)
            button.onClick { if (!busy) safely(action) }
            actionButtons.add(button)
            controls.add(button).colspan(2).row()
        }
        button("Generate / repeat") { generate() }
        button("New seed") { seed.text = Random.nextLong().toString() }
        button("Show game terrain") { showGameTerrain() }
        button("Show selected layer") { result?.let { showLayer(it) } }
        controls.add(sourceInfo).colspan(2).width(340f).row()
        field("New grid shape", shape)
        field("Hex radius", radius)
        field("Rectangle width", gridWidth)
        field("Rectangle height", gridHeight)
        button("Create new land mask") { createLandMask() }
        controls.add(path).colspan(2).row()
        button("Open snapshot / report") { openSnapshot() }
        button("Export displayed result") { exportDisplayed() }
        controls.add(status).colspan(2).width(340f).row()
        controls.add(metrics).colspan(2).width(340f).row()
        val root = Table(skin).apply { setFillParent(true); defaults().pad(8f) }
        root.add(ScrollPane(controls, skin).apply { setScrollingDisabled(true, false) }).width(370f)
            .growY()
        root.add(preview).grow()
        stage.addActor(root)
        loadFields()
        layer.onChange { if (!busy) result?.let { showLayer(it) } }
        generate()
    }

    /** Снимок содержит исходные клетки, а не результат предыдущего нажатия Generate. */
    fun snapshotForRun(): TerrainExperimentInput {
        val settings = source.settings.copy(
            seed = seed.text.trim().toLong(), climateScale = climate.text.trim().toDouble(),
            vegetationScale = vegetation.text.trim().toDouble(), elevationScale = elevation.text.trim().toDouble(),
            mode = TerrainGenerationMode.valueOf(mode.selected))
        return source.copy(settings = settings).snapshot().also { TerrainGenerationPipeline.validate(it) }
    }

    private fun loadFields() {
        val settings = source.settings
        seed.text = settings.seed.toString()
        climate.text = settings.climateScale.toString()
        vegetation.text = settings.vegetationScale.toString()
        elevation.text = settings.elevationScale.toString()
        mode.selected = settings.mode.name
        sourceInfo.setText("Fixed source: ${source.cells.size} cells; " +
            "${source.cells.count { it.baseTerrain in source.rules.waterTerrains }} water.\n" +
            "Generate always starts from this source. New land mask replaces it explicitly.")
    }

    private fun createLandMask() {
        val settings = snapshotForRun().settings
        val terrain = source.rules.flatTerrain ?: source.rules.firstLandTerrain
        val cells = if (shape.selected == "hexagonal") TerrainGridFactory.hexagonal(radius.text.trim().toInt(), terrain)
        else TerrainGridFactory.rectangular(gridWidth.text.trim().toInt(), gridHeight.text.trim().toInt(), terrain)
        val candidate = TerrainExperimentInput(cells, source.rules, settings)
        TerrainGenerationPipeline.validate(candidate)
        source = candidate.snapshot()
        loadFields()
        generate()
    }

    private fun generate() {
        val input = snapshotForRun()
        val selectedLayer = layer.selected
        background("Generating…", {
            val generated = TerrainGenerationPipeline.generate(input)
            generated to renderPng(generated, selectedLayer)
        }) { (generated, bytes) -> publish(generated, bytes, selectedLayer) }
    }

    private fun showLayer(current: TerrainExperimentResult) {
        val selectedLayer = layer.selected
        background("Rendering…", { renderPng(current, selectedLayer) }) { bytes -> publish(current, bytes, selectedLayer) }
    }

    private fun openSnapshot() {
        val filename = path.text.trim()
        require(filename.isNotEmpty()) { "Enter a snapshot or report path" }
        val selectedLayer = layer.selected
        background("Opening and verifying…", {
            val input = readInput(filename).snapshot()
            val generated = TerrainGenerationPipeline.generate(input)
            generated to renderPng(generated, selectedLayer)
        }) { (generated, bytes) ->
            publish(generated, bytes, selectedLayer)
            source = generated.input.snapshot()
            loadFields()
        }
    }

    private fun exportDisplayed() {
        val current = requireNotNull(result) { "Generate a result first" }
        val directory = path.text.trim()
        require(directory.isNotEmpty()) { "Enter a new export directory" }
        background("Exporting displayed result…", { exportResult(directory, current) }) {
            status.setText("Exported displayed result to $directory")
        }
    }

    /** Texture создаётся только на GL-потоке; старое изображение сохраняется при ошибке вычисления. */
    private fun publish(generated: TerrainExperimentResult, bytes: ByteArray, selectedLayer: String) {
        val pixmap = Pixmap(bytes, 0, bytes.size)
        val replacement = try { Texture(pixmap) } finally { pixmap.dispose() }
        val old = texture
        preview.clearChildren()
        preview.add(Image(replacement).apply { setScaling(Scaling.fit) }).grow()
        texture = replacement
        old?.dispose()
        result = generated
        val summary = TerrainMetrics.layers(generated).entries.joinToString("\n") { (name, values) ->
            "$name: ${values.includedCells} land cells, ${values.componentAreas.values.sumOf { it.size }} regions, " +
                "${values.singletonCells} singletons; boundary ${"%.3f".format(java.util.Locale.ROOT, values.boundaryShare)}"
        }
        metrics.setText(summary)
        status.setText("Displayed: $selectedLayer; seed ${generated.input.settings.seed}; ${generated.input.settings.mode}\n" +
            "Scales: ${generated.input.settings.climateScale} / ${generated.input.settings.vegetationScale} / ${generated.input.settings.elevationScale}")
    }

    private fun <T> background(message: String, operation: () -> T, completed: (T) -> Unit) {
        if (busy || disposed) return
        busy = true
        actionButtons.forEach { it.isDisabled = true }
        layer.isDisabled = true
        status.setText(message)
        worker.execute {
            val outcome = runCatching(operation)
            Gdx.app.postRunnable {
                if (!disposed) {
                    busy = false
                    actionButtons.forEach { it.isDisabled = false }
                    layer.isDisabled = false
                    outcome.fold({ value -> safely { completed(value) } }, { failure -> status.setText("Error: ${failure.message}") })
                }
            }
        }
    }

    private fun safely(action: () -> Unit) {
        try { action() } catch (failure: Exception) { status.setText("Error: ${failure.message}") }
    }

    override fun dispose() {
        disposed = true
        worker.shutdownNow()
        texture?.dispose()
        texture = null
        super.dispose()
    }

    /** Готовность опубликованного изображения для внешнего предпросмотра и UI-проверок. */
    val isResultReady: Boolean
        get() = !busy && result != null && !disposed

    /** Возвращает отделённую копию именно показанного результата, а не текущего черновика формы. */
    fun displayedResultSnapshot(): TerrainExperimentResult? = result?.let {
        it.copy(input = it.input.snapshot(), cells = it.cells.map { cell ->
            cell.copy(features = cell.features.toList(), neighbors = cell.neighbors.toList())
        })
    }

    /** Строит игровой вид на GL-потоке; до успешного создания прежний preview остаётся на месте. */
    private fun showGameTerrain() {
        val current = requireNotNull(result) { "Generate a result first" }
        val map = com.unciv.logic.map.mapgenerator.TerrainPreviewMapAdapter.create(
            current, com.unciv.ui.images.ImageGetter.ruleset
        )
        val holder = EditorMapHolder(this, map) {}.apply {
            name = "terrain-game-preview"
            // Конструктор не подключил обработчики редактора; доступны только навигация и zoom.
            touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
        }
        preview.clearChildren()
        preview.add(holder).grow()
        preview.invalidateHierarchy()
        preview.validate()
        holder.layout()
        holder.scrollPercentX = 0.5f
        holder.scrollPercentY = 0.5f
        holder.updateVisualScroll()
        stage.scrollFocus = holder
        texture?.dispose()
        texture = null
        status.setText(
            "Displayed: GAME; seed ${current.input.settings.seed}; ${current.input.settings.mode}\n" +
                    "Scales: ${current.input.settings.climateScale} / ${current.input.settings.vegetationScale} / ${current.input.settings.elevationScale}"
        )
    }
}
