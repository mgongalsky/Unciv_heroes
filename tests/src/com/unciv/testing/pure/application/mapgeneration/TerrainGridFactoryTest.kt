package com.unciv.testing.pure.application.mapgeneration

import com.unciv.pure.application.mapgeneration.TerrainGridFactory
import org.junit.Assert.*
import org.junit.Test

/** Проверки геометрии стенда без графических типов. */
class TerrainGridFactoryTest {
    /** Прямоугольник сохраняет размер, уникальные координаты и симметричное соседство. */
    @Test
    fun rectangularGridHasExpectedSizeAndSymmetricEdges() {
        val cells = TerrainGridFactory.rectangular(7, 5, "Flat")
        assertEquals(35, cells.size)
        assertEquals(35, cells.map { it.column to it.row }.toSet().size)
        cells.forEachIndexed { index, cell ->
            assertTrue(cell.neighbors.size <= 6)
            assertTrue(cell.neighbors.all { index in cells[it].neighbors })
        }
    }

    /** Первый гексагональный пояс перечислен в историческом порядке TileMap. */
    @Test
    fun hexagonalRingUsesHistoricalOrder() {
        val cells = TerrainGridFactory.hexagonal(1, "Flat")
        assertEquals(listOf(0 to 0, 1 to -1, -1 to 1, 1 to 0, -1 to 0, 0 to 1, 0 to -1),
            cells.map { it.column to it.row })
        assertEquals(listOf(1, 2, 3, 4, 5, 6), cells[0].neighbors)
        assertEquals(37, TerrainGridFactory.hexagonal(3, "Flat").size)
    }

    /** Неподдержанный wrap отклоняется явно, а не создаёт незамкнутую карту под прежним названием. */
    @Test
    fun unsupportedWrapIsRejected() {
        try {
            TerrainGridFactory.rectangular(8, 5, "Flat", worldWrap = true)
            fail("Wrap accepted")
        } catch (_: IllegalArgumentException) { }
    }

    /** Нулевые размеры запрещены; одиночная клетка допустима для неклиматических этапов. */
    @Test
    fun validatesDimensions() {
        try {
            TerrainGridFactory.rectangular(0, 5, "Flat")
            fail("Empty dimension accepted")
        } catch (_: IllegalArgumentException) { }
        assertTrue(TerrainGridFactory.hexagonal(0, "Flat").single().neighbors.isEmpty())
    }
}
