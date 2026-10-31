package com.unciv.testing.city

import com.unciv.logic.map.RoadStatus
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.Assert.*
import org.junit.Test

/** Проверяет согласованность городской территории, отдельного владельца дороги и центра города. */
class CityTerritoryOwnershipTest {
    /** Переданная с городом дорога остаётся у получателя после освобождения клетки из территории. */
    @Test
    fun transferredRoadBelongsToRecipientAfterTileIsRelinquished() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val oldOwner = fixture.civilization
            val next = FakeCivilizationInfo().apply {
                civName = "Greece"
                nation = fixture.ruleset.nations.getValue(civName)
                gameInfo = fixture.game
                tech.techsResearched.addAll(oldOwner.tech.techsResearched)
            }
            fixture.game.civilizations.add(next)
            next.setTransients()
            oldOwner.isCurrentPlayerOverride = false
            oldOwner.makeCivilizationsMeet(next)
            val road = city.getTiles().first { it.position != city.location }
            road.addRoad(RoadStatus.Road, oldOwner)

            city.moveToCiv(next)

            assertSame(city, road.getCity())
            assertSame(next, road.getOwner())
            assertEquals(next.civName, road.roadOwner)
            assertSame(next, road.getRoadOwner())
            assertFalse(road.position in oldOwner.neutralRoads)
            assertFalse(road.position in next.neutralRoads)

            city.expansion.relinquishOwnership(road)

            // Клетка больше не принадлежит городу, но существующая дорога сохраняет своего владельца.
            assertNull(road.getCity())
            assertNull(road.getOwner())
            assertSame(next, road.getRoadOwner())
            assertTrue(road.position in next.neutralRoads)
            assertFalse(road.position in oldOwner.neutralRoads)
            road.removeRoad()
            assertFalse(road.position in next.neutralRoads)
        }
    }

    /** Повторное присвоение занятого центра запрещено и не повреждает территорию или регистрацию. */
    @Test
    fun occupiedCenterCannotBeClaimedAgain() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val center = city.getCenterTile()
            val territory = city.tiles.toSet()
            var rejected = false
            try {
                city.expansion.takeOwnership(center)
            } catch (_: Exception) {
                rejected = true
            }
            assertTrue("An occupied city center must be rejected", rejected)
            assertEquals(territory, city.tiles)
            assertSame(city, center.getCity())
            assertSame(fixture.civilization, center.getOwner())
            assertTrue(center.isCityCenter())
            assertSame(city, fixture.game.cityRegistry.at(city.state.position))
        }
    }
}
