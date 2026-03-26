package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FakeArmyInfo : ArmyInfo(FakeCivilizationInfo(), maxSlots = 1)

class MapUnitCreateCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        val fakeRuleset = Ruleset().apply {
            val warrior = BaseUnit().apply { name = "Warrior" }
            units["Warrior"] = warrior
        }
        startKoin {
            allowOverride(true)
            modules(module {
                single { fakeRuleset }
            })
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testCreate() {
        val unit = TestableMapUnit()
    }

    @Test
    fun testCreateWithAmountAndName() {
        val unit = object : MapUnit(1, "Warrior") {
            override fun createArmy() = FakeArmyInfo()
        }
        assert(unit.name == "Warrior")
        assert(unit.amount == 1)
    }
}
