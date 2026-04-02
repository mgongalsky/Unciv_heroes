package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapUnitGlobalSeamsCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())

        val fakeRuleset = Ruleset().apply {
            val unitTypeObj = UnitType().apply { name = "Melee" }
            unitTypes["Melee"] = unitTypeObj
            val spearman = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
            }
            units["Spearman"] = spearman
        }

        startKoin {
            allowOverride(true)
            modules(module { single { fakeRuleset } })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun characterize_onImprovementCompleted_isOverridable() {
        var called = false
        val unit = object : TestableMapUnit() {
            override fun onImprovementCompleted() {
                called = true
            }
        }
        unit.onImprovementCompleted()
        assertTrue(called)
    }
}
