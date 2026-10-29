package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityRoundScheduleTest {
    private class CountingCity : CityInfo() {
        var rounds = 0
        override fun completeNeutralRound() {
            super.completeNeutralRound()
            rounds++
        }
    }

    @Test
    fun playerSwitchDoesNotRunNeutralPhaseUntilRoundWraps() {
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
                val city = CountingCity().apply {
                    state.ownership = CityOwnership.Neutral
                    state.garrison = Army(2)
                    location = Vector2(4f, 0f)
                    tiles.add(location)
                    bindNeutralWorld(game)
                }
                game.cityRegistry.register(city)
                city.setTransients()
                repeat(2) { round ->
                    assertEquals("Rome", game.currentPlayer)
                    game.nextTurn()
                    assertEquals("Greece", game.currentPlayer)
                    assertEquals(round + 1, game.turns)
                    assertEquals(round, city.rounds)
                    game.nextTurn()
                    assertEquals("Rome", game.currentPlayer)
                    assertEquals(round + 2, game.turns)
                    assertEquals(round + 1, city.rounds)
                    assertNull(city.ownerOrNull)
                    assertFalse(city in game.civilizations[0].cities)
                    assertFalse(city in game.civilizations[1].cities)
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
