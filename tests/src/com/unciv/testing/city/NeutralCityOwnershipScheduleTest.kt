package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityOwnershipScheduleTest {
    private class CountingCity : CityInfo() {
        var neutralRounds = 0
        override fun completeNeutralRound() {
            super.completeNeutralRound()
            neutralRounds++
        }
    }

    @Test
    fun ownershipChangesDoNotAddNeutralTurnsInsideOrAfterRoundBoundary() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                fixture.foundCity()
                val game = fixture.game.clone()
                game.turns = 1
                game.civilizations.add(CivilizationInfo("Greece").apply {
                    playerType = PlayerType.Human
                    tech.techsResearched.addAll(game.civilizations.first().tech.techsResearched)
                })
                game.setTransients()
                val rome = game.civilizations.first()
                val city = CountingCity().apply {
                    state.ownership = CityOwnership.Neutral
                    state.garrison = Army(2)
                    location = Vector2(4f, 0f)
                    tiles.add(location)
                    bindNeutralWorld(game)
                }
                game.cityRegistry.register(city)
                city.setTransients()

                NeutralCityOwnership.acquireUndefended(city, rome)
                assertEquals(0, city.neutralRounds)
                game.nextTurn()
                assertEquals("Greece", game.currentPlayer)
                assertEquals(1, game.turns)
                assertEquals(0, city.neutralRounds)

                NeutralCityOwnership.neutralize(city)
                assertEquals(0, city.neutralRounds)
                game.nextTurn()
                assertEquals("Rome", game.currentPlayer)
                assertEquals(2, game.turns)
                assertEquals(1, city.neutralRounds)

                NeutralCityOwnership.acquireUndefended(city, rome)
                NeutralCityOwnership.neutralize(city)
                assertEquals(2, game.turns)
                assertEquals(1, city.neutralRounds)
                game.nextTurn()
                assertEquals("Greece", game.currentPlayer)
                assertEquals(1, city.neutralRounds)
                game.nextTurn()
                assertEquals(3, game.turns)
                assertEquals(2, city.neutralRounds)
                assertNull(city.ownerOrNull)
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
