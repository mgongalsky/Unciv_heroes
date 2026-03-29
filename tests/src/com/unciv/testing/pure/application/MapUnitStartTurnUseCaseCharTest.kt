package com.unciv.testing.pure.application

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.application.MapUnitStartTurnUseCase
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeTileInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapUnitStartTurnUseCaseCharTest {

    private lateinit var civInfo: FakeCivilizationInfo
    private lateinit var tile: FakeTileInfo
    private lateinit var unit: TestableMapUnit

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        startKoin {
            allowOverride(true)
            modules(module {
                single {
                    Ruleset().apply {
                        val warrior = BaseUnit().apply { name = "Warrior" }
                        units["Warrior"] = warrior
                    }
                }
            })
        }
        civInfo = FakeCivilizationInfo()
        tile = FakeTileInfo()
        unit = TestableMapUnit()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun execute(
        getMaxMovement: () -> Int = { 2 },
        clearPathfindingCache: () -> Unit = {},
        teleportToClosestMoveableTile: () -> Unit = {},
        addMovementMemory: () -> Unit = {}
    ) {
        MapUnitStartTurnUseCase.execute(
            unit = unit,
            civInfo = civInfo,
            currentTile = tile,
            clearPathfindingCache = clearPathfindingCache,
            getMaxMovement = getMaxMovement,
            teleportToClosestMoveableTile = teleportToClosestMoveableTile,
            addMovementMemory = addMovementMemory
        )
    }

    // -------------------------------------------------------------------------
    // Basic reset
    // -------------------------------------------------------------------------

    @Test
    fun characterize_basicStartTurn() {
        unit.attacksThisTurn = 5
        unit.due = false
        execute(getMaxMovement = { 2 })
        assertEquals(0, unit.attacksThisTurn)
        assertTrue(unit.due)
        assertEquals(2.0f, unit.currentMovement, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Scenario 1: герой на пустом тайле
    // -------------------------------------------------------------------------

    @Test
    fun characterize_heroOnEmptyTile() {
        unit.attacksThisTurn = 2
        unit.due = false
        unit.attacksSinceTurnStart.add(Vector2(1f, 1f))

        execute(getMaxMovement = { 2 })

        assertEquals(0, unit.attacksThisTurn)
        assertTrue(unit.due)
        assertEquals(2.0f, unit.currentMovement, 0.01f)
        assertEquals(0, unit.attacksSinceTurnStart.size)
    }

    // -------------------------------------------------------------------------
    // Scenario 2: герой в городе
    // -------------------------------------------------------------------------

    @Test
    fun characterize_heroInCity() {
        tile.isCityCenterOverride = true
        civInfo.isCurrentPlayerOverride = true
        unit.attacksThisTurn = 1

        execute(getMaxMovement = { 2 })

        assertEquals(0, unit.attacksThisTurn)
        assertEquals(2.0f, unit.currentMovement, 0.01f)
        assertTrue(unit.due)
    }

    // -------------------------------------------------------------------------
    // Scenario 3: монстр начинает ход
    // -------------------------------------------------------------------------

    @Test
    fun characterize_monsterStartTurn() {
        val monsterUnit = object : MapUnit(1, "Warrior") {
            override fun createArmy() = unit.army
        }
        monsterUnit.attacksThisTurn = 3
        monsterUnit.due = false

        MapUnitStartTurnUseCase.execute(
            unit = monsterUnit,
            civInfo = civInfo,
            currentTile = tile,
            clearPathfindingCache = {},
            getMaxMovement = { 2 },
            teleportToClosestMoveableTile = {},
            addMovementMemory = {}
        )

        assertEquals(0, monsterUnit.attacksThisTurn)
        assertTrue(monsterUnit.due)
        assertEquals(2.0f, monsterUnit.currentMovement, 0.01f)
    }

    // -------------------------------------------------------------------------
    // Scenario 4: быстрый герой (движение 5)
    // -------------------------------------------------------------------------

    @Test
    fun characterize_fastHero_maxMovement5() {
        unit.attacksThisTurn = 1
        unit.due = false

        execute(getMaxMovement = { 5 })

        assertEquals(5.0f, unit.currentMovement, 0.01f)
        assertEquals(0, unit.attacksThisTurn)
        assertTrue(unit.due)
    }

    // -------------------------------------------------------------------------
    // Scenario 5: герой с историей атак
    // -------------------------------------------------------------------------

    @Test
    fun characterize_heroWithAttackHistory() {
        unit.attacksSinceTurnStart.add(Vector2(1f, 1f))
        unit.attacksSinceTurnStart.add(Vector2(2f, 2f))
        unit.attacksSinceTurnStart.add(Vector2(3f, 3f))
        unit.attacksThisTurn = 3

        execute(getMaxMovement = { 2 })

        assertEquals(0, unit.attacksSinceTurnStart.size)
        assertEquals(0, unit.attacksThisTurn)
        assertEquals(2.0f, unit.currentMovement, 0.01f)
    }
}
