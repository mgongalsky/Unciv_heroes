package com.unciv.testing.pure.logic.mapunit

import com.unciv.logic.map.MapUnit
import com.unciv.models.ruleset.unique.IHasUniques
import com.unciv.models.ruleset.unique.Unique
import com.unciv.models.ruleset.unique.UniqueTarget
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableMapUnit
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MapUnitSkillsCharTest {

    @Before
    fun setUp() {
        MapUnit.setTestingInstance(FakeCivilizationInfo())
    }

    @Test
    fun characterize_updateSkills_noBoosts() {
        val unit = TestableMapUnit()

        // morale starts at 3 (default)
        assertEquals(3, unit.morale)

        // updateSkills() resets morale to 0 when no boosts present
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

        // TODO: morale default is 3 but updateSkills() resets it to 0 before summing boosts.
        // This means calling updateSkills() without boosts destroys the default value.
        // Probably updateSkills() should be called only when boosts change, not on every access.
        assertEquals(3, unit.morale)
        unit.addBoost("test", sourceObject = fakeSource)
        unit.updateSkills()
        assertEquals(5, unit.morale)
    }
}
