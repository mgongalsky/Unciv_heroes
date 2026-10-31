package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.map.MapUnit
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет операцию занятия пустого города, которую вызывает действие интерфейса. */
class NeutralCityOccupyActionTest {
    /** Готовит соседнего героя и пустой город; восстанавливает ввод LibGDX после проверки. */
    private fun withTarget(check: (CityTestFixture, CityInfo, MapUnit) -> Unit) {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                fixture.foundCity()
                val city = CityInfo().apply {
                    state.ownership = CityOwnership.Neutral
                    location = Vector2(4f, 0f)
                    tiles.add(location)
                    garrisonInfo = ArmyInfo(null, 2)
                    bindNeutralWorld(fixture.game)
                    fixture.game.cityRegistry.register(this)
                    setTransients()
                }
                val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
                check(fixture, city, hero)
            }
        } finally {
            Gdx.input = previousInput
        }
    }

    /** Занятие действительно перемещает героя в центр и тратит движение, сохраняя число атак. */
    @Test
    fun occupyEntersCenterUsingMovementWithoutSpendingAnAttack() {
        withTarget { fixture, city, hero ->
            val movement = hero.currentMovement
            val attacks = hero.attacksThisTurn
            assertTrue(NeutralCityEntry.canEnterThisTurn(city, hero))
            assertNull(city.ownerOrNull)
            assertTrue(NeutralCityEntry.enterThisTurn(city, hero))
            assertSame(city.getCenterTile(), hero.currentTile)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertTrue(hero.currentMovement < movement)
            assertEquals(attacks, hero.attacksThisTurn)
        }
    }

    /** Без движения операция отказывает и не переносит героя или владение городом. */
    @Test
    fun noMovementPreventsOccupation() {
        withTarget { _, city, hero ->
            hero.currentMovement = 0f
            assertFalse(NeutralCityEntry.canEnterThisTurn(city, hero))
            assertFalse(NeutralCityEntry.enterThisTurn(city, hero))
            assertNull(city.ownerOrNull)
            assertNotSame(city.getCenterTile(), hero.currentTile)
        }
    }

    /** Условия проверяются снова при выполнении: появившийся после проверки гарнизон блокирует занятие. */
    @Test
    fun newlyAddedGarrisonPreventsPreviouslyAvailableOccupation() {
        withTarget { _, city, hero ->
            assertTrue(NeutralCityEntry.canEnterThisTurn(city, hero))
            city.garrisonInfo.addUnits("Spearman", 1)
            assertFalse(NeutralCityEntry.enterThisTurn(city, hero))
            assertNull(city.ownerOrNull)
            assertNotSame(city.getCenterTile(), hero.currentTile)
            assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.currentAmount)
        }
    }
}
