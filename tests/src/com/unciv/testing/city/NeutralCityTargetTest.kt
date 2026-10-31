package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.automation.unit.BattleHelper
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет обнаружение нейтрального гарнизона как боевой цели средствами общей автоматизации. */
class NeutralCityTargetTest {
    /** Создаёт зарегистрированный нейтральный город с одним живым защитником. */
    private fun city(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", 1) }
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    /** Цель доступна только с соседних клеток; сам поиск не начинает бой и не меняет город. */
    @Test
    fun neutralTargetUsesOnlyAdjacentApproachesWithoutChangingOwnership() {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                fixture.foundCity()
                val city = city(fixture)
                val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
                hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 10) }
                val center = city.getCenterTile()
                assertTrue(BattleHelper.containsAttackableEnemy(center, MapUnitCombatant(hero)))
                val targets = BattleHelper.getAttackableEnemies(
                    hero, hero.movement.getDistanceToTiles(), tilesToCheck = listOf(center)
                ).filter { it.tileToAttack == center }
                assertTrue(targets.isNotEmpty())
                assertTrue(targets.all { it.tileToAttackFrom.aerialDistanceTo(center) == 1 })
                assertTrue(targets.any { it.tileToAttackFrom == hero.currentTile })
                assertNull(city.ownerOrNull)
                assertNull(city.garrisonInfo.ownerOrNull)
                assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.currentAmount)
            }
        } finally {
            Gdx.input = previousInput
        }
    }

    /** Герой без живых войск не получает нейтральную боевую цель ни в одной из проверяемых точек входа. */
    @Test
    fun heroWithoutLivingArmyHasNoNeutralBattleTarget() {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                fixture.foundCity()
                val city = city(fixture)
                val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
                hero.army = ArmyInfo(fixture.civilization, 2)
                assertFalse(BattleHelper.containsAttackableEnemy(city.getCenterTile(), MapUnitCombatant(hero)))
                assertTrue(BattleHelper.getAttackableEnemies(
                    hero, hero.movement.getDistanceToTiles(), tilesToCheck = listOf(city.getCenterTile())
                ).none { it.tileToAttack == city.getCenterTile() })
                assertNull(city.ownerOrNull)
            }
        } finally {
            Gdx.input = previousInput
        }
    }
}
