package com.unciv.testing.pure.domain.mapgeneration

import com.unciv.pure.domain.mapgeneration.MountainZoneCell
import com.unciv.pure.domain.mapgeneration.MountainZoneGenerator
import com.unciv.pure.domain.mapgeneration.MountainZoneLayout
import com.unciv.pure.domain.mapgeneration.MountainZoneMode
import org.junit.Assert.*
import org.junit.Test

/** Проверки физических проходов на графе без движка, файлов, правил и глобального состояния. */
class MountainZoneGeneratorTest {
    /** Периферийные области связаны только через центр даже при соседстве через шов. */
    @Test
    fun crossHasNoPeripheralBypass() {
        for (cells in listOf(rectangle(36, 28), hexagon(18), rectangle(40, 30, true))) {
            for (seed in listOf(1L, 42L)) {
                val layout = MountainZoneGenerator.generate(cells, 5, MountainZoneMode.CROSS, seed)
                verify(cells, layout, 5)
                assertTrue(layout.passages.all { it.first == 0 })
                val outer = components(cells, layout, cells.indices.filter { layout.zoneByCell[it] == 0 }.toSet())
                assertEquals(4, outer.size)
                assertTrue(outer.all { component -> component.map { layout.zoneByCell[it] }.toSet().size == 1 })
            }
        }
    }

    /** Для разных чисел зон дерево соединений остаётся связным и каждый проход перекрывается одним защитником. */
    @Test
    fun networkSupportsConfiguredCountsAndSingleCellGates() {
        val cells = rectangle(48, 36)
        for (count in listOf(3, 5, 8, 12)) {
            verify(cells, MountainZoneGenerator.generate(cells, count, MountainZoneMode.NETWORK, 42L), count)
        }
    }

    /** Крайние значения числа зон применяются и к шаблону с центральной областью. */
    @Test
    fun crossSupportsMinimumAndMaximumZoneCounts() {
        val cells = hexagon(26)
        for (count in listOf(3, 12)) {
            verify(cells, MountainZoneGenerator.generate(cells, count, MountainZoneMode.CROSS, 19L), count)
        }
    }

    /** Одинаковый seed даёт тот же результат; другой seed меняет расположение границ или проходов. */
    @Test
    fun seedIsDeterministicAndInputRemainsUnchanged() {
        val cells = rectangle(36, 28)
        val before = cells.map { it.copy(neighbors = it.neighbors.toList()) }
        for (mode in MountainZoneMode.values()) {
            val first = MountainZoneGenerator.generate(cells, 5, mode, 42L)
            assertEquals(first, MountainZoneGenerator.generate(cells, 5, mode, 42L))
            assertNotEquals(first, MountainZoneGenerator.generate(cells, 5, mode, 73L))
        }
        assertEquals(before, cells)
    }

    /** Ошибочные размеры, число зон и некорректное соседство отклоняются без частичного результата. */
    @Test
    fun rejectsInvalidGeometryAndCounts() {
        val cells = rectangle(12, 10)
        for (count in listOf(0, 2, 13)) expectInvalid {
            MountainZoneGenerator.generate(cells, count, MountainZoneMode.CROSS, 42L)
        }
        expectInvalid { MountainZoneGenerator.generate(rectangle(5, 5), 3, MountainZoneMode.NETWORK, 42L) }
        expectInvalid { MountainZoneGenerator.generate(cells.map { it.copy(neighbors = emptyList()) }, 3, MountainZoneMode.CROSS, 42L) }
        expectInvalid { MountainZoneGenerator.generate(cells.toMutableList().apply {
            this[0] = this[0].copy(neighbors = listOf(cells.size))
        }, 3, MountainZoneMode.CROSS, 42L) }
        expectInvalid { MountainZoneGenerator.generate(cells.toMutableList().apply {
            this[0] = this[0].copy(x = Double.NaN)
        }, 3, MountainZoneMode.CROSS, 42L) }
    }

    private fun verify(cells: List<MountainZoneCell>, layout: MountainZoneLayout, count: Int) {
        assertEquals(cells.size, layout.zoneByCell.size)
        assertEquals(count, layout.centers.size)
        assertEquals(count - 1, layout.passages.size)
        assertTrue(layout.zoneByCell.any { it == -1 })
        assertEquals((0 until count).toSet(), layout.zoneByCell.filter { it >= 0 }.toSet())
        assertEquals(1, components(cells, layout, emptySet()).size)
        assertEquals(count, components(cells, layout, layout.passages.map { it.guard }.toSet()).size)
        layout.centers.forEachIndexed { zone, cell -> assertEquals(zone, layout.zoneByCell[cell]) }
        for (passage in layout.passages) {
            assertTrue(passage.guard in passage.cells)
            assertTrue(passage.cells.all { layout.zoneByCell[it] >= 0 })
            assertEquals("Guard does not block passage $passage", 2, components(cells, layout, setOf(passage.guard)).size)
        }
    }

    private fun components(cells: List<MountainZoneCell>, layout: MountainZoneLayout, removed: Set<Int>): List<Set<Int>> {
        val remaining = cells.indices.filter { layout.zoneByCell[it] >= 0 && it !in removed }.toMutableSet()
        val result = mutableListOf<Set<Int>>()
        while (remaining.isNotEmpty()) {
            val queue = ArrayDeque<Int>()
            val component = mutableSetOf<Int>()
            queue.addLast(remaining.first())
            remaining.remove(queue.first())
            while (queue.isNotEmpty()) {
                val i = queue.removeFirst()
                component.add(i)
                for (j in cells[i].neighbors) if (remaining.remove(j)) queue.addLast(j)
            }
            result.add(component)
        }
        return result
    }

    private fun rectangle(width: Int, height: Int, wrap: Boolean = false): List<MountainZoneCell> =
        graph((0 until width).flatMap { q -> (0 until height).map { r -> q to r } },
            if (wrap) width to height else null)

    private fun hexagon(radius: Int): List<MountainZoneCell> = graph(
        (-radius..radius).flatMap { q -> (-radius..radius).filter { r -> kotlin.math.abs(q + r) <= radius }.map { q to it } }
    )

    /** Обе координаты замыкаются для более строгой проверки обходов через шов. */
    private fun graph(points: List<Pair<Int, Int>>, wrap: Pair<Int, Int>? = null): List<MountainZoneCell> {
        val indices = points.withIndex().associate { it.value to it.index }
        val offsets = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to -1, -1 to 1)
        return points.map { (q, r) -> MountainZoneCell(q + r * 0.5, r * 0.866025403784,
            offsets.mapNotNull { (dq, dr) ->
                val x = if (wrap == null) q + dq else Math.floorMod(q + dq, wrap.first)
                val y = if (wrap == null) r + dr else Math.floorMod(r + dr, wrap.second)
                indices[x to y]
            }) }
    }

    private fun expectInvalid(action: () -> Unit) {
        try { action(); fail("Invalid mountain zone input accepted") } catch (_: IllegalArgumentException) { }
    }

    /** Обязательные препятствия внешнего контура не могут быть открыты поиском прохода. */
    @Test
    fun mandatoryWallsAreNeverCarvedIntoPassages() {
        val cells = rectangle(36, 28).map { it.copy(blocked = it.x < 2.0 || it.x > 46.0) }
        assertTrue(cells.any { it.blocked })
        for (mode in MountainZoneMode.values()) {
            val layout = MountainZoneGenerator.generate(cells, 5, mode, 42L)
            verify(cells, layout, 5)
            for (i in cells.indices) if (cells[i].blocked) assertEquals(-1, layout.zoneByCell[i])
        }
    }
}
