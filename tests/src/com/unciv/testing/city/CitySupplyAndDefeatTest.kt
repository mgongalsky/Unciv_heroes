package com.unciv.testing.city

import com.unciv.logic.city.CityFlags
import com.unciv.logic.map.MapUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CitySupplyAndDefeatTest {
    @Test
    fun startTurnSuppliesVisitingHeroFromStoredFoodWhenEnabled() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            city.setFlag(CityFlags.ResourceDemand, 20)
            val hero = MapUnit().apply {
                civInfo = fixture.civilization
                owner = civInfo.civName
                name = "Warrior"
                currentTile = city.getCenterTile()
                setTransients(fixture.ruleset)
                basicFoodCapacity = 15f
                this.hero.setFood(3f)
            }
            city.getCenterTile().militaryUnit = hero
            city.population.foodStored = 7

            city.startTurn()
            assertEquals(7, city.population.foodStored)
            assertEquals(3f, hero.hero.currentFood, 0f)

            city.autoFeedHero = true
            city.startTurn()
            assertEquals(0, city.population.foodStored)
            assertEquals(10f, hero.hero.currentFood, 0f)
        }
    }

    @Test
    fun defeatUsesUnitsUntilOriginalCapitalHistoryExistsThenUsesCities() {
        CityTestFixture().use { fixture ->
            val civ = fixture.civilization
            assertTrue(civ.isDefeated())
            val unit = MapUnit().apply {
                civInfo = civ
                owner = civ.civName
                name = "Warrior"
                currentTile = fixture.game.tileMap.values.first()
                setTransients(fixture.ruleset)
                assignOwner(civ)
            }
            assertFalse(civ.isDefeated())

            civ.hasEverOwnedOriginalCapital = true
            assertTrue(civ.isDefeated())
            fixture.foundCity()
            civ.removeUnit(unit)
            assertFalse(civ.isDefeated())

            civ.hasEverOwnedOriginalCapital = false
            assertTrue(civ.isDefeated())
            civ.hasEverOwnedOriginalCapital = null
            assertTrue(civ.isDefeated())
        }
    }
}
