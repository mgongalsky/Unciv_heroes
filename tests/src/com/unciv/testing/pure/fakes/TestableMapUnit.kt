package com.unciv.testing.pure.fakes

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.testing.pure.logic.mapunit.FakeArmyInfo

class TestableMapUnit : MapUnit() {
    override fun createArmy(): ArmyInfo = FakeArmyInfo()
}
