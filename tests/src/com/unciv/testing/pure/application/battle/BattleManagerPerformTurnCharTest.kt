package com.unciv.testing.pure.application.battle

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.unciv.ui.battlescreen.ActionType
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.badlogic.gdx.math.Vector2
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import com.unciv.logic.battle.performTurn

class BattleManagerPerformTurnCharTest {

    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager

    private lateinit var tileA: FakeBattleTile
    private lateinit var tileB: FakeBattleTile
    private lateinit var tileC: FakeBattleTile

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
                speed = 5
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

        tileA = FakeBattleTile(Vector2(0f, 0f))
        tileB = FakeBattleTile(Vector2(1f, 0f))
        tileC = FakeBattleTile(Vector2(2f, 0f))
        tileA.addNeighbor(tileB)
        tileB.addNeighbor(tileA)
        tileB.addNeighbor(tileC)
        tileC.addNeighbor(tileB)

        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 8) }

        manager = TestableBattleManager(
            attackerArmy, defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 })
        )
        manager.initializeTurnQueue()
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    // --- SKIP ---

    @Test
    fun `performTurn SKIP returns success`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileA,
            actionType = ActionType.SKIP
        ))
        assertTrue(result.success)
        assertEquals(ActionType.SKIP, result.actionType)
        assertFalse(result.battleEnded)
    }

    // --- MOVE ---

    @Test
    fun `performTurn MOVE to free tile returns success`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileB,
            actionType = ActionType.MOVE
        ))

        assertTrue(result.success)
        assertEquals(tileA, result.movedFrom)  // был assertNull — неверно
        assertEquals(Vector2(1f, 0f), result.movedTo?.position)
    }

    @Test
    fun `performTurn MOVE to self-occupied tile returns HEX_OCCUPIED`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)
        tileB.setTroop(attacker)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileB,
            actionType = ActionType.MOVE
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
    }

    @Test
    fun `performTurn MOVE to enemy-occupied tile returns HEX_OCCUPIED`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)
        manager.placeTroop(defender, tileB)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileB,
            actionType = ActionType.MOVE
        ))

        // TODO: MOVE на врага возвращает HEX_OCCUPIED, не INVALID_TARGET.
        //       Логика проверяет isTileFree ПОСЛЕ isTileOccupiedByAlly, не проверяя врага отдельно.
        //       Возможно стоит добавить отдельную ветку для MOVE на врага.
        assertFalse(result.success)
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
    }

    // --- ATTACK ---

    @Test
    fun `performTurn ATTACK enemy reduces defender amount`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)
        manager.placeTroop(defender, tileC)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileC,
            attackTile = tileB,
            actionType = ActionType.ATTACK
        ))

        assertTrue(result.success)
        assertFalse(result.isLuck)
        assertFalse(result.isMorale)
        assertFalse(result.battleEnded)
        assertEquals(7, defender.currentAmount)
    }

    @Test
    fun `performTurn ATTACK empty tile returns INVALID_TARGET`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileC,
            attackTile = tileB,
            actionType = ActionType.ATTACK
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.INVALID_TARGET, result.errorId)
    }

    @Test
    fun `performTurn ATTACK with null attackTile returns INVALID_TARGET`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)
        manager.placeTroop(defender, tileC)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileC,
            attackTile = null,
            actionType = ActionType.ATTACK
        ))

        assertFalse(result.success)
        assertEquals(ErrorId.INVALID_TARGET, result.errorId)
    }

    // --- SHOOT ---

    @Test
    fun `performTurn SHOOT with ranged unit reduces defender`() {
        val archerArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 5).apply {
            addUnits("Archer", 5)
        }
        val shootManager = TestableBattleManager(
            archerArmy, defenderArmy, FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 })
        )
        shootManager.initializeTurnQueue()

        val shooter = archerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        shootManager.placeTroop(shooter, tileA)
        shootManager.placeTroop(defender, tileC)

        val result = shootManager.performTurn(BattleActionRequest(
            troop = shooter,
            targetPosition = tileC,
            actionType = ActionType.SHOOT
        ))

        assertTrue(result.success)
        assertFalse(result.isLuck)
        assertEquals(8, defender.currentAmount)
        // TODO: лучник наносит меньше урона чем копейщик (8 vs 10 damage) — проверить формулу CalculateDamageUseCase
    }

    @Test
    fun `performTurn SHOOT with melee unit returns NOT_IMPLEMENTED`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, tileA)
        manager.placeTroop(defender, tileC)

        val result = manager.performTurn(BattleActionRequest(
            troop = attacker,
            targetPosition = tileC,
            actionType = ActionType.SHOOT
        ))

        assertFalse(result.success)
        // TODO: ошибка называется NOT_IMPLEMENTED — стоит переименовать в CANNOT_SHOOT
        assertEquals(ErrorId.NOT_IMPLEMENTED, result.errorId)
    }
}
