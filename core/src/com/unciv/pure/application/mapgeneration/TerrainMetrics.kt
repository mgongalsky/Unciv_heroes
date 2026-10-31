package com.unciv.pure.application.mapgeneration

/** Показатели одного слоя; исключённые клетки и рёбра к ним не входят в знаменатели. */
data class TerrainLayerMetrics(
    val includedCells: Int,
    val counts: Map<String, Int>,
    val shares: Map<String, Double>,
    val componentAreas: Map<String, List<Int>>,
    val smallComponentCellShare: Double,
    val singletonCells: Int,
    val edges: Int,
    val boundaryEdges: Int,
    val boundaryShare: Double
)

/** Анализ связных областей по явному графу, включая переданные замыкающие рёбра. */
object TerrainMetrics {
    fun measure(
        labels: List<String?>,
        neighbors: List<List<Int>>,
        smallAreaLimit: Int = 3
    ): TerrainLayerMetrics {
        require(labels.size == neighbors.size) { "Labels and graph sizes differ" }
        require(smallAreaLimit >= 1) { "Small area limit must be positive" }
        for ((index, adjacent) in neighbors.withIndex()) {
            require(adjacent.distinct().size == adjacent.size && index !in adjacent) { "Duplicate or self edge" }
            require(adjacent.all { it in labels.indices }) { "Neighbor outside graph" }
            require(adjacent.all { index in neighbors[it] }) { "Asymmetric graph" }
        }
        val counts = labels.filterNotNull().groupingBy { it }.eachCount().toSortedMap()
        val total = counts.values.sum()
        val visited = BooleanArray(labels.size)
        val areas = sortedMapOf<String, MutableList<Int>>()
        var smallCells = 0
        var singletons = 0
        for (start in labels.indices) {
            val label = labels[start] ?: continue
            if (visited[start]) continue
            val queue = java.util.ArrayDeque<Int>()
            queue.add(start)
            visited[start] = true
            var area = 0
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                area++
                for (next in neighbors[current]) {
                    if (!visited[next] && labels[next] == label) {
                        visited[next] = true
                        queue.addLast(next)
                    }
                }
            }
            areas.getOrPut(label) { mutableListOf() }.add(area)
            if (area <= smallAreaLimit) smallCells += area
            if (area == 1) singletons++
        }
        var edges = 0
        var boundaries = 0
        for (index in labels.indices) {
            if (labels[index] == null) continue
            for (next in neighbors[index]) {
                if (next <= index || labels[next] == null) continue
                edges++
                if (labels[index] != labels[next]) boundaries++
            }
        }
        return TerrainLayerMetrics(total, counts,
            counts.mapValues { (_, count) -> if (total == 0) 0.0 else count.toDouble() / total },
            areas.mapValues { (_, values) -> values.sorted() },
            if (total == 0) 0.0 else smallCells.toDouble() / total,
            singletons, edges, boundaries, if (edges == 0) 0.0 else boundaries.toDouble() / edges)
    }

    /** Вода исключена из всех трёх слоёв; отсутствие леса или холмов является самостоятельной категорией. */
    fun layers(result: TerrainExperimentResult): Map<String, TerrainLayerMetrics> {
        val cells = result.cells
        val rules = result.input.rules
        val graph = cells.map { it.neighbors }
        val vegetation = rules.vegetation.map { it.terrain }.toSet()
        fun labels(select: (TerrainSurfaceCell) -> String) = cells.map {
            if (it.baseTerrain in rules.waterTerrains) null else select(it)
        }
        return linkedMapOf(
            "base" to measure(labels { it.baseTerrain }, graph),
            "elevation" to measure(labels {
                when {
                    it.baseTerrain == rules.mountainTerrain -> "mountain"
                    rules.hillTerrain != null && rules.hillTerrain in it.features -> "hill"
                    else -> "flat"
                }
            }, graph),
            "vegetation" to measure(labels { cell ->
                cell.features.lastOrNull { it in vegetation }?.let { "terrain:$it" } ?: "none"
            }, graph)
        )
    }
}
