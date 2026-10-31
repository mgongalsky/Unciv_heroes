package com.unciv.pure.domain.mapgeneration

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

/** Геометрия клетки; blocked задаёт обязательную непроходимую клетку внешнего контура. */
data class MountainZoneCell(
    val x: Double,
    val y: Double,
    val neighbors: List<Int>,
    val blocked: Boolean = false
)

/** CROSS связывает периферию только с центром; NETWORK строит дерево соседних областей. */
enum class MountainZoneMode { CROSS, NETWORK }

/** Одна клетка guard перекрывает узкий проход между двумя зонами. */
data class MountainZonePassage(val first: Int, val second: Int, val guard: Int, val cells: List<Int>)

/** zoneByCell == -1 означает горную стену; centers находятся внутри проходимых областей. */
data class MountainZoneLayout(
    val zoneByCell: List<Int>,
    val centers: List<Int>,
    val passages: List<MountainZonePassage>
)

/** Строит связные области и проверенные узкие проходы, не меняя входной граф. */
object MountainZoneGenerator {
    fun generate(cells: List<MountainZoneCell>, count: Int, mode: MountainZoneMode, seed: Long): MountainZoneLayout {
        require(count in 3..12) { "Mountain zone count must be between 3 and 12" }
        require(cells.size >= count * 30) { "Map is too small for $count mountain zones; increase map size or reduce zone count" }
        require(cells.all { c -> c.x.isFinite() && c.y.isFinite() &&
            c.neighbors.distinct().size == c.neighbors.size && c.neighbors.all { it in cells.indices } }) {
            "Invalid mountain zone geometry"
        }
        require(cells.indices.all { i -> i !in cells[i].neighbors && cells[i].neighbors.all { i in cells[it].neighbors } }) {
            "Mountain zone graph must have symmetric edges without self-loops"
        }
        require(components(cells, IntArray(cells.size), emptySet()).size == 1) { "Mountain zone graph is disconnected" }
        val minX = cells.minOf { it.x }
        val minY = cells.minOf { it.y }
        val width = cells.maxOf { it.x } - minX
        val height = cells.maxOf { it.y } - minY
        require(width > 0.0 && height > 0.0) { "Mountain zones require a two-dimensional map" }
        val points = cells.map { ((it.x - minX) / width * 2 - 1) to ((it.y - minY) / height * 2 - 1) }
        val random = Random(seed)
        repeat(32) {
            val labels = if (mode == MountainZoneMode.CROSS) crossLabels(points, count, random)
                else networkLabels(points, count, random)
            val result = connect(cells, labels, count, mode, random)
            if (result != null) return result
        }
        throw IllegalArgumentException("Cannot fit $count connected mountain zones on this map; increase map size or reduce zone count")
    }

    private fun crossLabels(points: List<Pair<Double, Double>>, count: Int, random: Random): IntArray {
        val rotation = random.nextDouble() * 2 * PI
        val radius = 0.34 + random.nextDouble() * 0.12
        return IntArray(points.size) { i ->
            val (x, y) = points[i]
            if (hypot(x, y) < radius) 0 else {
                val angle = (atan2(y, x) + rotation + 4 * PI) % (2 * PI)
                1 + (angle / (2 * PI) * (count - 1)).toInt().coerceAtMost(count - 2)
            }
        }
    }

    private fun networkLabels(points: List<Pair<Double, Double>>, count: Int, random: Random): IntArray {
        val sites = mutableListOf(random.nextInt(points.size))
        fun distance(first: Int, second: Int): Double {
            val dx = points[first].first - points[second].first
            val dy = points[first].second - points[second].second
            return dx * dx + dy * dy
        }
        while (sites.size < count) {
            val next = points.indices.maxByOrNull { i -> sites.minOf { distance(i, it) } * (0.85 + random.nextDouble() * 0.3) }!!
            sites.add(next)
        }
        return IntArray(points.size) { i -> sites.indices.minByOrNull { distance(i, sites[it]) }!! }
    }

