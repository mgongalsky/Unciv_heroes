package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.NeutralCityOwnership
import com.unciv.logic.civilization.CivilizationInfo
import org.junit.Assert.*
import org.junit.Test

class NeutralCityOwnershipSaveTest {
    @Test
    fun ownershipTransitionsSurviveBothSaveFormatsAndNeutralTurns() {
        CityTestFixture().use { fixture ->
            val previous = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                fixture.foundCity()
                val original = fixture.foundCity(Vector2(4f, 0f)).apply {
                    garrisonInfo = ArmyInfo(fixture.civilization, 2)
                    health = 91
                    population.foodStored = 17
                    cityConstructions.constructionQueue.clear()
                    cityConstructions.constructionQueue.add("Scout")
                    cityConstructions.inProgressConstructions["Scout"] = 23
                }
                val id = original.state.id
                val buildings = original.cityConstructions.builtBuildings.toSet()
                fixture.game.turns = 1
                for (compressed in listOf(false, true)) {
                    fun reload(game: GameInfo) = UncivFiles.gameInfoFromString(
                        UncivFiles.gameInfoToString(game.clone(), compressed)
                    )
                    var game = reload(fixture.game)
                    var city = game.cityRegistry.byId(id)!!
                    NeutralCityOwnership.neutralize(city)
                    game = reload(game)
                    city = game.cityRegistry.byId(id)!!
                    assertNull(city.ownerOrNull)
                    assertNull(city.garrisonInfo.ownerOrNull)
                    val owner = game.civilizations.single()
                    NeutralCityOwnership.acquireUndefended(city, owner)
                    game = reload(game)
                    city = game.cityRegistry.byId(id)!!
                    assertSame(game.civilizations.single(), city.ownerOrNull)
                    assertSame(city.ownerOrNull, city.garrisonInfo.ownerOrNull)
                    NeutralCityOwnership.neutralize(city)
                    game = reload(game)
                    game.nextTurn()
                    city = game.cityRegistry.byId(id)!!
                    assertNull(city.ownerOrNull)
                    assertNull(city.garrisonInfo.ownerOrNull)
                    assertEquals(original.id, city.id)
                    assertEquals(buildings, city.cityConstructions.builtBuildings.toSet())
                    assertEquals(91, city.health)
                    assertEquals(17, city.population.foodStored)
                    assertEquals(listOf("Scout"), city.cityConstructions.constructionQueue)
                    assertEquals(23, city.cityConstructions.inProgressConstructions["Scout"])
                    assertEquals(original.tiles, city.tiles)
                    assertSame(city, game.tileMap[city.location].getCity())
                    assertNull(game.tileMap[city.location].getOwner())
                    assertEquals(2, game.turns)
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previous
            }
        }
    }
}
