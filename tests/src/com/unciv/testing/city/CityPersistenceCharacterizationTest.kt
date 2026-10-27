package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.logic.map.MapUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class CityPersistenceCharacterizationTest {
    @Test
    fun cloneCopiesTerritoryAndActiveGarrisonAndPreservesPreviousOwner() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            city.previousOwner = "Greece"
            city.population.foodStored = 3
            city.currConscription = 17
            city.setFlag(CityFlags.Resistance, 4)
            val copy = city.clone()

            assertNotSame(city, copy)
            assertEquals(city.id, copy.id)
            assertEquals("Greece", copy.previousOwner)
            assertEquals(city.location, copy.location)
            assertNotSame(city.tiles, copy.tiles)
            assertNotSame(city.workedTiles, copy.workedTiles)
            assertNotSame(city.lockedTiles, copy.lockedTiles)
            assertSame(city.garrison, copy.garrison)
            assertNotSame(city.garrisonInfo, copy.garrisonInfo)
            assertNotSame(city.population, copy.population)
            assertNotSame(city.cityConstructions, copy.cityConstructions)
            assertEquals(17, copy.currConscription)
            copy.population.foodStored = 8
            copy.setFlag(CityFlags.Resistance, 2)
            assertEquals(3, city.population.foodStored)
            assertEquals(4, city.getFlag(CityFlags.Resistance))
        }
    }

    @Test
    fun cityJsonPreservesLocalStateAndBothGarrisonsButUsesPlaceholderArmyOwner() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            city.previousOwner = "Greece"
            city.population.foodStored = 3
            city.currConscription = 17
            city.setFlag(CityFlags.Resistance, 4)
            val copy = json().fromJson(CityInfo::class.java, json().toJson(city))

            assertEquals(city.id, copy.id)
            assertEquals("Greece", copy.previousOwner)
            assertEquals(3, copy.population.foodStored)
            assertEquals(17, copy.currConscription)
            assertEquals(4, copy.getFlag(CityFlags.Resistance))
            assertEquals(city.tiles, copy.tiles)
            assertNotSame(city.tiles, copy.tiles)
            assertEquals(listOf("Archer:30", "Spearman:20", "Horseman:10", "Swordsman:15"),
                copy.garrison.map { "${it.unitName}:${it.amount}" })
            assertEquals(listOf("Archer:35", "Peasant:20", null, null),
                copy.garrisonInfo.getAllTroops().map { it?.let { "${it.unitName}:${it.amount}" } })
            assertNotSame(city.garrisonInfo, copy.garrisonInfo)
            assertNotSame(city.garrisonInfo.getTroopAt(0), copy.garrisonInfo.getTroopAt(0))
            assertSame(MapUnit.monsterCivInfo, copy.garrisonInfo.civInfo)
            assertNotSame(fixture.civilization, copy.garrisonInfo.civInfo)
        }
    }
}
