package com.unciv.testing.simulations.terrain

import com.unciv.app.desktop.terrain.TerrainExperimentExport
import com.unciv.app.desktop.terrain.TerrainExperimentFiles
import com.unciv.app.desktop.terrain.TerrainExperimentSource
import com.unciv.pure.application.mapgeneration.*
import java.io.File

/** Пакетный запуск общего координатора без игрового приложения, окна и JUnit. */
object TerrainExperimentCli {
    private val options = setOf("input", "terrains", "shape", "radius", "width", "height", "output", "seeds",
        "climate-scales", "vegetation-scales", "elevation-scales", "mode")
    private const val USAGE = "TerrainExperimentCli (--input=<snapshot-or-report.json> | --terrains=<Terrains.json> " +
        "[--shape=hexagonal --radius=12 | --shape=rectangular --width=30 --height=20]) --output=<new-directory> " +
        "[--seeds=0,42,-17] [--climate-scales=6,12] [--vegetation-scales=3,6] " +
        "[--elevation-scales=2,4] [--mode=ALL|ELEVATION|CLIMATE|VEGETATION]"

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.contentEquals(arrayOf("--help"))) {
            println(USAGE)
            return
        }
        val parsed = linkedMapOf<String, String>()
        for (argument in args) {
            require(argument.startsWith("--") && '=' in argument) { "Expected --name=value. $USAGE" }
            val name = argument.substring(2).substringBefore('=')
            val value = argument.substringAfter('=')
            require(name in options) { "Unknown option: $name" }
            require(value.isNotBlank()) { "Empty option: $name" }
            require(parsed.put(name, value) == null) { "Duplicate option: $name" }
        }
        require(parsed.containsKey("input") xor parsed.containsKey("terrains")) {
            "Specify exactly one of --input or --terrains. $USAGE"
        }
        val output = File(requireNotNull(parsed["output"]) { "Missing --output. $USAGE" })
        require(!output.exists()) { "Output directory already exists: $output" }
        val requestedMode = parsed["mode"]?.let { TerrainGenerationMode.valueOf(it) }
        fun integer(name: String, default: Int) = parsed[name]?.let {
            requireNotNull(it.toIntOrNull()) { "Invalid integer in --$name: $it" }
        } ?: default
        val source = if (parsed.containsKey("input")) {
            require(setOf("shape", "radius", "width", "height").none { it in parsed }) {
                "Snapshot geometry cannot be overridden; use --terrains for a new grid"
            }
            TerrainExperimentFiles.readInput(File(parsed.getValue("input")))
        } else {
            val shape = parsed["shape"] ?: "hexagonal"
            require(shape in setOf("hexagonal", "rectangular")) { "Unknown experiment shape: $shape" }
            require(if (shape == "hexagonal") "width" !in parsed && "height" !in parsed else "radius" !in parsed) {
                "Grid dimensions do not match the selected shape"
            }
            TerrainExperimentSource.create(
                TerrainExperimentSource.loadRules(File(parsed.getValue("terrains"))), shape,
                radius = integer("radius", 12), width = integer("width", 30), height = integer("height", 20),
                settings = TerrainExperimentSettings(mode = requestedMode ?: TerrainGenerationMode.ALL))
        }
        val original = source.settings
        fun doubles(name: String, default: Double): List<Double> = parsed[name]?.split(',')?.map {
            requireNotNull(it.trim().toDoubleOrNull()) { "Invalid number in --$name: $it" }
        } ?: listOf(default)
        val seeds = parsed["seeds"]?.split(',')?.map {
            requireNotNull(it.trim().toLongOrNull()) { "Invalid seed: $it" }
        } ?: listOf(original.seed)
        val climates = doubles("climate-scales", original.climateScale)
        val vegetations = doubles("vegetation-scales", original.vegetationScale)
        val elevations = doubles("elevation-scales", original.elevationScale)
        val mode = requestedMode ?: original.mode
        val count = seeds.size.toLong() * climates.size * vegetations.size * elevations.size
        require(count in 1..1000) { "A batch must contain between 1 and 1000 experiments" }
        val configurations = mutableListOf<TerrainExperimentSettings>()
        for (seed in seeds) for (climate in climates) for (vegetation in vegetations) for (elevation in elevations) {
            val settings = original.copy(seed = seed, climateScale = climate, vegetationScale = vegetation,
                elevationScale = elevation, mode = mode)
            TerrainGenerationPipeline.validate(source.copy(settings = settings))
            configurations.add(settings)
        }
        check(output.mkdirs()) { "Cannot create output directory: $output" }
        // Результаты не становятся входами следующих запусков.
        TerrainExperimentFiles.writeInput(File(output, "input.json"), source)
        val index = File(output, "index.csv")
        index.writeText("run,seed,climateScale,vegetationScale,elevationScale,mode,report\n", Charsets.UTF_8)
        for ((number, settings) in configurations.withIndex()) {
            val name = "run-${number.toString().padStart(4, '0')}"
            val result = TerrainGenerationPipeline.generate(source.copy(settings = settings))
            TerrainExperimentExport.write(File(output, name), result)
            index.appendText("$name,${settings.seed},${settings.climateScale},${settings.vegetationScale}," +
                "${settings.elevationScale},${settings.mode},$name/result.json\n", Charsets.UTF_8)
            println("EXPORTED $name seed=${settings.seed} mode=${settings.mode}")
        }
        println("Completed ${configurations.size} terrain experiments: ${output.absolutePath}")
    }
}
