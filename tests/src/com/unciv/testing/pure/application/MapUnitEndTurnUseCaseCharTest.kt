package com.unciv.testing.pure.application

import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.MapUnitEndTurnUseCase
import com.unciv.pure.domain.supply.RealSupplyMechanic
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeTileInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse


class MapUnitEndTurnUseCaseCharTest {

    private lateinit var civInfo: FakeCivilizationInfo
    private lateinit var tile: FakeTileInfo
    private lateinit var unit: TestableMapUnit

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            val warrior = BaseUnit().apply {
                name = "Warrior"
                unitType = "Melee"
            }
            units["Warrior"] = warrior
        }

        startKoin {
            allowOverride(true)
            modules(module {
                single { fakeRuleset }
            })
        }

        civInfo = FakeCivilizationInfo()
        tile = FakeTileInfo()
        unit = TestableMapUnit()

        unit.baseUnit = BaseUnit().apply {
            name = "Warrior"
            ruleset = fakeRuleset
            unitType = "Melee"
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun execute(
        clearPathfindingCache: () -> Unit = {},
        heal: () -> Unit = {},
        doCitadelDamage: () -> Unit = {},
        doTerrainDamage: () -> Unit = {},
        addMovementMemory: () -> Unit = {},
        onReligiousStrengthLost: () -> Unit = {}
    ) {
        MapUnitEndTurnUseCase.execute(
            unit = unit,
            civInfo = civInfo,
            currentTile = tile,
            ruleset = org.koin.core.context.GlobalContext.get().get(),
            supplyMechanic = RealSupplyMechanic(), // добавляем
            clearPathfindingCache = clearPathfindingCache,
            heal = heal,
            doCitadelDamage = doCitadelDamage,
            doTerrainDamage = doTerrainDamage,
            addMovementMemory = addMovementMemory,
            onReligiousStrengthLost = onReligiousStrengthLost
        )
    }

    @Test
    fun characterize_basicEndTurn() {
        unit.currentMovement = 2f

        var healCalled = false
        var citadelCalled = false
        var terrainCalled = false
        var memoryCalled = false

        execute(
            heal = { healCalled = true },
            doCitadelDamage = { citadelCalled = true },
            doTerrainDamage = { terrainCalled = true },
            addMovementMemory = { memoryCalled = true }
        )

        assertEquals(100, unit.health)
        assertEquals(3.0f, unit.hero.currentFood, 0.01f)
        assertFalse(healCalled)
        assertTrue(citadelCalled)
        assertTrue(terrainCalled)
        assertTrue(memoryCalled)
    }
    @Test
    fun characterize_nonMonster_foodConsumed() {
        unit.hero.currentFood = 10f
        tile.isCityCenterOverride = false

        execute()

        assertEquals(10.0f, unit.hero.currentFood, 0.01f)
    }

    @Test
    fun characterize_monster_foodNotConsumed() {
        val monster = object : MapUnit(1, "Warrior") {
            override fun createArmy() = unit.army
        }
        monster.hero.currentFood = 10f
        monster.baseUnit = unit.baseUnit

        MapUnitEndTurnUseCase.execute(
            unit = monster,
            civInfo = civInfo,
            currentTile = tile,
            ruleset = org.koin.core.context.GlobalContext.get().get(),
            supplyMechanic = RealSupplyMechanic(), // добавляем
            clearPathfindingCache = {},
            heal = {},
            doCitadelDamage = {},
            doTerrainDamage = {},
            addMovementMemory = {},
            onReligiousStrengthLost = {},
            isPreparingParadropOrAirSweep = { false }
        )

        assertEquals(10.0f, monster.hero.currentFood, 0.01f)
    }

    @Test
    fun characterize_citadelAndTerrain_alwaysCalled() {
        var citadelCalled = false
        var terrainCalled = false

        execute(
            doCitadelDamage = { citadelCalled = true },
            doTerrainDamage = { terrainCalled = true }
        )

        assertTrue(citadelCalled)
        assertTrue(terrainCalled)
    }

    @Test
    fun characterize_addMovementMemory_alwaysCalled() {
        var memoryCalled = false

        execute(addMovementMemory = { memoryCalled = true })

        assertTrue(memoryCalled)
    }

    @Test
    fun characterize_nonMonster_inCity_foodNotConsumed() {
        unit.hero.currentFood = 10f
        tile.isCityCenterOverride = true

        execute()

        assertEquals(10.0f, unit.hero.currentFood, 0.01f)
    }}
