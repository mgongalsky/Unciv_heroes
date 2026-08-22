package com.unciv.testing.pure.application.army

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import com.unciv.logic.battle.performTurn

class ArcherAttackTest {

    private lateinit var fakeRuleset: Ruleset

    @Before
    fun setUp() {
        fakeRuleset = Ruleset().apply {
            val meleeType = UnitType().apply { name = "Melee" }
            val archeryType = UnitType().apply { name = "Archery" }
            unitTypes["Melee"] = meleeType
            unitTypes["Archery"] = archeryType
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                health = 100
                damage = 10
                speed = 5
                rangedStrength = 0
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"
                unitType = "Archery"
                health = 80
                damage = 8
                speed = 4
                rangedStrength = 10
            }
        }
        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }
    }

    @After
    fun tearDown() = stopKoin()

    private fun makeManager(
        attackerArmy: ArmyInfo,
        defenderArmy: ArmyInfo
    ): TestableBattleManager {
        val random = FakeBattleRandom(listOf(1.0, 1.0, 1.0, 1.0, 1.0))
        return TestableBattleManager(attackerArmy, defenderArmy, FakeBattleField(), random)
    }

    @Test
    fun `archer has rangedStrength after army creation`() {
        val army = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Archer", 10) }
        val troop = army.getAllTroops().filterNotNull().first()
        assertEquals(10, troop.rangedStrength)
        assertTrue(troop.isRanged)
    }

    @Test
    fun `canShoot returns true for archer`() {
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Archer", 10) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val manager = makeManager(attackerArmy, defenderArmy)
        manager.initializeTurnQueue()

        val archer = attackerArmy.getAllTroops().filterNotNull().first()
        assertTrue(manager.canShoot(archer))
    }

    @Test
    fun `SHOOT reduces defender currentHealth`() {
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Archer", 10) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val manager = makeManager(attackerArmy, defenderArmy)

        val archer = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()

        val archerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(5f, 0f))
        defenderTile.receiveTroop(defender)

        manager.placeTroop(archer, archerTile)
        manager.placeTroop(defender, defenderTile)
        manager.initializeTurnQueue()

        val healthBefore = defender.currentHealth
        println("Health before: $healthBefore")

        val request = BattleActionRequest(
            troop = archer,
            targetPosition = defenderTile,
            actionType = ActionType.SHOOT
        )
        val result = manager.performTurn(request)

        println("Health after: ${defender.currentHealth}")
        assertTrue(result.success)
        assertTrue(defender.currentHealth < healthBefore)
    }

    @Test
    fun `SHOOT with heavy damage reduces defender currentAmount`() {
        // 100 лучников — гарантированно убьём хотя бы одного юнита
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Archer", 100) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val manager = makeManager(attackerArmy, defenderArmy)

        val archer = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()

        val archerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(5f, 0f))
        defenderTile.receiveTroop(defender)

        manager.placeTroop(archer, archerTile)
        manager.placeTroop(defender, defenderTile)
        manager.initializeTurnQueue()

        val amountBefore = defender.currentAmount
        println("Amount before: $amountBefore")

        val request = BattleActionRequest(
            troop = archer,
            targetPosition = defenderTile,
            actionType = ActionType.SHOOT
        )
        val result = manager.performTurn(request)

        println("Amount after: ${defender.currentAmount}")
        assertTrue(result.success)
        assertTrue(defender.currentAmount < amountBefore)
    }

    @Test
    fun `SHOOT fails for melee unit`() {
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val manager = makeManager(attackerArmy, defenderArmy)

        val spearman = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()

        val spearmanTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(5f, 0f))
        defenderTile.receiveTroop(defender)

        manager.placeTroop(spearman, spearmanTile)
        manager.placeTroop(defender, defenderTile)
        manager.initializeTurnQueue()

        val request = BattleActionRequest(
            troop = spearman,
            targetPosition = defenderTile,
            actionType = ActionType.SHOOT
        )
        val result = manager.performTurn(request)

        println("SHOOT melee result: success=${result.success} errorId=${result.errorId}")
        assertTrue(!result.success)
    }

    @Test
    fun `ranged AI targets enemy tile not own tile`() {
        val attackerArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Archer", 10) }
        val defenderArmy = ArmyInfo(FakeCivilizationInfo(), 7).apply { addUnits("Spearman", 10) }
        val manager = makeManager(attackerArmy, defenderArmy)

        val archer = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()

        val archerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(5f, 0f))
        defenderTile.receiveTroop(defender)

        manager.placeTroop(archer, archerTile)
        manager.placeTroop(defender, defenderTile)
        manager.initializeTurnQueue()

        val ai = AIBattle(manager)
        ai.performTurn(archer)

        // Проверяем побочный эффект — защитник получил урон
        assertTrue(defender.currentHealth < defender.maxHealth || defender.currentAmount < 10)
    }
}
