package com.unciv.testing.pure.application.battle.session

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.battle.session.BattleFieldPreparation
import com.unciv.logic.map.ClimateParameters
import org.junit.Assert.*
import org.junit.Test

class BattleFieldPreparationTest {
    @Test
    fun descriptionMatchesEveryCombatCellIncludingObstacles() {
        val prepared = BattleFieldPreparation.create(
            ClimateParameters(0.3, 0.5, 0.6), ClimateParameters(0.9, 0.5, 0.6)
        ) { it == "Mountain" }
        val description = prepared.description
        assertEquals(112, description.cells.size)
        assertEquals(description.cells.size, prepared.field.values.size)
        assertEquals(description.cells.size, description.cells.map { it.position }.distinct().size)
        assertTrue(description.cells.any { it.impassable })
        assertTrue(description.cells.any { !it.impassable })
        description.cells.forEach { cell ->
            val tile = requireNotNull(prepared.field.getTileAt(
                Vector2(cell.position.x.toFloat(), cell.position.y.toFloat())
            ))
            assertEquals(cell.impassable, tile.isImpassible())
            assertNull(tile.getTroop())
        }
    }

    @Test
    fun lastFeatureDeterminesPassability() {
        val climate = ClimateParameters(0.6, 0.5, 0.6)
        val consulted = mutableListOf<String>()
        val prepared = BattleFieldPreparation.create(climate, climate) {
            consulted.add(it)
            it == "Hill"
        }
        assertEquals(112, consulted.size)
        assertTrue(consulted.all { it == "Hill" })
        assertTrue(prepared.description.cells.all { it.features == listOf("Hill") && it.impassable })
        assertTrue(prepared.field.values.all { it.isImpassible() })
    }

    @Test
    fun sameSeedReproducesTerrainButCreatesIndependentFields() {
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        fun prepare(seed: Long) = BattleFieldPreparation.create(
            climate, climate, terrainSeed = seed
        ) { false }
        val first = prepare(42L)
        val second = prepare(42L)
        assertEquals(first.description, second.description)
        assertNotSame(first.field, second.field)
        val position = first.description.cells.first().position
        val vector = Vector2(position.x.toFloat(), position.y.toFloat())
        assertNotSame(first.field.getTileAt(vector), second.field.getTileAt(vector))
        assertNotEquals(first.description, prepare(43L).description)
    }
}
