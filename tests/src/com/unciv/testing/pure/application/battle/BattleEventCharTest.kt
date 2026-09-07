package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.battle.BattleEvent
import com.unciv.pure.domain.battle.Point
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import com.unciv.logic.battle.performTurn

class BattleEventCharTest {
    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager
    private lateinit var attackerTile: FakeBattleTile
    private lateinit var defenderTile: FakeBattleTile
    private lateinit var emptyTile: FakeBattleTile

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        val fakeRuleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 5
            }
        }
        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } }, testModule)
        }
        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        defenderTile = FakeBattleTile(Vector2(1f, 0f))
        emptyTile = FakeBattleTile(Vector2(2f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        defenderTile.addNeighbor(emptyTile)
        emptyTile.addNeighbor(defenderTile)
        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 8) }
        val attacker = attackerArmy.getAllTroops().filterNotNull().first().apply {
            formation.current = 0
        }
        val defender = defenderArmy.getAllTroops().filterNotNull().first().apply {
            formation.current = 0
        }
        manager = TestableBattleManager(
            attackerArmy = attackerArmy,
            defenderArmy = defenderArmy,
            battleField = FakeBattleField(),
            random = FakeBattleRandom(List(100) { 0.0 })
        )
        manager.initializeTurnQueue()
        manager.placeTroop(attacker, attackerTile)
        manager.placeTroop(defender, defenderTile)
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `characterize events on SKIP`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        manager.performTurn(
            BattleActionRequest(
                manager.getCurrentTroop()!!,
                attackerTile,
                ActionType.SKIP
            )
        ) { events.add(it) }
        assertEquals(1, events.size)
        assertTrue(events[0] is com.unciv.pure.application.battle.BattleEvent.TurnSkipped)
    }

    @Test
    fun `characterize events on MOVE to empty tile`() {
        val destination = FakeBattleTile(Vector2(-1f, 0f))
        attackerTile.addNeighbor(destination)
        destination.addNeighbor(attackerTile)
        val troop = manager.getCurrentTroop()!!
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val result = manager.performTurn(
            BattleActionRequest(troop, destination, ActionType.MOVE)
        ) {
            assertSame(destination, manager.getTroopTile(troop))
            events.add(it)
        }
        assertTrue(result.success)
        assertEquals(1, events.size)
        val event = events.single() as com.unciv.pure.application.battle.BattleEvent.TroopMoved
        assertEquals(Point(0, 0), event.from)
        assertEquals(Point(-1, 0), event.to)
        assertFalse(event.isMorale)
        assertNull(attackerTile.getTroop())
        assertSame(troop, destination.getTroop())
    }

    @Test
    fun `characterize events on MOVE to occupied tile`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        manager.performTurn(
            BattleActionRequest(
                manager.getCurrentTroop()!!,
                defenderTile,
                ActionType.MOVE
            )
        ) { events.add(it) }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `characterize events on ATTACK`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        manager.performTurn(
            BattleActionRequest(
                manager.getCurrentTroop()!!,
                defenderTile,
                ActionType.ATTACK,
                attackerTile
            )
        ) { events.add(it) }
        assertEquals(1, events.size)
        val event = events[0] as com.unciv.pure.application.battle.BattleEvent.TroopAttacked
        assertFalse(event.isLuck)
        assertFalse(event.isMorale)
        assertFalse(event.defenderDied)
        assertEquals(7, event.defenderRemainingAmount)
    }

    @Test
    fun `SKIP succeeds without advancing current troop and emits TurnSkipped`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val currentBefore = manager.getCurrentTroop()?.id
        val result = manager.performTurn(
            BattleActionRequest(
                manager.getCurrentTroop()!!,
                attackerTile,
                ActionType.SKIP
            )
        ) { events.add(it) }
        assertTrue(result.success)
        assertFalse(result.battleEnded)
        assertEquals(currentBefore, manager.getCurrentTroop()?.id)
        assertEquals(1, events.size)
        assertTrue(events.single() is com.unciv.pure.application.battle.BattleEvent.TurnSkipped)
    }

    @Test
    fun `rejected MOVE preserves field state and emits no events`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        val attackerPositionBefore = manager.getTroopTile(attacker)?.position
        val defenderOnTargetBefore = manager.getTroopOnTile(defenderTile)?.id
        val result = manager.performTurn(
            BattleActionRequest(attacker, defenderTile, ActionType.MOVE)
        ) { events.add(it) }
        assertFalse(result.success)
        assertEquals(ErrorId.HEX_OCCUPIED, result.errorId)
        assertEquals(attackerPositionBefore, manager.getTroopTile(attacker)?.position)
        assertEquals(defenderOnTargetBefore, manager.getTroopOnTile(defenderTile)?.id)
        assertEquals(defender.id, manager.getTroopOnTile(defenderTile)?.id)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `lethal ATTACK removes defender and emits attack before battle end`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        defender.currentAmount = 1
        val result = manager.performTurn(
            BattleActionRequest(
                attacker,
                defenderTile,
                ActionType.ATTACK,
                attackerTile
            )
        ) { events.add(it) }
        assertTrue(result.success)
        assertTrue(result.battleEnded)
        assertEquals(0, defender.currentAmount)
        assertFalse(defenderArmy.contains(defender))
        assertNull(manager.getTroopTile(defender))
        assertFalse(manager.getTurnQueue().contains(defender))
        assertEquals(listOf("TroopAttacked", "BattleEnded"), events.map { it.javaClass.simpleName })
    }

    @Test
    fun `legacy request can publish application event without mutable manager listener`() {
        val events = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()

        manager.performTurn(
            BattleActionRequest(
                manager.getCurrentTroop()!!,
                attackerTile,
                ActionType.SKIP
            )
        ) { events.add(it) }

        assertEquals(
            listOf(com.unciv.pure.application.battle.BattleEvent.TurnSkipped),
            events
        )
    }
}
