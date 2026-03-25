package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.Before
import org.junit.Test

class FakeArmyInfo : ArmyInfo(FakeCivilizationInfo(), maxSlots = 1)



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
