package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Scaling
import com.unciv.pure.application.mapgeneration.*
import com.unciv.pure.domain.mapgeneration.LatitudeRange
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.onClick
import java.util.concurrent.Executors
import kotlin.random.Random

/** Песочница без GameInfo; файлы и PNG обслуживают callbacks launcher. */
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
    private val latitudeMode = SelectBox<String>(skin).apply {
        name = "terrain-latitude-mode"
        setItems("Legacy", "Latitude range")
    }
    private val topLatitude = TextField("60", skin).apply { name = "terrain-top-latitude" }
    private val bottomLatitude = TextField("20", skin).apply { name = "terrain-bottom-latitude" }
    private val mode = SelectBox<String>(skin).apply { setItems(*TerrainGenerationMode.values().map { it.name }.toTypedArray()) }
    private val layer = SelectBox<String>(skin).apply {
        name = "terrain-layer"
        setItems("BASE", "TEMPERATURE", "HUMIDITY", "ELEVATION", "VEGETATION")
    }
    private val shape = SelectBox<String>(skin).apply {
        name = "terrain-shape"
        setItems("hexagonal", "rectangular")
    }
    private val radius = TextField("12", skin).apply { name = "terrain-radius" }
    private val gridWidth = TextField("30", skin).apply { name = "terrain-width" }
    private val gridHeight = TextField("20", skin).apply { name = "terrain-height" }
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
        fun field(table: Table, label: String, actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            table.add(Label(label, skin)).left()
            table.add(actor).width(180f).row()
        }
        fun button(text: String, action: () -> Unit) {
            val button = TextButton(text, skin)
            button.onClick { if (!busy) safely(action) }
            actionButtons.add(button)
            controls.add(button).colspan(2).row()
        }
        field(controls, "New grid shape", shape)
        val dimensions = Table(skin).apply { defaults().pad(4f).growX() }
        fun updateDimensions() {
            dimensions.clearChildren()
            if (shape.selected == "hexagonal") field(dimensions, "Hex radius", radius)
            else {
                field(dimensions, "Width (cells)", gridWidth)
                field(dimensions, "Height (cells)", gridHeight)
            }
        }
        shape.onChange { updateDimensions() }
        updateDimensions()
        controls.add(dimensions).colspan(2).growX().row()
        button("Create new land mask") { createLandMask() }
        controls.add(sourceInfo).colspan(2).width(340f).row()
        field(controls, "Seed", seed)
        button("New seed") { seed.text = Random.nextLong().toString() }
        field(controls, "Latitude model", latitudeMode)
        val latitudeFields = Table(skin).apply { defaults().pad(4f).growX() }
        fun updateLatitudeFields() {
            latitudeFields.clearChildren()
            if (latitudeMode.selected == "Latitude range") {
                field(latitudeFields, "Top latitude (deg)", topLatitude)
                field(latitudeFields, "Bottom latitude (deg)", bottomLatitude)
                latitudeFields.add(Label("North +90; equator 0; south -90", skin)).colspan(2).row()
            }
        }
        latitudeMode.onChange { updateLatitudeFields() }
        controls.add(latitudeFields).colspan(2).growX().row()
        field(controls, "Climate scale", climate)
        field(controls, "Vegetation scale", vegetation)
        field(controls, "Elevation scale", elevation)
        field(controls, "Generation mode", mode)
        button("Generate / repeat") { generate() }
        field(controls, "View layer", layer)
        button("Show game terrain") { showGameTerrain() }
        button("Show selected layer") { result?.let { showLayer(it) } }
        controls.add(path).colspan(2).row()
        button("Open snapshot / report") { openSnapshot() }
        button("Export displayed result") { exportDisplayed() }
        controls.add(status).colspan(2).width(340f).row()
        controls.add(metrics).colspan(2).width(340f).row()
        val root = Table(skin).apply { setFillParent(true); defaults().pad(8f) }
        root.add(ScrollPane(controls, skin).apply { setScrollingDisabled(true, false) }).width(370f).growY()
        root.add(preview).grow()
        stage.addActor(root)
        loadFields()
        updateLatitudeFields()
        layer.onChange { if (!busy) result?.let { showLayer(it) } }
        generate()
    }

    /** Читает только настройки: новая сетка проверяется уже вместе со своей геометрией. */
    private fun settingsFromFields(): TerrainExperimentSettings = source.settings.copy(
        seed = seed.text.trim().toLong(), climateScale = climate.text.trim().toDouble(),
        vegetationScale = vegetation.text.trim().toDouble(), elevationScale = elevation.text.trim().toDouble(),
        mode = TerrainGenerationMode.valueOf(mode.selected),
        latitudeRange = if (latitudeMode.selected == "Latitude range")
            LatitudeRange(topLatitude.text.trim().toDouble(), bottomLatitude.text.trim().toDouble()) else null
    )

    /** Повтор всегда использует исходные клетки, не результат предыдущей генерации. */
    fun snapshotForRun(): TerrainExperimentInput = source.copy(settings = settingsFromFields()).snapshot()
        .also { TerrainGenerationPipeline.validate(it) }

    private fun loadFields() {
        val settings = source.settings
        seed.text = settings.seed.toString()
        climate.text = settings.climateScale.toString()
        vegetation.text = settings.vegetationScale.toString()
        elevation.text = settings.elevationScale.toString()
        mode.selected = settings.mode.name
        latitudeMode.selected = if (settings.latitudeRange == null) "Legacy" else "Latitude range"
        topLatitude.text = (settings.latitudeRange?.top ?: 60.0).toString()
        bottomLatitude.text = (settings.latitudeRange?.bottom ?: 20.0).toString()
        sourceInfo.setText("Current source: ${source.cells.size} cells; " +
            "${source.cells.count { it.baseTerrain in source.rules.waterTerrains }} water.\n" +
            "Generate keeps this grid. Create new land mask applies the size above.")
    }

    /** Новый источник публикуется только после успешного расчёта и создания изображения. */
    private fun createLandMask() {
        val settings = settingsFromFields()
        val terrain = source.rules.flatTerrain ?: source.rules.firstLandTerrain
        val cells = if (shape.selected == "hexagonal") TerrainGridFactory.hexagonal(radius.text.trim().toInt(), terrain)
        else TerrainGridFactory.rectangular(gridWidth.text.trim().toInt(), gridHeight.text.trim().toInt(), terrain)
        val candidate = TerrainExperimentInput(cells, source.rules, settings).snapshot()
        TerrainGenerationPipeline.validate(candidate)
        val selectedLayer = layer.selected
        background("Creating grid…", {
            val generated = TerrainGenerationPipeline.generate(candidate)
            generated to renderPng(generated, selectedLayer)
        }) { (generated, bytes) ->
            publish(generated, bytes, selectedLayer)
            source = generated.input.snapshot()
            loadFields()
        }
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

    /** Texture создаётся только на GL-потоке; прежнее изображение живёт до успешной замены. */
    private fun publish(generated: TerrainExperimentResult, bytes: ByteArray, selectedLayer: String) {
        val pixmap = Pixmap(bytes, 0, bytes.size)
        val replacement = try { Texture(pixmap) } finally { pixmap.dispose() }
        val old = texture
        stage.scrollFocus = null
        preview.clearChildren()
        preview.add(Image(replacement).apply { setScaling(Scaling.fit) }).grow()
        texture = replacement
        old?.dispose()
        result = generated
        metrics.setText(TerrainMetrics.layers(generated).entries.joinToString("\n") { (name, values) ->
            "$name: ${values.includedCells} land cells, ${values.componentAreas.values.sumOf { it.size }} regions, " +
                "${values.singletonCells} singletons; boundary ${"%.3f".format(java.util.Locale.ROOT, values.boundaryShare)}"
        })
        status.setText(displayStatus(generated, selectedLayer))
    }

    /** Статус относится к показанному результату, а не изменяемому черновику формы. */
    private fun displayStatus(current: TerrainExperimentResult, view: String): String {
        val settings = current.input.settings
        val latitude = settings.latitudeRange?.let { "${it.top} to ${it.bottom} deg" } ?: "Legacy"
        return "Displayed: $view; seed ${settings.seed}; ${settings.mode}\n" +
            "Scales: ${settings.climateScale} / ${settings.vegetationScale} / ${settings.elevationScale}\n" +
            "Latitude: $latitude"
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

    /** Готовность опубликованного результата для внешнего предпросмотра и UI-проверок. */
    val isResultReady: Boolean
        get() = !busy && result != null && !disposed

    /** Отделённая копия показанного результата, а не текущего черновика. */
    fun displayedResultSnapshot(): TerrainExperimentResult? = result?.let {
        it.copy(input = it.input.snapshot(), cells = it.cells.map { cell ->
            cell.copy(features = cell.features.toList(), neighbors = cell.neighbors.toList())
        })
    }

    /** Игровой контейнер создаётся до замены preview, без обработчиков редактирования. */
    private fun showGameTerrain() {
        val current = requireNotNull(result) { "Generate a result first" }
        val map = com.unciv.logic.map.mapgenerator.TerrainPreviewMapAdapter.create(
            current, com.unciv.ui.images.ImageGetter.ruleset)
        val holder = EditorMapHolder(this, map) {}.apply {
            name = "terrain-game-preview"
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
        status.setText(displayStatus(current, "GAME"))
    }
}
