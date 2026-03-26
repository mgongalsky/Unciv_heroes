package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Characterization tests for MapUnit seams around UncivGame.Current global dependency.
 *
 * Goal: verify that open methods exist as seams so tests can override them
 * without pulling in UncivGame.Current.
 *
 * These tests do NOT test the real implementation of the methods —
 * they test that the seam is in place and overridable.
 */
class MapUnitGlobalSeamsCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
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
