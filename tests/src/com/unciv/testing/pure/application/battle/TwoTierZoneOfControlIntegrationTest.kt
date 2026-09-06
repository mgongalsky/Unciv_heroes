package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.battle.execute
import com.unciv.logic.map.TileInfo
import com.unciv.logic.map.TileMap
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.tile.Terrain
import com.unciv.models.ruleset.tile.TerrainType
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleCommand
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattlePolicy
import com.unciv.pure.application.battle.BattleRejection
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class TwoTierZoneOfControlIntegrationTest {
    private lateinit var ruleset: Ruleset

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.0, moraleProbability = 0.0, armySize = 5)
        )
        ruleset = Ruleset().apply {
            terrains["Grassland"] = Terrain().apply {
                name = "Grassland"
                type = TerrainType.Land
            }
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            for (unitName in listOf("Runner", "Guard", "Second guard")) {
                units[unitName] = BaseUnit().apply {
                    name = unitName
                    unitType = "Melee"
                    speed = if (unitName == "Runner") 3 else 1
                    health = 100
                    damage = 10
                }
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    private data class Scenario(
        val manager: BattleManager,
        val runner: Troop,
        val guard: Troop,
        val start: TileInfo,
        val next: TileInfo,
        val beyond: TileInfo
    )

    private fun scenario(reinforced: Boolean = false): Scenario {
        val map = TileMap(1, ruleset)
        val center = map[Vector2.Zero]
        val ring = center.neighbors.toList()
        val start = ring.first()
        val next = start.neighbors.first { it in ring }
        val beyond = next.neighbors.first { it in ring && it != start }
        val attackers = ArmyInfo(FakeCivilizationInfo(), 5).apply { addUnits("Runner", 10) }
        val defenders = ArmyInfo(FakeCivilizationInfo(), 5).apply {
            addUnits("Guard", 10)
            if (reinforced) addUnits("Second guard", 10)
        }
        val manager = BattleManager(
            attackers, defenders, map, SeededBattleRandom(42L),
            moraleProbability = 0.0, luckProbability = 0.0
        )
        val runner = attackers.getAllTroops().filterNotNull().single()
        val guard = defenders.getAllTroops().filterNotNull().first { it.unitName == "Guard" }
        fun place(troop: Troop, tile: TileInfo) {
            tile.receiveTroop(troop)
            manager.setTroopPosition(troop, tile)
        }
        place(runner, start)
        place(guard, center)
        if (reinforced) {
            val second =
                defenders.getAllTroops().filterNotNull().first { it.unitName == "Second guard" }
            place(second, start.neighbors.first { it in ring && it != next })
        }
        manager.initializeTurnQueue()
        return Scenario(manager, runner, guard, start, next, beyond)
    }

    @Test
    fun `real hex field permits one doubled step but rejects two with speed three`() {
        val s = scenario()
        assertEquals(6, s.manager.getTroopTile(s.guard)!!.neighbors.count())
        assertTrue(s.manager.getReachableTiles(s.runner).contains(s.next))
        assertFalse(s.manager.getReachableTiles(s.runner).contains(s.beyond))
        assertTrue(s.manager.isTileAchievable(s.runner, s.next))
        assertFalse(s.manager.isTileAchievable(s.runner, s.beyond))
        val result = s.manager.execute(BattleCommand.Move(s.runner.id, s.next.toPoint()))
        assertTrue(result.success)
        assertSame(s.next, s.manager.getTroopTile(s.runner))
    }

    @Test
    fun `over budget command leaves troops formation and events unchanged`() {
        val s = scenario()
        val formation = s.runner.formation.current
        val events = mutableListOf<BattleEvent>()
        val result = s.manager.execute(
            BattleCommand.Move(
                s.runner.id,
                s.beyond.toPoint()
            )
        ) { events.add(it) }
        assertFalse(result.success)
        assertEquals(BattleRejection.TOO_FAR, result.rejection)
        assertSame(s.start, s.manager.getTroopTile(s.runner))
        assertSame(s.runner, s.start.getTroop())
        assertNull(s.beyond.getTroop())
        assertEquals(formation, s.runner.formation.current)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `removing the guard immediately restores ordinary movement range`() {
        val s = scenario()
        assertFalse(s.manager.isTileAchievable(s.runner, s.beyond))
        s.manager.removeTroop(s.guard)
        assertTrue(s.manager.isTileAchievable(s.runner, s.beyond))
        assertTrue(s.manager.getReachableTiles(s.runner).contains(s.beyond))
    }

    @Test
    fun `headless runner advances one controlled step per activation`() {
        val s = scenario(reinforced = true)
        assertTrue(s.manager.isTileAchievable(s.runner, s.next))
        assertFalse(s.manager.isTileAchievable(s.runner, s.beyond))
        val movement = BattlePolicy { id ->
            val destination = if (s.manager.getTroopTile(s.runner) == s.start) s.next else s.beyond
            BattleCommand.Move(id, destination.toPoint())
        }
        val result = BattleSimulationRunner(
            s.manager, movement, BattlePolicy { BattleCommand.Skip(it) },
            maxTurns = 4, maxTurnsWithoutProgress = 10, seed = 42L
        ).run()
        assertEquals(BattleTermination.MAX_TURNS, result.termination)
        assertEquals(4, result.turns)
        val moves = result.events.filterIsInstance<BattleEvent.TroopMoved>()
        assertEquals(listOf(s.next.toPoint(), s.beyond.toPoint()), moves.map { it.to })
        assertSame(s.beyond, s.manager.getTroopTile(s.runner))
    }

    @Test
    fun `morale grants a second controlled step before the enemy activation`() {
        val s = scenario(reinforced = true)
        val attackers = s.manager.getAttackerArmy()
        val defenders = s.manager.getDefenderArmy()
        // The public getter creates a real civilization when unset; preserve null without invoking it.
        val civilizationField = com.unciv.logic.map.MapUnit::class.java
            .getDeclaredField("_monsterCivInfo").apply { isAccessible = true }
        val previousCivilization = civilizationField.get(null)
        val previousId = com.unciv.logic.map.MapUnit.currID
        try {
            com.unciv.logic.map.MapUnit.setTestingInstance(FakeCivilizationInfo())
            attackers.hero = com.unciv.testing.pure.fakes.TestableMapUnit().apply { morale = 3 }
        } finally {
            civilizationField.set(null, previousCivilization)
            com.unciv.logic.map.MapUnit.currID = previousId
        }
        val manager = BattleManager(
            attackers, defenders, s.manager.battleField, SeededBattleRandom(42L),
            moraleProbability = 1.0, luckProbability = 0.0
        )
        (attackers.getAllTroops() + defenders.getAllTroops()).filterNotNull().forEach {
            manager.setTroopPosition(it, s.manager.getTroopTile(it)!!)
        }
        manager.initializeTurnQueue()
        val destinations = java.util.ArrayDeque(listOf(s.next, s.beyond))
        val movement =
                BattlePolicy { id -> BattleCommand.Move(id, destinations.removeFirst().toPoint()) }
        val result = BattleSimulationRunner(
            manager, movement, BattlePolicy { BattleCommand.Skip(it) },
            maxTurns = 2, maxTurnsWithoutProgress = 10, seed = 42L
        ).run()
        assertEquals(BattleTermination.MAX_TURNS, result.termination)
        assertEquals(listOf(s.runner.id, s.runner.id), result.commands.map { it.troopId })
        val moves = result.events.filterIsInstance<BattleEvent.TroopMoved>()
        assertEquals(listOf(s.next.toPoint(), s.beyond.toPoint()), moves.map { it.to })
        assertTrue(moves.all { it.isMorale })
        assertSame(s.beyond, manager.getTroopTile(s.runner))
    }

    @Test
    fun `attack cannot bypass control by choosing an unaffordable approach tile`() {
        val s = scenario()
        val formation = s.runner.formation.current
        val guardAmount = s.guard.currentAmount
        val guardHealth = s.guard.currentHealth
        val guardFormation = s.guard.formation.current
        val events = mutableListOf<BattleEvent>()
        val result = s.manager.execute(
            BattleCommand.Attack(
                s.runner.id,
                s.manager.getTroopTile(s.guard)!!.toPoint(),
                s.beyond.toPoint()
            )
        ) { events.add(it) }
        assertFalse(result.success)
        assertEquals(BattleRejection.INVALID_TARGET, result.rejection)
        assertSame(s.start, s.manager.getTroopTile(s.runner))
        assertEquals(formation, s.runner.formation.current)
        assertEquals(guardAmount, s.guard.currentAmount)
        assertEquals(guardHealth, s.guard.currentHealth)
        assertEquals(guardFormation, s.guard.formation.current)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `four movement leaves real red control through two yellow cells in one command`() {
        ruleset.units.getValue("Runner").speed = 4
        val s = scenario(reinforced = true)
        val context = com.unciv.pure.application.pathfinding.TroopMovementContext(s.runner) {
            s.manager.getDefenderArmy().contains(it)
        }
        assertEquals(
            com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength.REINFORCED,
            context.controlStrength(s.start)
        )
        assertEquals(
            com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength.NORMAL,
            context.controlStrength(s.next)
        )
        assertEquals(
            com.unciv.pure.domain.battle.ZoneOfControlTransition.Strength.NORMAL,
            context.controlStrength(s.beyond)
        )
        val paths = com.unciv.pure.application.pathfinding.MovementRangeUseCase.execute(
            s.start,
            4f,
            context
        )
        assertEquals(4f, paths.getValue(s.beyond).totalDistance, 0f)
        assertEquals(listOf(s.next, s.beyond), paths.getPathToTile(s.beyond))
        val events = mutableListOf<BattleEvent>()
        val result = s.manager.execute(BattleCommand.Move(s.runner.id, s.beyond.toPoint())) {
            events.add(it)
            if (it is BattleEvent.TroopMoved) assertSame(s.beyond, s.manager.getTroopTile(s.runner))
        }
        assertTrue(result.success)
        assertNull(s.start.getTroop())
        assertSame(s.runner, s.beyond.getTroop())
        assertEquals(
            listOf(s.beyond.toPoint()),
            events.filterIsInstance<BattleEvent.TroopMoved>().map { it.to })
    }

    @Test
    fun `real field range and move commands agree at every yellow route budget`() {
        for (speed in 1..6) {
            for (steps in 1..2) {
                ruleset.units.getValue("Runner").speed = speed
                val s = scenario(reinforced = true)
                val destination = if (steps == 1) s.next else s.beyond
                val expected = speed >= steps * 2
                val label = "speed=$speed steps=$steps"
                assertEquals(
                    label,
                    expected,
                    s.manager.getReachableTiles(s.runner).contains(destination)
                )
                assertEquals(label, expected, s.manager.isTileAchievable(s.runner, destination))
                val formation = s.runner.formation.copy()
                val health = s.runner.currentHealth
                val amount = s.runner.currentAmount
                val events = mutableListOf<BattleEvent>()
                val result =
                        s.manager.execute(BattleCommand.Move(s.runner.id, destination.toPoint())) {
                            events.add(it)
                        }
                assertEquals(label, expected, result.success)
                assertEquals(label, health, s.runner.currentHealth)
                assertEquals(label, amount, s.runner.currentAmount)
                if (expected) {
                    assertSame(destination, s.manager.getTroopTile(s.runner))
                    assertSame(s.runner, destination.getTroop())
                    assertNull(s.start.getTroop())
                } else {
                    assertSame(s.start, s.manager.getTroopTile(s.runner))
                    assertSame(s.runner, s.start.getTroop())
                    assertNull(destination.getTroop())
                    assertEquals(formation, s.runner.formation)
                    assertTrue(label, events.isEmpty())
                }
            }
        }
    }

    @Test
    fun `headless runner completes red to yellow to yellow route in one activation`() {
        ruleset.units.getValue("Runner").speed = 4
        val s = scenario(reinforced = true)
        val result = BattleSimulationRunner(
            s.manager,
            BattlePolicy { BattleCommand.Move(it, s.beyond.toPoint()) },
            BattlePolicy { BattleCommand.Skip(it) },
            maxTurns = 1, maxTurnsWithoutProgress = 10, seed = 42L
        ).run()
        assertEquals(BattleTermination.MAX_TURNS, result.termination)
        assertEquals(1, result.turns)
        assertEquals(
            listOf(s.beyond.toPoint()),
            result.events.filterIsInstance<BattleEvent.TroopMoved>().map { it.to })
        assertSame(s.beyond, s.manager.getTroopTile(s.runner))
    }
}
