package com.unciv.testing.city

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.mock.input.MockInput
import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.automation.unit.BattleHelper
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

class NeutralCityBattleAutomationTest {
    private fun verifyAutomation(attackerAmount: Int, defenderAmount: Int, shouldAttack: Boolean) {
        val previousInput = Gdx.input
        Gdx.input = MockInput()
        try {
            CityTestFixture().use { fixture ->
                fixture.foundCity()
                fixture.civilization.playerType = PlayerType.AI
                val city = CityInfo().apply {
                    state.ownership = CityOwnership.Neutral
                    location = Vector2(4f, 0f)
                    tiles.add(location)
                    garrisonInfo = ArmyInfo(null, 2).apply { addUnits("Spearman", defenderAmount) }
                    bindNeutralWorld(fixture.game)
                    fixture.game.cityRegistry.register(this)
                    setTransients()
                }
                val hero = fixture.civilization.placeUnitNearTile(Vector2(3f, 0f), "Warrior")!!
                hero.army = ArmyInfo(fixture.civilization, 2).apply { addUnits("Spearman", attackerAmount) }
                fixture.civilization.updateViewableTiles()
                val movement = hero.currentMovement
                val attacks = hero.attacksThisTurn

                val consumedTurn = BattleHelper.tryAttackNearbyEnemy(hero, stayOnTile = true)

                if (shouldAttack) {
                    assertTrue(consumedTurn)
                    assertSame(fixture.civilization, city.ownerOrNull)
                    assertEquals(attacks + 1, hero.attacksThisTurn)
                    assertEquals(0f, hero.currentMovement, 0f)
                } else {
                    assertFalse(consumedTurn)
                    assertNull(city.ownerOrNull)
                    assertEquals(defenderAmount, city.garrisonInfo.getTroopAt(0)!!.currentAmount)
                    assertEquals(attacks, hero.attacksThisTurn)
                    assertEquals(movement, hero.currentMovement, 0f)
                }
            }
        } finally {
            Gdx.input = previousInput
        }
    }

    @Test
    fun strongArmySelectsNeutralGarrisonAndRunsTacticalBattle() = verifyAutomation(50, 1, true)

    @Test
    fun weakArmyAvoidsStrongerNeutralGarrison() = verifyAutomation(1, 50, false)
}
