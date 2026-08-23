package com.unciv.testing.pure.domain.troop

import com.unciv.pure.domain.troop.Formation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormationTest {
    @Test
    fun `formation capacity is proportional to soldiers`() {
        val formation = Formation.forSoldiers(12)

        assertEquals(120, formation.maximum)
        assertEquals(120, formation.current)
        assertEquals(1f, formation.fraction, 0f)
        assertFalse(formation.isBroken)
    }

    @Test
    fun `zero soldiers create a broken empty formation`() {
        val formation = Formation.forSoldiers(0)

        assertEquals(0, formation.maximum)
        assertEquals(0, formation.current)
        assertEquals(0f, formation.fraction, 0f)
        assertTrue(formation.isBroken)
    }

    @Test
    fun `normalization clamps malformed serialized values`() {
        val formation = Formation(current = 150, maximum = 100)

        formation.normalize()

        assertEquals(100, formation.current)
        assertEquals(100, formation.maximum)
    }
}
