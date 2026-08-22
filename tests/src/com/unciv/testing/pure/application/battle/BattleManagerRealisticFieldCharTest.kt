package com.unciv.testing.pure.application.battle

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import com.unciv.logic.battle.performTurn

class BattleManagerRealisticFieldCharTest {

    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var grid: Array<Array<FakeBattleTile>>

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.0,
                moraleProbability = 0.0,
                armySize = 5
            )
        )

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 3
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"
                unitType = "Melee"
                damage = 8
                health = 80
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

        grid = FakeBattleFieldBuilder.buildGrid(14, 8)

        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 8) }
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    private fun tile(x: Int, y: Int) = FakeBattleFieldBuilder.tileAt(grid, x, y)

    private fun makeManager(): TestableBattleManager {
        val manager = TestableBattleManager(
            attackerArmy, defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 }),
            useRealMovement = true
        )
        manager.initializeTurnQueue()
        return manager
    }

    // --- MOVE ---

    @Test
    fun `MOVE to tile within speed range succeeds`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(2, 0),
            actionType = ActionType.MOVE
        ))

        assertTrue(result.success)
        assertEquals(tile(2, 0), result.movedTo)
    }

    @Test
    fun `MOVE to tile beyond speed range returns TOO_FAR`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(4, 0),  // distance=4, speed=3
            actionType = ActionType.MOVE
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.TOO_FAR, result.errorId)
    }

    @Test
    fun `MOVE to enemy-occupied tile returns HEX_OCCUPIED`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))
        manager.placeTroop(defender, tile(2, 0))

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(2, 0),
            actionType = ActionType.MOVE
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
        // TODO: MOVE на врага возвращает HEX_OCCUPIED — логика не различает врага и союзника при MOVE
    }

    // --- ATTACK ---

    @Test
    fun `ATTACK enemy on adjacent tile succeeds and reduces defender`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))
        manager.placeTroop(defender, tile(1, 0))

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(1, 0),
            attackTile = tile(0, 0),
            actionType = ActionType.ATTACK
        ))

        assertTrue(result.success)
        assertEquals(7, defender.currentAmount)
    }

    @Test
    fun `ATTACK enemy too far away returns INVALID_TARGET`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))
        manager.placeTroop(defender, tile(5, 0))

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(5, 0),
            attackTile = tile(4, 0),  // distance=4 > speed=3
            actionType = ActionType.ATTACK
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.INVALID_TARGET, result.errorId)
    }

    // --- SHOOT ---

    @Test
    fun `SHOOT ranged unit hits target across full field`() {
        val archerArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 5).apply {
            addUnits("Archer", 5)
        }
        val manager = TestableBattleManager(
            archerArmy, defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 }),
            useRealMovement = true
        )
        manager.initializeTurnQueue()

        val shooter = archerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(shooter, tile(0, 4))
        manager.placeTroop(defender, tile(13, 4))

        val result = manager.performTurn(BattleActionRequest(
            troop = shooter,
            targetPosition = tile(13, 4),
            actionType = ActionType.SHOOT
        ))

        assertTrue(result.success)
        assertEquals(8, defender.currentAmount)
    }

    @Test
    fun `ATTACK enemy after moving towards it succeeds`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))
        manager.placeTroop(defender, tile(4, 0))

        val moveResult = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(2, 0),
            actionType = ActionType.MOVE
        ))
        val attackResult = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(4, 0),
            attackTile = tile(3, 0),
            actionType = ActionType.ATTACK
        ))

        assertTrue(moveResult.success)
        assertTrue(attackResult.success)
        assertEquals(7, defender.currentAmount)
    }

    @Test
    fun `ATTACK enemy too far even after moving returns INVALID_TARGET`() {
        val manager = makeManager()
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tile(0, 0))
        manager.placeTroop(defender, tile(8, 0))

        val moveResult = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(2, 0),
            actionType = ActionType.MOVE
        ))
        // после MOVE на (2,0) — attackTile=(7,0) это distance=5 > speed=3
        val attackResult = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tile(8, 0),
            attackTile = tile(7, 0),
            actionType = ActionType.ATTACK
        ))

        assertTrue(moveResult.success)
        assertFalse(attackResult.success)
        assertEquals(ErrorId.INVALID_TARGET, attackResult.errorId)
    }
}
