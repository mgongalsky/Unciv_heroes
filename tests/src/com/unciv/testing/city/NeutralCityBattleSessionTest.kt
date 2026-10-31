package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.NeutralCityBattleSession
import com.unciv.logic.city.CityInfo
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет общую тактическую сессию боя с нейтральным гарнизоном без графического экрана. */
class NeutralCityBattleSessionTest {
    /** Готовит город с одним защитником, чтобы занятие обязательно требовало боя. */
    private fun city(fixture: CityTestFixture) = CityInfo().apply {
        state.ownership = CityOwnership.Neutral
        location = Vector2(4f, 0f)
        tiles.add(location)
        garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", 1) }
        bindNeutralWorld(fixture.game)
        fixture.game.cityRegistry.register(this)
        setTransients()
    }

    /** Создание сессии не тратит атаку, а повторное завершение или закрытие учитывает её только один раз. */
    @Test
    fun completionAndClosingConsumeOnlyOneWorldAttack() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 10) }
            val attacks = hero.attacksThisTurn
            val memories = hero.attacksSinceTurnStart.size
            val session = NeutralCityBattleSession.create(hero, city)
            assertEquals(attacks, hero.attacksThisTurn)
            assertTrue(hero.currentMovement > 0f)

            session.consumeWorldAttack()
            session.consumeWorldAttack()

            assertEquals(attacks + 1, hero.attacksThisTurn)
            assertEquals(memories + 1, hero.attacksSinceTurnStart.size)
            assertEquals(city.getCenterTile().position, hero.attacksSinceTurnStart.last())
            assertEquals(0f, hero.currentMovement, 0f)
            assertNull(city.ownerOrNull)
            assertEquals(1, city.garrisonInfo.getTroopAt(0)!!.currentAmount)
        }
    }
    /** Автоматическая сессия завершается победой в заданном лимите, сохраняя гарнизон без владельца. */
    @Test
    fun automatedSessionUsesOwnerlessGarrisonAndTerminatesWithFixedSeed() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", 50) }
            assertTrue(NeutralCityBattleSession.canStart(hero, city))
            val session = NeutralCityBattleSession.create(hero, city, seed = 42L)
            val result = session.runAutomated(maxTurns = 200)
            assertEquals(42L, result.seed)
            assertEquals(BattleTermination.VICTORY, result.termination)
            assertEquals(true, result.winnerIsAttacker)
            assertTrue(result.turns <= 200)
            // Симуляция сама не применяет передачу города на мировой карте.
            assertNull(city.ownerOrNull)
            assertNull(city.garrisonInfo.ownerOrNull)
        }
    }

    /** Отсутствие движения запрещает создание сессии до изменения здоровья и численности защитников. */
    @Test
    fun heroWithoutMovementCannotStartBattleOrMutateArmies() {
        CityTestFixture().use { fixture ->
            fixture.foundCity()
            val city = city(fixture)
            val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
            hero.currentMovement = 0f
            val defender = city.garrisonInfo.getTroopAt(0)!!
            val health = defender.currentHealth
            assertFalse(NeutralCityBattleSession.canStart(hero, city))
            try {
                NeutralCityBattleSession.create(hero, city)
                fail("No movement means no new battle")
            } catch (_: IllegalArgumentException) {
                assertNull(city.ownerOrNull)
                assertEquals(health, defender.currentHealth)
                assertEquals(1, defender.currentAmount)
            }
        }
    }
}
