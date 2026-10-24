package com.unciv.testing.pure.application.battle

import com.unciv.logic.battle.BattleDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BattleDiagnosticsTest {
    @Test
    fun `disabled diagnostics neither build nor emit messages`() {
        var evaluated = false
        val lines = mutableListOf<String>()
        val diagnostics = BattleDiagnostics({ false }, { lines.add(it) })
        diagnostics.log { evaluated = true; "unused" }
        assertFalse(evaluated)
        assertEquals(emptyList<String>(), lines)
    }

    @Test
    fun `enabled diagnostics emit one labeled message and can be disabled again`() {
        var enabled = true
        val lines = mutableListOf<String>()
        val diagnostics = BattleDiagnostics({ enabled }, { lines.add(it) })
        diagnostics.log { "attack: raw=200 soldiers=175 formation=60->0" }
        enabled = false
        diagnostics.log { error("Disabled messages must not be evaluated") }
        assertEquals(listOf("[Battle] attack: raw=200 soldiers=175 formation=60->0"), lines)
    }
}
