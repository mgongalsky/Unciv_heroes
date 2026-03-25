package com.unciv.pure.logic

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.pure.helpers.FakeCivilizationInfo
import org.junit.Before
import org.junit.Test

class FakeArmyInfo : ArmyInfo(FakeCivilizationInfo(), maxSlots = 1)

class TestableMapUnit : MapUnit() {
    override fun createArmy(): ArmyInfo = FakeArmyInfo()
}

class MapUnitCreateTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
    }

    @Test
    fun testCreate() {
        val unit = TestableMapUnit()
    }
}
