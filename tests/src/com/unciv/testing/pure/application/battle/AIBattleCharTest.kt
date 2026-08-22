package com.unciv.testing.pure.application.battle

import com.unciv.ai.AIBattle
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
import com.badlogic.gdx.math.Vector2
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class AIBattleCharTest {

    private lateinit var attackerArmy: ArmyInfo
    private lateinit var defenderArmy: ArmyInfo
    private lateinit var manager: TestableBattleManager

    private lateinit var attackerTile: FakeBattleTile
    private lateinit var defenderTile: FakeBattleTile
    private lateinit var middleTile: FakeBattleTile

    @Before
    fun setUp() {
        AIBattle.AI_verbose = false
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.0,
                moraleProbability = 0.0,
                armySize = 5
            )
        )

        val fakeRuleset = Ruleset().apply {
            val sword = UnitType().apply { name = "Sword" }
            val archery = UnitType().apply { name = "Archery" }
            unitTypes["Sword"] = sword
            unitTypes["Archery"] = archery
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Sword"
                speed = 5; health = 40; damage = 10; rangedStrength = 0
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"; unitType = "Archery"
                speed = 3; health = 15; damage = 5; rangedStrength = 7
            }
        }

        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }

        // три тайла в ряд: attacker -- middle -- defender
        attackerTile = FakeBattleTile(Vector2(0f, 0f))
        middleTile   = FakeBattleTile(Vector2(1f, 0f))
        defenderTile = FakeBattleTile(Vector2(2f, 0f))

        attackerTile.addNeighbor(middleTile)
        middleTile.addNeighbor(attackerTile)
        middleTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(middleTile)

        val civInfo = FakeCivilizationInfo()
        attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }
        defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }

        manager = TestableBattleManager(
            attackerArmy = attackerArmy,
            defenderArmy = defenderArmy,
            battleField = FakeBattleField(),
            random = FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )

        val attacker = attackerArmy.getAllTroops().filterNotNull().first()
        val defender = defenderArmy.getAllTroops().filterNotNull().first()
        manager.placeTroop(attacker, attackerTile)
        manager.placeTroop(defender, defenderTile)
        manager.initializeTurnQueue()
    }

    @After
    fun tearDown() {
        AIBattle.AI_verbose = true
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `characterize melee AI attacks adjacent enemy`() {
        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        val troop = attackerArmy.getAllTroops().filterNotNull().first()
        AIBattle(manager).performTurn(troop)

        assertEquals(1, events.size)
        val event = events[0] as BattleEvent.TroopAttacked
        assertEquals(8, event.defenderRemainingAmount)
        assertFalse(event.isLuck)
        assertFalse(event.isMorale)
        assertFalse(event.defenderDied)
    }

    @Test
    fun `characterize melee AI moves toward enemy when not adjacent`() {
        val farTile = FakeBattleTile(Vector2(5f, 0f))
        val troop = attackerArmy.getAllTroops().filterNotNull().first()
        manager.placeTroop(troop, farTile)

        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        AIBattle(manager).performTurn(troop)

        assertEquals(1, events.size)
        assertTrue(events[0] is BattleEvent.TroopAttacked)
    }

    @Test
    fun `characterize melee AI when no enemies`() {
        defenderArmy.getAllTroops().filterNotNull().forEach { manager.removeTroop(it) }

        val events = mutableListOf<BattleEvent>()
        manager.onEvent = { events.add(it) }

        val troop = attackerArmy.getAllTroops().filterNotNull().first()
        AIBattle(manager).performTurn(troop)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `characterize ranged AI shoots enemy`() {
        val civInfo = FakeCivilizationInfo()
        val archerArmy = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Archer", 10) }
        val spearArmy  = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Spearman", 10) }

        val archerManager = TestableBattleManager(
            attackerArmy = archerArmy,
            defenderArmy = spearArmy,
            battleField = FakeBattleField(),
            random = FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )

        val archer = archerArmy.getAllTroops().filterNotNull().first()
        val spear  = spearArmy.getAllTroops().filterNotNull().first()
        archerManager.placeTroop(archer, attackerTile)
        archerManager.placeTroop(spear, defenderTile)
        archerManager.initializeTurnQueue()

        val events = mutableListOf<BattleEvent>()
        archerManager.onEvent = { events.add(it) }

        AIBattle(archerManager).performTurn(archer)

        assertEquals(1, events.size)
        val event = events[0] as BattleEvent.TroopShot
        assertEquals(9, event.defenderRemainingAmount)
        assertFalse(event.isLuck)
        assertFalse(event.defenderDied)
    }

    @Test
    fun `characterize ranged AI target priority ranged over melee`() {
        val civInfo = FakeCivilizationInfo()
        val archerArmy   = ArmyInfo(civInfo, maxSlots = 5).apply { addUnits("Archer", 10) }
        val mixedDefArmy = ArmyInfo(civInfo, maxSlots = 5).apply {
            addUnits("Spearman", 5)
            addUnits("Archer", 5)
        }

        val archerManager = TestableBattleManager(
            attackerArmy = archerArmy,
            defenderArmy = mixedDefArmy,
            battleField = FakeBattleField(),
            random = FakeBattleRandom(List(100) { 0.0 }),
            allTilesReachable = true
        )

        val shooter = archerArmy.getAllTroops().filterNotNull().first()
        mixedDefArmy.getAllTroops().filterNotNull().forEachIndexed { i, troop ->
            archerManager.placeTroop(troop, FakeBattleTile(Vector2(i.toFloat() + 1f, 0f)))
        }
        archerManager.placeTroop(shooter, attackerTile)
        archerManager.initializeTurnQueue()

        val events = mutableListOf<BattleEvent>()
        archerManager.onEvent = { events.add(it) }

        AIBattle(archerManager).performTurn(shooter)

        val shot = events.filterIsInstance<BattleEvent.TroopShot>().first()
        // AI стреляет в лучника (ranged приоритет), не в копейщика
        val targetTroop = mixedDefArmy.getAllTroops().filterNotNull().find { it.id == shot.defenderId }
        assertNotNull(targetTroop)
        assertTrue(targetTroop!!.isRanged)
    }

    @Test
    fun `AI forwards application events while preserving legacy events`() {
        val legacyEvents = mutableListOf<BattleEvent>()
        val applicationEvents = mutableListOf<com.unciv.pure.application.battle.BattleEvent>()
        manager.onEvent = { legacyEvents.add(it) }
        val troop = attackerArmy.getAllTroops().filterNotNull().first()

        AIBattle(manager) { applicationEvents.add(it) }.performTurn(troop)

        assertEquals(listOf("TroopAttacked"), legacyEvents.map { it.javaClass.simpleName })
        assertEquals(listOf("TroopAttacked"), applicationEvents.map { it.javaClass.simpleName })
    }
}
