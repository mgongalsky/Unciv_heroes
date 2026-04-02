package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.IHasUniques
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MapUnitSkillsCharTest {

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
    fun characterize_updateSkills_noBoosts() {
        val unit = TestableMapUnit()
        assertEquals(3, unit.morale)
        unit.updateSkills()
        assertEquals(0, unit.morale)
    }

    @Test
    fun characterize_updateSkills_withMoraleBoost() {
        val unit = TestableMapUnit()

        val fakeSource = object : IHasUniques {
            override var uniques = arrayListOf(
                "Gives bonus [5] to [morale] to [Hero] for [3] turns"
            )
            override val uniqueObjects get() = uniques.map {
                Unique(it, UniqueTarget.Triggerable, "test")
            }
            override val uniqueMap get() = uniqueObjects.groupBy { it.placeholderText }
            override fun getUniqueTarget() = UniqueTarget.Triggerable
        }

        assertEquals(3, unit.morale)
        unit.addBoost("test", sourceObject = fakeSource)
        unit.updateSkills()
        assertEquals(5, unit.morale)
    }
}
