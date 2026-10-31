package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.map.MapUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Защищает перемещение юнитов, для которых клетка становится недопустимой после разрушения города. */
class CityDestructionDisplacementTest {
    /** Корабль из центра прибрежного города переносится на воду и остаётся в списках клетки и владельца. */
    @Test
    fun destroyingCoastalCityMovesItsShipToWater() {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                val center = fixture.game.tileMap[Vector2(0f, 0f)]
                // Соседнее море обеспечивает допустимое место для вытесненного корабля.
                val coast = center.neighbors.first()
                coast.baseTerrain = "Coast"
                coast.setTerrainTransients()
                val city = fixture.foundCity()
                val definition = fixture.ruleset.units.values.first { it.isWaterUnit() }
                val ship = MapUnit().apply {
                    civInfo = fixture.civilization
                    owner = civInfo.civName
                    name = definition.name
                    currentTile = center
                    setTransients(fixture.ruleset)
                    assignOwner(fixture.civilization)
                    putInTile(center)
                }
                assertSame(center, ship.currentTile)
                assertTrue(center.getUnits().any { it === ship })

                city.destroyCity(overrideSafeties = true)

                assertFalse(ship.isDestroyed)
                assertNotSame(center, ship.currentTile)
                assertTrue(ship.currentTile.isWater)
                assertTrue(ship.movement.canPassThrough(ship.currentTile))
                assertFalse(center.getUnits().any { it === ship })
                assertTrue(ship.currentTile.getUnits().any { it === ship })
                assertTrue(fixture.civilization.getCivUnits().any { it === ship })
            }
        } finally {
            Gdx.input = previousInput
        }
    }
}
