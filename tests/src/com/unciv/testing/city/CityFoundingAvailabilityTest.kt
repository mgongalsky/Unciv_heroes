package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.ui.worldscreen.unit.UnitActions
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource

class CityFoundingAvailabilityTest {
    @get:Rule
    val inputEnvironment = object : ExternalResource() {
        private var previous: Input? = null
        override fun before() {
            previous = Gdx.input
            Gdx.input = MockInput()
        }
        override fun after() {
            Gdx.input = previous
        }
    }

    private fun founder(fixture: CityTestFixture, position: Vector2 = Vector2(0f, 0f)): MapUnit {
        val definition = fixture.ruleset.units.values.first { it.hasUnique(UniqueType.FoundCity) }
        return MapUnit().apply {
            civInfo = fixture.civilization
            owner = civInfo.civName
            name = definition.name
            currentTile = fixture.game.tileMap[position]
            setTransients(fixture.ruleset)
            currentMovement = 2f
        }
    }

    @Test
    fun humanRequiresSmallVillageButAiDoesNot() {
        CityTestFixture().use { fixture ->
            val unit = founder(fixture)
            val tile = unit.currentTile
            tile.improvement = null
            assertNull(UnitActions.getFoundCityAction(unit, tile))

            tile.improvement = "Small village"
            assertNotNull(UnitActions.getFoundCityAction(unit, tile)?.action)

            tile.improvement = null
            fixture.civilization.playerType = PlayerType.AI
            assertNotNull(UnitActions.getFoundCityAction(unit, tile)?.action)
        }
    }

    @Test
    fun noMovementOrNearbyCityLeavesADisabledAction() {
        CityTestFixture().use { fixture ->
            val unit = founder(fixture, Vector2(1f, 0f))
            val tile = unit.currentTile
            tile.improvement = "Small village"
            unit.currentMovement = 0f
            val exhausted = UnitActions.getFoundCityAction(unit, tile)
            assertNotNull(exhausted)
            assertNull(exhausted!!.action)

            unit.currentMovement = 2f
            fixture.foundCity()
            val nearby = UnitActions.getFoundCityAction(unit, tile)
            assertNotNull(nearby)
            assertNull(nearby!!.action)
        }
    }

    @Test
    fun oneCityChallengeHidesFoundingAfterOriginalCapitalWasOwned() {
        CityTestFixture().use { fixture ->
            val unit = founder(fixture)
            unit.currentTile.improvement = "Small village"
            fixture.game.gameParameters.oneCityChallenge = true
            fixture.civilization.hasEverOwnedOriginalCapital = false
            assertNotNull(UnitActions.getFoundCityAction(unit, unit.currentTile)?.action)

            fixture.civilization.hasEverOwnedOriginalCapital = true
            assertNull(UnitActions.getFoundCityAction(unit, unit.currentTile))
        }
    }
}
