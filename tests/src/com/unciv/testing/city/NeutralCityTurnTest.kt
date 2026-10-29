package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityTurnTest {
    private class CountingNeutralCity : CityInfo() {
        var rounds = 0
        override fun completeNeutralRound() {
            super.completeNeutralRound()
            rounds++
        }
    }

    @Test
    fun neutralPhaseRunsOncePerRoundAndPreservesLocalState() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                fixture.foundCity()
                val game = fixture.game.clone()
                game.turns = 1
                game.setTransients()
                val city = CountingNeutralCity().apply {
                    state.ownership = CityOwnership.Neutral
                    location = Vector2(4f, 0f)
                    tiles.add(location)
                    health = 91
                    population.foodStored = 17
                    currConscription = 13
                    expansion.cultureStored = 27
                    attackedThisTurn = true
                    autoFeedHero = true
                    setFlag(CityFlags.Resistance, 4)
                    cityConstructions.constructionQueue.add("Scout")
                    cityConstructions.inProgressConstructions["Scout"] = 23
                    garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Archer", 7) }
                    bindNeutralWorld(game)
                }
                game.cityRegistry.register(city)
                city.setTransients()
                val territory = city.tiles.map { it.cpy() }.toSet()
                val pressures = city.religion.getPressures()
                val population = city.population.population
                val player = game.currentPlayerCiv
                val created = player.citiesCreated
                repeat(3) { index ->
                    game.nextTurn()
                    assertEquals(index + 2, game.turns)
                    assertEquals(index + 1, city.rounds)
                    assertEquals(91, city.health)
                    assertEquals(population, city.population.population)
                    assertEquals(17, city.population.foodStored)
                    assertEquals(13, city.currConscription)
                    assertEquals(27, city.expansion.cultureStored)
                    assertTrue(city.attackedThisTurn)
                    assertEquals(4, city.getFlag(CityFlags.Resistance))
                    assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
                    assertEquals(mapOf("Scout" to 23), city.cityConstructions.inProgressConstructions)
                    assertEquals(territory, city.tiles)
                    assertEquals(pressures, city.religion.getPressures())
                    assertEquals(listOf(7, null), city.garrisonInfo.getAllTroops().map { it?.amount })
                    assertNull(city.garrisonInfo.ownerOrNull)
                    assertSame(city, game.tileMap[city.location].getCity())
                    assertNull(game.tileMap[city.location].getOwner())
                    assertEquals(created, player.citiesCreated)
                    assertFalse(city in player.cities)
                    assertFalse(city in player.citiesConnectedToCapitalToMediums)
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
