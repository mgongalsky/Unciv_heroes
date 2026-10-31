package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.diplomacy.DiplomacyFlags
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.ui.worldscreen.unit.UnitActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Защищает границу между запросом дипломатического подтверждения и фактическим основанием. */
class CityFoundingDiplomacyTest {
    /** До подтверждения обещания мир неизменен; после него выполняются основание и реакция соседа. */
    @Test
    fun brokenPromiseDefersAllFoundingEffectsUntilConfirmation() {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                val neighbor = FakeCivilizationInfo().apply {
                    civName = "Greece"
                    nation = fixture.ruleset.nations.getValue(civName)
                    gameInfo = fixture.game
                    tech.techsResearched.addAll(fixture.civilization.tech.techsResearched)
                }
                fixture.game.civilizations.add(neighbor)
                neighbor.setTransients()
                CityInfo(neighbor, Vector2(4f, 0f), MapUnit().apply {
                    civInfo = neighbor
                    owner = neighbor.civName
                    army.setTransients(neighbor, this)
                })
                // Обучение при первом контакте пишет настройки и не относится к этому сценарию.
                fixture.civilization.isCurrentPlayerOverride = false
                fixture.civilization.makeCivilizationsMeet(neighbor)
                fixture.civilization.isCurrentPlayerOverride = true
                neighbor.exploredTiles.add(Vector2(4f, 0f))
                neighbor.exploredTiles.add(Vector2(0f, 0f))
                val diplomacy = neighbor.getDiplomacyManager(fixture.civilization)
                diplomacy.setFlag(DiplomacyFlags.AgreedToNotSettleNearUs, 30)

                val tile = fixture.game.tileMap[Vector2(0f, 0f)]
                val definition = fixture.ruleset.units.values.first { it.hasUnique(UniqueType.FoundCity) }
                val unit = MapUnit().apply {
                    civInfo = fixture.civilization
                    owner = civInfo.civName
                    name = definition.name
                    currentTile = tile
                    setTransients(fixture.ruleset)
                    assignOwner(fixture.civilization)
                    putInTile(tile)
                    currentMovement = 2f
                }
                tile.improvement = "Small village"
                val events = mutableListOf<String>()
                var confirm: (() -> Unit)? = null
                val action = UnitActions.getFoundCityAction(
                    unit, tile,
                    onFoundingTutorial = { events.add("tutorial") },
                    onWorldUpdated = { events.add("world") },
                    confirmBrokenPromise = { leaders, continuation ->
                        assertEquals(neighbor.getLeaderDisplayName(), leaders)
                        events.add("confirmation")
                        // Сохраняем продолжение вместо открытия окна и немедленного согласия.
                        confirm = continuation
                    }
                )
                assertNotNull(action?.action)
                action!!.action!!.invoke()

                assertEquals(listOf("confirmation"), events)
                assertTrue(fixture.civilization.cities.isEmpty())
                assertFalse(unit.isDestroyed)
                assertEquals("Small village", tile.improvement)
                assertFalse(diplomacy.hasFlag(DiplomacyFlags.SettledCitiesNearUs))
                assertNotNull(confirm)

                // Только явное подтверждение разрешает все отложенные игровые эффекты.
                confirm!!.invoke()

                assertEquals(listOf("confirmation", "tutorial", "world"), events)
                assertEquals(1, fixture.civilization.cities.size)
                assertTrue(unit.isDestroyed)
                assertTrue(diplomacy.hasFlag(DiplomacyFlags.SettledCitiesNearUs))
            }
        } finally {
            Gdx.input = previousInput
        }
    }
}
