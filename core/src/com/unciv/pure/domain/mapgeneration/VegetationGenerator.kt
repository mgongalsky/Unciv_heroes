package com.unciv.pure.domain.mapgeneration

import kotlin.random.Random

/** Допустимые основания типа растительности; порядок списка типов задаёт вызывающий адаптер. */
data class VegetationRule(val terrain: String, val occursOn: List<String>)

/** Снимок основания и верхнего террейна до запуска этапа, без ссылок на игровую карту. */
data class VegetationCell(val x: Double, val y: Double, val baseTerrain: String, val lastTerrain: String)

/** Вычисляет добавляемые особенности, сохраняя порог, порядок кандидатов и расход исторического RNG. */
object VegetationGenerator {
    fun generate(
        cells: List<VegetationCell>,
        rules: List<VegetationRule>,
        richness: Float,
        random: Random,
        scale: Double = 3.0
    ): List<String?> {
        require(scale.isFinite() && scale > 0.0) { "Vegetation scale must be positive and finite" }
        require(richness.isFinite()) { "Vegetation richness must be finite" }
        // Seed потребляется даже при отсутствии растительности или подходящих клеток.
        val seed = random.nextInt().toDouble()
        val candidateTerrains = rules.flatMap { it.occursOn }.toSet()
        return cells.map { cell ->
            if (cell.baseTerrain !in candidateTerrains || cell.lastTerrain !in candidateTerrains) return@map null
            val vegetation = (PerlinNoise.noise3d(cell.x, cell.y, seed, nOctaves = 1, scale = scale) + 1.0) / 2.0
            if (vegetation <= richness) rules.filter { cell.lastTerrain in it.occursOn }.random(random).terrain
            else null
        }
    }
}
