package com.unciv.battle.server

import com.badlogic.gdx.files.FileHandle
import com.unciv.logic.battle.session.BattleFieldPreparation
import com.unciv.logic.map.ClimateParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.pure.domain.battle.Point
import com.unciv.ui.battlescreen.BattleFieldViewMapFactory
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BattleFieldViewMapTest {
    private fun assets() = File(requireNotNull(System.getProperty("battle.assets")))
    private fun ruleset() = Ruleset().apply {
        load(FileHandle(File(assets(), "jsons/Civ V - Gods & Kings")))
    }

    @Test
    fun graphicalMapPreservesEveryServerTerrainValue() {
        val session = BattleScenarioCatalog(assets()).create("spearmen-vs-archers", 42L)
        val description = requireNotNull(session.read(BattleSide.ATTACKER).state.field)
        val map = BattleFieldViewMapFactory.create(description, ruleset())
        assertEquals(description.cells.size, map.values.size)
        val byPosition = description.cells.associateBy { it.position }
        map.values.forEach { tile ->
            val cell = byPosition.getValue(Point(tile.position.x.toInt(), tile.position.y.toInt()))
            assertEquals(cell.baseTerrain, tile.baseTerrain)
            assertEquals(cell.features, tile.terrainFeatures.toList())
            assertEquals(cell.temperature, tile.temperature)
            assertEquals(cell.humidity, tile.humidity)
            assertEquals(cell.impassable, tile.isImpassible())
        }
    }

    @Test
    fun hillsAndMountainsMatchTheCombatField() {
        val rules = ruleset()
        val prepared = BattleFieldPreparation.create(
            ClimateParameters(0.3, 0.5, 0.6), ClimateParameters(0.9, 0.5, 0.6)
        ) { requireNotNull(rules.terrains[it]).impassable }
        val map = BattleFieldViewMapFactory.create(prepared.description, rules)
        assertTrue(prepared.description.cells.any { "Hill" in it.features })
        assertTrue(prepared.description.cells.any { it.baseTerrain == "Mountain" })
        map.values.forEach { tile ->
            assertEquals(requireNotNull(prepared.field.getTileAt(tile.position)).isImpassible(), tile.isImpassible())
        }
    }

    @Test
    fun inconsistentCoordinatesAndPassabilityAreRejected() {
        val rules = ruleset()
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        val description = BattleFieldPreparation.create(climate, climate) {
            requireNotNull(rules.terrains[it]).impassable
        }.description
        val first = description.cells.first()
        val rest = description.cells.drop(1)
        listOf(
            description.copy(cells = listOf(first.copy(position = Point(999, 999))) + rest),
            description.copy(cells = listOf(first.copy(impassable = !first.impassable)) + rest)
        ).forEach { invalid ->
            try {
                BattleFieldViewMapFactory.create(invalid, rules)
                fail("Expected incompatible field to be rejected")
            } catch (_: IllegalArgumentException) { }
        }
    }
}
