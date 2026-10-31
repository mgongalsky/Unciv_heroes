package com.unciv.pure.domain.mapgeneration

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.random.Random

/** Числовая геометрия и состояние клетки; соседство задано индексами в исходном порядке обхода. */
data class ElevationCell(
    val x: Double,
    val y: Double,
    val baseTerrain: String,
    val features: List<String>,
    val water: Boolean,
    val neighbors: List<Int>
)

/** Факты правил рельефа, подготовленные адаптером без передачи Ruleset в чистое ядро. */
data class ElevationRules(
    val flat: String?,
    val mountain: String?,
    val hill: String?,
    val impassableTerrains: Set<String>,
    val waterTerrains: Set<String>,
    val risingMarker: String,
    val loweringMarker: String
)

/** Собственный результат клетки; список особенностей не разделяется с изменяемым входом. */
data class ElevationResult(val baseTerrain: String, val features: List<String>)

/** Исторический рельеф: шум из шести октав и клеточная обработка с отложенным применением пометок. */
object ElevationGenerator {
    fun generate(
        cells: List<ElevationCell>, rules: ElevationRules, exponent: Float,
        random: Random, scale: Double = 2.0
    ): List<ElevationResult> {
        require(scale.isFinite() && scale > 0.0) { "Elevation scale must be positive and finite" }
        require(exponent.isFinite()) { "Elevation exponent must be finite" }
        require(cells.all { cell -> cell.neighbors.all { it in cells.indices } }) { "Invalid elevation neighbor index" }
        val flat = rules.flat ?: return cells.map { ElevationResult(it.baseTerrain, it.features.toList()) }
        val seed = random.nextInt().toDouble()
        val bases = cells.map { it.baseTerrain }.toMutableList()
        val features = cells.map { it.features.toMutableList() }
        val water = cells.map { it.water }.toMutableList()
        val mountain = rules.mountain
        val hill = rules.hill
        for (index in cells.indices) {
            if (water[index]) continue
            val cell = cells[index]
            var elevation = PerlinNoise.noise3d(cell.x, cell.y, seed, nOctaves = 6, scale = scale)
            elevation = abs(elevation).pow(1.0 - exponent.toDouble()) * elevation.sign
            when {
                elevation <= 0.5 -> bases[index] = flat
                elevation <= 0.7 && hill != null -> features[index].add(hill)
                elevation <= 0.7 && hill == null -> bases[index] = flat
                elevation <= 1.0 && mountain != null -> bases[index] = mountain
            }
            water[index] = bases[index] in rules.waterTerrains
        }
        if (mountain != null) {
            val targetMountains = bases.count { it == mountain } * 2
            repeat(5) {
                var totalMountains = bases.count { it == mountain }
                for (index in cells.indices) {
                    if (water[index]) continue
                    val neighbors = cells[index].neighbors
                    val adjacentMountains = neighbors.count { bases[it] == mountain }
                    val adjacentImpassable = neighbors.count { bases[it] in rules.impassableTerrains }
                    if (adjacentMountains == 0 && bases[index] == mountain) {
                        if (random.nextInt(4) == 0) features[index].add(rules.loweringMarker)
                    } else if (adjacentMountains == 1) {
                        if (random.nextInt(10) == 0) features[index].add(rules.risingMarker)
                    } else if (adjacentImpassable == 3) {
                        if (random.nextInt(2) == 0) features[index].add(rules.loweringMarker)
                    } else if (adjacentImpassable > 3) {
                        features[index].add(rules.loweringMarker)
                    }
                }
                for (index in cells.indices) {
                    if (water[index]) continue
                    val tileFeatures = features[index]
                    if (rules.risingMarker in tileFeatures) {
                        tileFeatures.remove(rules.risingMarker)
                        // continue пропускает также lowering той же клетки — историческая семантика.
                        if (totalMountains >= targetMountains) continue
                        if (hill != null) tileFeatures.remove(hill)
                        bases[index] = mountain
                        totalMountains++
                    }
                    if (rules.loweringMarker in tileFeatures) {
                        tileFeatures.remove(rules.loweringMarker)
                        if (totalMountains <= targetMountains * 0.5f) continue
                        if (bases[index] == mountain) {
                            if (hill != null && hill !in tileFeatures) tileFeatures.add(hill)
                            totalMountains--
                        }
                        bases[index] = flat
                    }
                }
            }
        }
        if (hill != null) {
            val targetHills = features.count { hill in it }
            for (iteration in 1..5) {
                var totalHills = features.count { hill in it }
                for (index in cells.indices) {
                    if (water[index] || (mountain != null && bases[index] == mountain)) continue
                    val neighbors = cells[index].neighbors
                    val adjacentMountains = if (mountain == null) 0 else neighbors.count { bases[it] == mountain }
                    val adjacentHills = neighbors.count { hill in features[it] }
                    if (adjacentHills <= 1 && adjacentMountains == 0 && random.nextInt(2) == 0) {
                        features[index].add(rules.loweringMarker)
                    } else if (adjacentHills > 3 && adjacentMountains == 0 && random.nextInt(2) == 0) {
                        features[index].add(rules.loweringMarker)
                    } else if (adjacentHills + adjacentMountains in 2..3 && random.nextInt(2) == 0) {
                        features[index].add(rules.risingMarker)
                    }
                }
                for (index in cells.indices) {
                    if (water[index] || (mountain != null && bases[index] == mountain)) continue
                    val tileFeatures = features[index]
                    if (rules.risingMarker in tileFeatures) {
                        tileFeatures.remove(rules.risingMarker)
                        if (totalHills > targetHills && iteration != 1) continue
                        if (hill !in tileFeatures) {
                            tileFeatures.add(hill)
                            totalHills++
                        }
                    }
                    if (rules.loweringMarker in tileFeatures) {
                        tileFeatures.remove(rules.loweringMarker)
                        if (totalHills >= targetHills * 0.9f || iteration == 1) {
                            if (hill in tileFeatures) tileFeatures.remove(hill)
                            // Legacy уменьшает счётчик даже при отсутствии холма; не исправлять при извлечении.
                            totalHills--
                        }
                    }
                }
            }
        }
        return cells.indices.map { ElevationResult(bases[it], features[it].toList()) }
    }
}
