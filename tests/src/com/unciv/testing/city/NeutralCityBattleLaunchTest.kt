package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.Battle
import com.unciv.logic.battle.CityCombatant
import com.unciv.logic.battle.MapUnitCombatant
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет запуск нейтрального тактического боя через общий игровой метод Battle.attack. */
class NeutralCityBattleLaunchTest {
    /** Создаёт защищённую цель с заметными запасами, чтобы обнаружить лишние эффекты завоевания. */
    private fun city(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", 1) }
        health = 91
        population.foodStored = 17
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    /** Атака AI проходит тактический бой, очищает гарнизон и вводит победившего героя в полученный город. */
    @Test
    fun aiAttackRunsTacticalBattleAndAcquiresCity() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            fixture.civilization.playerType = PlayerType.AI
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 50) }
            val attacks = hero.attacksThisTurn
            val gold = fixture.civilization.gold

            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))

            assertSame(fixture.civilization, city.ownerOrNull)
            assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            assertTrue(city.garrisonInfo.getAllTroops().all { it == null })
            assertEquals(attacks + 1, hero.attacksThisTurn)
            assertEquals(0f, hero.currentMovement, 0f)
            assertSame(city.getCenterTile(), hero.currentTile)
            assertSame(hero, city.getCenterTile().militaryUnit)
            assertEquals(91, city.health)
            assertEquals(17, city.population.foodStored)
            assertEquals(gold, fixture.civilization.gold)
        }
    }

    /** Запуск без движения отклоняется без расхода атаки, повреждения гарнизона и смены владельца. */
    @Test
    fun rejectedAttackLeavesWorldAndGarrisonUnchanged() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            fixture.civilization.playerType = PlayerType.AI
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 50) }
            hero.currentMovement = 0f
            val defender = city.garrisonInfo.getTroopAt(0)!!
            val health = defender.currentHealth
            val attacks = hero.attacksThisTurn

            Battle.attack(MapUnitCombatant(hero), CityCombatant(city))

            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
            assertSame(defender, city.garrisonInfo.getTroopAt(0))
            assertEquals(1, defender.currentAmount)
            assertEquals(health, defender.currentHealth)
            assertEquals(attacks, hero.attacksThisTurn)
            assertSame(hero, hero.currentTile.militaryUnit)
        }
    }
}
