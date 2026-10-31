package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Проверяет получение нейтрального города через настоящее перемещение юнита на мировой карте. */
class NeutralCityMovementTest {
    private var previousInput: Input? = null

    /** Даёт игровому коду ввод LibGDX без графического окна, сохраняя прежнюю ссылку. */
    @Before
    fun prepareInput() {
        previousInput = Gdx.input
        Gdx.input = MockInput()
    }

    /** Возвращает ввод после каждого сценария, в том числе после неудачного утверждения. */
    @After
    fun restoreInput() {
        Gdx.input = previousInput
    }

    /** Создаёт пустой нейтральный город и связывает его центр с реальной картой. */
    private fun neutralCity(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2)
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    /** Проверки доступности пути не меняют владельца; город переходит герою только после входа в центр. */
    @Test
    fun heroAcquiresCityOnlyWhenActuallyEnteringEmptyCenter() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = neutralCity(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            val center = city.getCenterTile()
            val gold = fixture.civilization.gold
            assertTrue(hero.movement.canPassThrough(center))
            assertTrue(hero.movement.canMoveTo(center))
            assertNull(city.ownerOrNull)
            hero.movement.moveToTile(center)
            assertSame(center, hero.currentTile)
            assertSame(hero, center.militaryUnit)
            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            assertSame(city, center.getCity())
            assertEquals(gold, fixture.civilization.gold)
        }
    }

    /** Живой гарнизон блокирует обычный вход; проверки движения не убивают защитников и не передают город. */
    @Test
    fun defendedCenterBlocksMovementWithoutChangingOwnershipOrGarrison() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = neutralCity(fixture)
            city.garrisonInfo.addUnits("Archer", 1)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            assertFalse(hero.movement.canPassThrough(city.getCenterTile()))
            assertFalse(hero.movement.canMoveTo(city.getCenterTile()))
            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
            assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.amount)
        }
    }
}
