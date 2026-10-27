package com.unciv.testing.city

import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CityRecaptureCharacterizationTest {
    private fun addCivilization(fixture: CityTestFixture, name: String): FakeCivilizationInfo {
        val civ = FakeCivilizationInfo().apply {
            civName = name
            nation = fixture.ruleset.nations.getValue(name)
            gameInfo = fixture.game
            tech.techsResearched.addAll(fixture.civilization.tech.techsResearched)
        }
        fixture.game.civilizations.add(civ)
        civ.setTransients()
        return civ
    }

    private fun withCapturedCity(block: (CityTestFixture, CityInfo, FakeCivilizationInfo, FakeCivilizationInfo) -> Unit) {
        CityTestFixture().use { fixture ->
            fixture.civilization.isCurrentPlayerOverride = false
            val city = fixture.foundCity()
            city.population.setPopulation(8)
            val greece = addCivilization(fixture, "Greece")
            val egypt = addCivilization(fixture, "Egypt")
            fixture.civilization.makeCivilizationsMeet(greece)
            fixture.civilization.makeCivilizationsMeet(egypt)
            greece.makeCivilizationsMeet(egypt)
            fixture.game.turns = 60
            city.puppetCity(greece)
            assertSame(greece, city.civInfo)
            assertEquals("Rome", city.previousOwner)
            assertEquals(5, city.population.population)
            assertEquals(5, city.getFlag(CityFlags.Resistance))
            assertTrue(city.isPuppet)
            block(fixture, city, greece, egypt)
        }
    }

    @Test
    fun recaptureDuringResistanceRemovesResistanceButStillLosesPopulation() {
        withCapturedCity { _, city, greece, egypt ->
            val id = city.id
            city.puppetCity(egypt)
            assertSame(egypt, city.civInfo)
            assertEquals("Greece", city.previousOwner)
            assertEquals(3, city.population.population)
            assertEquals(3, city.getFlag(CityFlags.Resistance))

            city.puppetCity(greece)

            assertEquals(id, city.id)
            assertSame(greece, city.civInfo)
            assertEquals("Egypt", city.previousOwner)
            assertEquals(2, city.population.population)
            assertFalse(city.isInResistance())
            assertTrue(city.isPuppet)
            assertEquals(100, city.health)
            assertTrue(city.isCapital())
            assertEquals(listOf(city), greece.cities)
            assertTrue(egypt.cities.isEmpty())

            city.cityConstructions.inProgressConstructions["Warrior"] = 17
            city.annexCity()

            assertFalse(city.isPuppet)
            assertTrue(city.cityConstructions.inProgressConstructions.isEmpty())
            assertEquals(2, city.population.population)
            assertFalse(city.isInResistance())
        }
    }

    @Test
    fun liberationReturnsCityToFounderWithoutResistanceOrPuppetStatus() {
        withCapturedCity { fixture, city, greece, egypt ->
            val id = city.id
            city.liberateCity(egypt)

            assertEquals(id, city.id)
            assertSame(fixture.civilization, city.civInfo)
            assertEquals("Greece", city.previousOwner)
            assertEquals(3, city.population.population)
            assertEquals(100, city.health)
            assertFalse(city.isInResistance())
            assertFalse(city.isPuppet)
            assertTrue(city.isCapital())
            assertEquals(listOf(city), fixture.civilization.cities)
            assertTrue(greece.cities.isEmpty())
            assertTrue(egypt.cities.isEmpty())
        }
    }
}