    private fun connect(
        cells: List<MountainZoneCell>, labels: IntArray, count: Int,
        mode: MountainZoneMode, random: Random
    ): MountainZoneLayout? {
        // Обязательные стены и обе стороны границ исключают диагональные обходы.
        val owners = IntArray(cells.size) { i ->
            if (cells[i].blocked || cells[i].neighbors.any { labels[it] != labels[i] }) -1 else labels[i]
        }
        val centers = mutableListOf<Int>()
        for (zone in 0 until count) {
            val room = components(
                cells,
                IntArray(cells.size) { if (owners[it] == zone) zone else -1 },
                emptySet()
            )
                .maxByOrNull { it.size } ?: return null
            if (room.size < 6) return null
            val keep = room.toHashSet()
            for (i in cells.indices) if (owners[i] == zone && i !in keep) owners[i] = -1
            centers.add(room.first())
        }
        val candidates = if (mode == MountainZoneMode.CROSS) (1 until count).map { 0 to it }
        else cells.indices.flatMap { i ->
            cells[i].neighbors.mapNotNull { j ->
                val a = labels[i];
                val b = labels[j]
                if (a < b) a to b else null
            }
        }.distinct().shuffled(random)
        val roots = IntArray(count) { it }
        fun root(zone: Int): Int {
            var z = zone; while (roots[z] != z) z = roots[z]; return z
        }

        val passages = mutableListOf<MountainZonePassage>()
        val allowed = mutableSetOf<Pair<Int, Int>>()
        for ((a, b) in candidates) {
            if (root(a) == root(b)) continue
            val path = path(cells, labels, owners, a, b, random) ?: continue
            val opened = path.filter { owners[it] < 0 }
            if (opened.isEmpty()) continue
            val before = count - passages.size
            opened.forEach { owners[it] = labels[it] }
            val permitted = allowed + (a to b)
            val edgesValid = cells.indices.all { i ->
                owners[i] < 0 || cells[i].neighbors.all { j ->
                    owners[j] < 0 || owners[i] == owners[j] ||
                            (minOf(owners[i], owners[j]) to maxOf(
                                owners[i],
                                owners[j]
                            )) in permitted
                }
            }
            val guard = if (edgesValid && components(cells, owners, emptySet()).size == before - 1)
                opened.firstOrNull { components(cells, owners, setOf(it)).size == before } else null
            if (guard == null) {
                opened.forEach { owners[it] = -1 }; continue
            }
            passages.add(MountainZonePassage(a, b, guard, opened.toList()))
            allowed.add(a to b)
            roots[root(a)] = root(b)
            if (passages.size == count - 1) break
        }
        if (passages.size != count - 1) return null
        if (components(cells, owners, passages.map { it.guard }.toSet()).size != count) return null
        if (passages.any { components(cells, owners, setOf(it.guard)).size != 2 }) return null
        return MountainZoneLayout(owners.toList(), centers.toList(), passages.toList())
    }

    /** Коридор не пересекает обязательные стены и не касается третьей открытой зоны. */
    private fun path(
        cells: List<MountainZoneCell>, labels: IntArray, owners: IntArray,
        a: Int, b: Int, random: Random
    ): List<Int>? {
        val previous = IntArray(cells.size) { -2 }
        val queue = ArrayDeque<Int>()
        cells.indices.filter { owners[it] == a }.shuffled(random)
            .forEach { previous[it] = -1; queue.addLast(it) }
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            if (owners[i] == b) {
                val result = mutableListOf<Int>()
                var cursor = i
                while (cursor >= 0) {
                    result.add(cursor); cursor = previous[cursor]
                }
                return result.asReversed().toList()
            }
            for (j in cells[i].neighbors) {
                if (previous[j] != -2 || cells[j].blocked || (labels[j] != a && labels[j] != b)) continue
                if (cells[j].neighbors.any { owners[it] >= 0 && owners[it] != a && owners[it] != b }) continue
                previous[j] = i
                queue.addLast(j)
            }
        }
        return null
    }

    private fun components(cells: List<MountainZoneCell>, owners: IntArray, removed: Set<Int>): List<List<Int>> {
        val seen = BooleanArray(cells.size)
        val result = mutableListOf<List<Int>>()
        for (start in cells.indices) {
            if (seen[start] || owners[start] < 0 || start in removed) continue
            val component = mutableListOf<Int>()
            val queue = ArrayDeque<Int>()
            seen[start] = true
            queue.addLast(start)
            while (queue.isNotEmpty()) {
                val i = queue.removeFirst()
                component.add(i)
                for (j in cells[i].neighbors) if (!seen[j] && owners[j] >= 0 && j !in removed) {
                    seen[j] = true
                    queue.addLast(j)
                }
            }
            result.add(component)
        }
        return result
    }
}
