package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityFlags
import com.unciv.logic.map.RoadStatus
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CityOwnershipCharacterizationTest {
    private fun recipient(fixture: CityTestFixture): FakeCivilizationInfo {
        val recipient = FakeCivilizationInfo().apply {
            civName = "Greece"
            nation = fixture.ruleset.nations.getValue(civName)
            gameInfo = fixture.game
            tech.techsResearched.addAll(fixture.civilization.tech.techsResearched)
        }
        fixture.game.civilizations.add(recipient)
        recipient.setTransients()
        fixture.civilization.isCurrentPlayerOverride = false
        fixture.civilization.makeCivilizationsMeet(recipient)
        return recipient
    }

    @Test
    fun peacefulTransferPreservesPopulationAndHealthWithoutConquestRewards() {
        characterizeTransfer(capture = false)
    }

    @Test
    fun conquestReducesPopulationAndAddsResistanceAndPlunder() {
        characterizeTransfer(capture = true)
    }

    private fun characterizeTransfer(capture: Boolean) {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            city.population.setPopulation(5)
            city.health = 37
            city.setFlag(CityFlags.WeLoveTheKing, 8)
            city.demandedResource = "Gold"
            city.isBeingRazed = true
            city.cityConstructions.addBuilding("Granary")
            val next = recipient(fixture)
            fixture.game.turns = 60
            val originalId = city.id
            val originalPosition = city.location.cpy()
            val originalGarrison = city.garrisonInfo

            if (capture) city.puppetCity(next) else city.moveToCiv(next)

            assertEquals(originalId, city.id)
            assertEquals(originalPosition, city.location)
            assertSame(next, city.civInfo)
            assertTrue(fixture.civilization.cities.isEmpty())
            assertEquals(listOf(city), next.cities)
            assertEquals("Rome", city.foundingCiv)
            assertEquals("Rome", city.previousOwner)
            assertEquals(60, city.turnAcquired)
            assertTrue(city.isCapital())
            assertTrue(city.isOriginalCapital)
            assertFalse(city.isBeingRazed)
            assertFalse(city.isWeLoveTheKingDayActive())
            assertEquals("", city.demandedResource)
            assertEquals(listOf("Granary", "Palace"), city.cityConstructions.builtBuildings.sorted())
            assertEquals(RoadStatus.None, city.getCenterTile().roadStatus)
            assertEquals(setOf(
                Vector2(-1f, 0f), Vector2(0f, -1f), Vector2(0f, 0f),
                Vector2(0f, 1f), Vector2(1f, 0f), Vector2(1f, 1f)
            ), city.tiles)
            for (position in city.tiles) {
                val tile = fixture.game.tileMap[position]
                assertSame(city, tile.owningCity)
                assertSame(next, tile.getOwner())
            }

            // Garrison ownership follows the city; legacy defeat conditions remain unchanged.
            assertSame(originalGarrison, city.garrisonInfo)
            assertSame(next, city.garrisonInfo.civInfo)
            assertTrue(fixture.civilization.isDefeated())
            assertNull(next.hasEverOwnedOriginalCapital)
            assertTrue(next.isDefeated())

            assertEquals(capture, city.isPuppet)
            assertEquals(if (capture) 3 else 5, city.population.population)
            assertEquals(if (capture) 100 else 37, city.health)
            assertEquals(capture, city.isInResistance())
            if (capture) {
                assertEquals(3, city.getFlag(CityFlags.Resistance))
                assertEquals(109, next.gold)
                assertEquals(listOf("Nothing"), city.cityConstructions.constructionQueue)
                assertEquals(listOf(
                    "Received [109] Gold for capturing [Rome]",
                    "The civilization of [Rome] has been destroyed!",
                    "Work has started on [Nothing]"
                ), next.capturedNotifications)
            } else {
                assertEquals(0, next.gold)
                assertTrue(city.cityConstructions.constructionQueue.isEmpty())
                assertTrue(next.capturedNotifications.isEmpty())
            }
        }
    }
}
