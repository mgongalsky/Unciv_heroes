package com.unciv.app.desktop.terrain

import com.unciv.pure.application.mapgeneration.*
import java.awt.Color
import java.awt.Polygon
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*

/** Файловый экспорт работает в headless JVM: не создаёт окна, Gdx-приложение или OpenGL-контекст. */
object TerrainExperimentExport {
    enum class Layer { BASE, TEMPERATURE, HUMIDITY, ELEVATION, VEGETATION }

    /** Каталог одного запуска должен быть новым, чтобы не смешивать результаты разных экспериментов. */
    fun write(directory: File, result: TerrainExperimentResult) {
        require(!directory.exists()) { "Export directory already exists: $directory" }
        TerrainGenerationPipeline.validate(result.input)
        require(TerrainGenerationPipeline.generate(result.input) == result) { "Result does not reproduce its input" }
        check(directory.mkdirs()) { "Cannot create export directory: $directory" }
        TerrainExperimentFiles.writeResult(File(directory, "result.json"), result)
        for (layer in Layer.values()) {
            check(ImageIO.write(render(result, layer), "png", File(directory, "${layer.name.lowercase()}.png"))) {
                "PNG writer unavailable"
            }
        }
        writeMetrics(File(directory, "metrics.csv"), result)
        val legend = mutableListOf(listOf("layer", "category", "rgb"))
        for (layer in listOf(Layer.BASE, Layer.ELEVATION, Layer.VEGETATION)) {
            for (label in result.cells.map { category(it, result.input.rules, layer) }.distinct().sorted()) {
                legend.add(listOf(layer.name, label, "%06x".format(categoryColor(label).rgb and 0xffffff)))
            }
        }
        legend.add(listOf("TEMPERATURE", "-1 to +1", "blue to red; missing: #707070"))
        legend.add(listOf("HUMIDITY", "0 to 1", "blue to red; missing: #707070"))
        writeCsv(File(directory, "legend.csv"), legend)
    }

    /** Единый масштаб осей сохраняет геометрию; положительное направление Y направлено вверх. */
    fun render(result: TerrainExperimentResult, layer: Layer, width: Int = 1000, height: Int = 800): BufferedImage {
        require(width in 64..4096 && height in 64..4096) { "Image dimensions must be between 64 and 4096" }
        require(result.cells.isNotEmpty()) { "Cannot render an empty map" }
        require(result.cells.all { it.x.isFinite() && it.y.isFinite() }) { "Non-finite image coordinates" }
        val cells = result.cells
        val minX = cells.minOf { it.x } - 1.0
        val maxX = cells.maxOf { it.x } + 1.0
        val minY = cells.minOf { it.y } - 1.0
        val maxY = cells.maxOf { it.y } + 1.0
        val scale = min((width - 16.0) / (maxX - minX), (height - 16.0) / (maxY - minY))
        val centerX = minX / 2 + maxX / 2
        val centerY = minY / 2 + maxY / 2
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(0x20242a)
            graphics.fillRect(0, 0, width, height)
            for (cell in cells) {
                val cx = width / 2.0 + (cell.x - centerX) * scale
                val cy = height / 2.0 - (cell.y - centerY) * scale
                val polygon = Polygon()
                repeat(6) { corner ->
                    val angle = PI / 6 + corner * PI / 3
                    polygon.addPoint((cx + cos(angle) * scale).roundToInt(), (cy - sin(angle) * scale).roundToInt())
                }
                graphics.color = when (layer) {
                    Layer.TEMPERATURE -> heat(cell.temperature?.let { (it + 1.0) / 2.0 })
                    Layer.HUMIDITY -> heat(cell.humidity)
                    else -> categoryColor(category(cell, result.input.rules, layer))
                }
                graphics.fillPolygon(polygon)
            }
        } finally {
            graphics.dispose()
        }
        return image
    }

    private fun heat(value: Double?): Color {
        if (value == null || !value.isFinite()) return Color(0x707070)
        val normalized = value.coerceIn(0.0, 1.0)
        return Color((255 * normalized).roundToInt(), 64, (255 * (1 - normalized)).roundToInt())
    }

    private fun category(cell: TerrainSurfaceCell, rules: TerrainRuleSnapshot, layer: Layer): String {
        if (layer == Layer.BASE) return cell.baseTerrain
        if (cell.baseTerrain in rules.waterTerrains) return "water"
        return when (layer) {
            Layer.ELEVATION -> when {
                cell.baseTerrain == rules.mountainTerrain -> "mountain"
                rules.hillTerrain != null && rules.hillTerrain in cell.features -> "hill"
                else -> "flat"
            }
            Layer.VEGETATION -> cell.features.lastOrNull { feature -> rules.vegetation.any { it.terrain == feature } }
                ?.let { "terrain:$it" } ?: "none"
            else -> error("Not a categorical layer")
        }
    }

    /** Цвет зависит от строки, а не от порядка обхода или identity hash объектов ruleset. */
    private fun categoryColor(label: String): Color = when (label) {
        "water" -> Color(0x2864a0)
        "mountain" -> Color(0xaaaaaa)
        "hill" -> Color(0x88704c)
        "flat", "none" -> Color(0xc9c3a0)
        else -> Color.getHSBColor((label.hashCode().toLong() and 0xffffffffL).rem(360).toFloat() / 360f, 0.55f, 0.85f)
    }

    private fun writeMetrics(file: File, result: TerrainExperimentResult) {
        val rows = mutableListOf(listOf("layer", "category", "cells", "share", "componentAreas", "includedCells",
            "smallComponentCellShare", "singletonCells", "edges", "boundaryEdges", "boundaryShare"))
        for ((layer, metrics) in TerrainMetrics.layers(result)) {
            for (label in metrics.counts.keys.ifEmpty { setOf("") }) {
                rows.add(listOf(layer, label, (metrics.counts[label] ?: 0).toString(),
                    (metrics.shares[label] ?: 0.0).toString(), metrics.componentAreas[label].orEmpty().joinToString(";"),
                    metrics.includedCells.toString(), metrics.smallComponentCellShare.toString(),
                    metrics.singletonCells.toString(), metrics.edges.toString(), metrics.boundaryEdges.toString(),
                    metrics.boundaryShare.toString()))
            }
        }
        writeCsv(file, rows)
    }

    private fun writeCsv(file: File, rows: List<List<String>>) {
        file.writeText(rows.joinToString("\n", postfix = "\n") { row ->
            row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }
        }, Charsets.UTF_8)
    }
}
