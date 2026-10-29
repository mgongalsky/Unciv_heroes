package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityExpansionTest {
    @Test
    fun neutralTerritoryCannotBeChosenBoughtOrClaimedByAnotherCity() {
        CityTestFixture().use { fixture ->
            val playerCity = fixture.foundCity()
            val neutral = CityInfo().apply {
                state.ownership = CityOwnership.Neutral
                location = Vector2(4f, 0f)
                tiles.add(location)
                tiles.add(Vector2(3f, 0f))
                garrisonInfo = ArmyInfo(null, 2)
                bindNeutralWorld(fixture.game)
                fixture.game.cityRegistry.register(this)
                setTransients()
            }
            val territory = neutral.getTiles().toList()
            val choices = playerCity.expansion.getChoosableTiles().toList()
            assertTrue(territory.none { it in choices })
            assertTrue(choices.isNotEmpty())
            val gold = fixture.civilization.gold
            val playerTiles = playerCity.tiles.toSet()
            val neutralTiles = neutral.tiles.toSet()

            for (tile in territory) {
                for (buy in listOf(false, true)) {
                    try {
                        if (buy) playerCity.expansion.buyTile(tile)
                        else playerCity.expansion.takeOwnership(tile)
                        fail("Neutral territory must require a city ownership operation")
                    } catch (_: IllegalArgumentException) {
                        assertEquals(gold, fixture.civilization.gold)
                        assertEquals(playerTiles, playerCity.tiles.toSet())
                        assertEquals(neutralTiles, neutral.tiles.toSet())
                        assertSame(neutral, tile.getCity())
                        assertNull(tile.getOwner())
                    }
                }
            }
        }
    }
}
