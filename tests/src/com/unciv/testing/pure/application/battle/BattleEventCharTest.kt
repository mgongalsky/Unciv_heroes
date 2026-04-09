package com.unciv.testing.pure.application.battle

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.battle.BattleEvent
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import com.unciv.ui.battlescreen.ActionType
import com.unciv.ui.battlescreen.BattleActionRequest
import com.badlogic.gdx.math.Vector2
import com.unciv.pure.domain.battle.Point
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

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
            GameConstantsData(
                luckProbability = 0.0,
                moraleProbability = 0.0,
                armySize = 5
            )
        )

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            val spearman = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                damage = 10
                health = 100
                speed = 5
            }
            units["Spearman"] = spearman
        }

        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }

        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        defenderTile = FakeBattleTile(Vector2(1f, 0f))
        emptyTile = FakeBattleTile(Vector2(2f, 0f))

        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        defenderTile.addNeighbor(emptyTile)
        emptyTile.addNeighbor(defenderTile)

        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Spearman", 10)
        }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Spearman", 8)
        }

        manager = TestableBattleManager(
            attackerArmy = attackerArmy,
            defenderArmy = defenderArmy,
            battleField = FakeBattleField(),
            random = FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )

        manager.initializeTurnQueue()

        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
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
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        manager.performTurn(BattleActionRequest(
            troop = manager.getCurrentTroop()!!,
            targetPosition = attackerTile,
            actionType = ActionType.SKIP
        ))

        assertEquals(1, events.size)
        assertTrue(events[0] is BattleEvent.TurnSkipped)
    }

    @Test
    fun `characterize events on MOVE to empty tile`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        manager.performTurn(BattleActionRequest(
            troop = manager.getCurrentTroop()!!,
            targetPosition = emptyTile,
            actionType = ActionType.MOVE
        ))

        assertEquals(1, events.size)
        val event = events[0] as BattleEvent.TroopMoved
        assertEquals(Point(0, 0), event.from)
        assertEquals(Point(2, 0), event.to)
        assertFalse(event.isMorale)
    }

    @Test
    fun `characterize events on MOVE to occupied tile`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        manager.performTurn(BattleActionRequest(
            troop = manager.getCurrentTroop()!!,
            targetPosition = defenderTile,
            actionType = ActionType.MOVE
        ))

        assertTrue(events.isEmpty())
    }

    @Test
    fun `characterize events on ATTACK`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        manager.performTurn(BattleActionRequest(
            troop = manager.getCurrentTroop()!!,
            targetPosition = defenderTile,
            actionType = ActionType.ATTACK,
            attackTile = attackerTile
        ))

        assertEquals(1, events.size)
        val event = events[0] as BattleEvent.TroopAttacked
        assertFalse(event.isLuck)
        assertFalse(event.isMorale)
        assertFalse(event.defenderDied)
        assertEquals(7, event.defenderRemainingAmount)
    }

    @Test
    fun `characterize events on advanceTurn`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        manager.advanceTurn()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `characterize BattleEnded event via direct attack`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        defenderArmy.getAllTroops().filterNotNull().dropLast(1).forEach {
            manager.removeTroop(it)
        }
        val lastDefender = defenderArmy.getAllTroops().filterNotNull().first()
        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        manager.attack(lastDefender, attacker)

        assertTrue(events.isEmpty())
    }
}
