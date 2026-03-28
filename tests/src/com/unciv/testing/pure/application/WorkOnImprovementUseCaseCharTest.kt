package com.unciv.testing.pure.application

import com.unciv.Constants
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.RoadStatus
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.WorkOnImprovementUseCase
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeTileInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
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

class WorkOnImprovementUseCaseCharTest {

    private lateinit var civInfo: FakeCivilizationInfo
    private lateinit var tile: FakeTileInfo
    private lateinit var unit: TestableMapUnit

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        startKoin {
            allowOverride(true)
            modules(module {
                single { Ruleset() }
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

    // -------------------------------------------------------------------------
    // Scenario 1: early exit — isMarkedForCreatesOneImprovement
    // turnsToImprovement < 0 means marked for CreatesOneImprovement
    // UseCase should return immediately without touching anything
    // -------------------------------------------------------------------------

    @Test
    fun characterize_earlyExit_isMarkedForCreatesOneImprovement() {
        tile.turnsToImprovement = -1
        tile.improvementInProgress = "Farm"

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { }, tryProvideProductionToClosestCity = { }
        )

        assertEquals(-1, tile.turnsToImprovement)
        assertEquals("Farm", tile.improvementInProgress)
    }

    // -------------------------------------------------------------------------
    // Scenario 2: decrement only — turnsToImprovement > 1
    // Only decrements counter, nothing else happens
    // -------------------------------------------------------------------------

    @Test
    fun characterize_decrement_turnsNotFinished() {
        tile.turnsToImprovement = 3
        tile.improvementInProgress = "Farm"
        var completedCalled = false

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { completedCalled = true },
            tryProvideProductionToClosestCity = { }
        )

        assertEquals(2, tile.turnsToImprovement)
        assertEquals("Farm", tile.improvementInProgress)
        assertFalse(completedCalled)
        assertFalse(tile.setRepairedCalled)
        assertFalse(tile.addRoadCalled)
    }

    // -------------------------------------------------------------------------
    // Scenario 3: repair completes
    // setRepaired() called, improvementInProgress cleared
    // -------------------------------------------------------------------------

    @Test
    fun characterize_repair_completes() {
        tile.turnsToImprovement = 1
        tile.improvementInProgress = Constants.repair

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { }, tryProvideProductionToClosestCity = { }
        )

        assertTrue(tile.setRepairedCalled)
        assertEquals(0, tile.turnsToImprovement)
        assertNull(tile.improvementInProgress)
    }

    // -------------------------------------------------------------------------
    // Scenario 4: Road completes
    // addRoad() called with Road type, improvementInProgress cleared
    // -------------------------------------------------------------------------

    @Test
    fun characterize_road_completes() {
        tile.turnsToImprovement = 1
        tile.improvementInProgress = RoadStatus.Road.name

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { }, tryProvideProductionToClosestCity = { }
        )

        assertTrue(tile.addRoadCalled)
        assertEquals(RoadStatus.Road, tile.roadStatus)
        assertNull(tile.improvementInProgress)
    }

    // -------------------------------------------------------------------------
    // Scenario 5: onImprovementCompleted called when isCurrentPlayer = true
    // -------------------------------------------------------------------------

    @Test
    fun characterize_onImprovementCompleted_calledWhenCurrentPlayer() {
        tile.turnsToImprovement = 1
        tile.improvementInProgress = Constants.repair
        civInfo.isCurrentPlayerOverride = true
        var completedCalled = false

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { completedCalled = true },
            tryProvideProductionToClosestCity = { }
        )

        assertTrue(completedCalled)
    }

    // -------------------------------------------------------------------------
    // Scenario 6: onImprovementCompleted NOT called when isCurrentPlayer = false
    // -------------------------------------------------------------------------

    @Test
    fun characterize_onImprovementCompleted_notCalledWhenNotCurrentPlayer() {
        tile.turnsToImprovement = 1
        tile.improvementInProgress = Constants.repair
        civInfo.isCurrentPlayerOverride = false
        var completedCalled = false

        WorkOnImprovementUseCase.execute(
            tile = tile, civInfo = civInfo, ruleset = Ruleset(), mapUnit = unit,
            onImprovementCompleted = { completedCalled = true },
            tryProvideProductionToClosestCity = { }
        )

        assertFalse(completedCalled)
    }
}
