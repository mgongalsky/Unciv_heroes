package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.Visitable
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeTileInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapUnitVisitPlaceTest {
    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `ordinary improvement without visitable does not crash or request UI update`() {
        var uiUpdateRequested = false
        val unit = object : TestableMapUnit() {
            override fun onVisitPlace() {
                uiUpdateRequested = true
            }
        }
        val tile = FakeTileInfo().apply {
            improvement = "Farm"
            visitable = null
        }

        unit.visitPlace(tile)

        assertFalse(uiUpdateRequested)
    }

    @Test
    fun `improvement with visitable still visits and requests UI update`() {
        var uiUpdateRequested = false
        val unit = object : TestableMapUnit() {
            override fun onVisitPlace() {
                uiUpdateRequested = true
            }
        }
        val tile = FakeTileInfo().apply {
            improvement = "Farm"
            visitable = Visitable("Farm", this)
        }

        unit.visitPlace(tile)

        assertTrue(uiUpdateRequested)
    }
}
