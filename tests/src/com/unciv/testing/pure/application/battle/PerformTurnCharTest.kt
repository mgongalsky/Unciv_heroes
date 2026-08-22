package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
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

class PerformTurnCharTest {

    private lateinit var civInfo: FakeCivilizationInfo
    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerTile: FakeBattleTile
    private lateinit var defenderTile: FakeBattleTile
    private lateinit var emptyTile: FakeBattleTile

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
                name = "Spearman"; unitType = "Melee"
                damage = 10; health = 100; speed = 5
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"; unitType = "Melee"
                damage = 10; health = 100; speed = 5
                rangedStrength = 10
            }
        }

        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } }, testModule)
        }

        civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }

        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        defenderTile = FakeBattleTile(Vector2(1f, 0f))
        emptyTile = FakeBattleTile(Vector2(2f, 0f))

        manager = TestableBattleManager(
            attackerArmy, defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )

        manager.initializeTurnQueue()

        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        manager.placeTroop(attacker, attackerTile)
        manager.placeTroop(defender, defenderTile)
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    // --- SKIP ---

    @Test
    fun `SKIP returns success`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(actionType = ActionType.SKIP, troop = attacker, targetPosition = attackerTile)
        )
        assertTrue(result.success)
        assertEquals(ActionType.SKIP, result.actionType)
        assertFalse(result.battleEnded)
    }

    @Test
    fun `MOVE to free tile returns success`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(actionType = ActionType.MOVE, troop = attacker, targetPosition = emptyTile)
        )
        assertTrue(result.success)
        assertEquals(attackerTile, result.movedFrom)
        assertEquals(emptyTile, result.movedTo)
        assertFalse(result.battleEnded)
    }

    @Test
    fun `MOVE to occupied tile returns error`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(actionType = ActionType.MOVE, troop = attacker, targetPosition = defenderTile)
        )
        assertFalse(result.success)
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
    }

    @Test
    fun `ATTACK on enemy returns success`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(
                actionType = ActionType.ATTACK,
                troop = attacker,
                targetPosition = defenderTile,
                attackTile = attackerTile
            )
        )
        assertTrue(result.success)
        assertEquals(attackerTile, result.movedFrom)
        assertEquals(attackerTile, result.movedTo)
        assertFalse(result.battleEnded)
    }

    @Test
    fun `ATTACK with null attackTile returns error`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(
                actionType = ActionType.ATTACK,
                troop = attacker,
                targetPosition = defenderTile,
                attackTile = null
            )
        )
        assertFalse(result.success)
        assertEquals(ErrorId.INVALID_TARGET, result.errorId)
    }

    @Test
    fun `SHOOT on enemy returns success`() {
        val archerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Archer", 10) }
        val shooterManager = TestableBattleManager(
            archerArmy, defenderArmy,
            FakeBattleField(),
            FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )
        shooterManager.initializeTurnQueue()
        val archer = archerArmy.getAllTroops().first { it != null }!!
        val defender = defenderArmy.getAllTroops().first { it != null }!!
        shooterManager.placeTroop(archer, attackerTile)
        shooterManager.placeTroop(defender, defenderTile)

        val result = shooterManager.performTurn(
            BattleActionRequest(actionType = ActionType.SHOOT, troop = archer, targetPosition = defenderTile)
        )
        assertTrue(result.success)
        assertFalse(result.battleEnded)
        assertFalse(result.isLuck)
    }

    @Test
    fun `SHOOT by non-ranged troop returns error`() {
        val attacker = attackerArmy.getAllTroops().first { it != null }!!
        val result = manager.performTurn(
            BattleActionRequest(actionType = ActionType.SHOOT, troop = attacker, targetPosition = defenderTile)
        )
        assertFalse(result.success)
        assertEquals(ErrorId.NOT_IMPLEMENTED, result.errorId)
    }
}
