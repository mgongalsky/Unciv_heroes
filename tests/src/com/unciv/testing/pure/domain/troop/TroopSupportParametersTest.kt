package com.unciv.testing.pure.domain.troop

import com.unciv.json.json
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.Troop
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TroopSupportParametersTest {
    private fun create(extra: String = ""): Troop {
        val unit = json().fromJson(
            BaseUnit::class.java,
            """{"name":"Custom supporting unit","health":40$extra}"""
        )
        val ruleset = Ruleset().apply { units[unit.name] = unit }
        return TroopFactory.create(unit.name, 3, RulesetTroopDefinitionSource(ruleset))
    }

    @Test
    fun `support comes from JSON rather than unit name`() {
        val troop = create(",\"supportBonusPercent\":25")
        assertEquals(25, troop.supportBonusPercent)
        assertTrue(troop.hasSupport)
    }

    @Test
    fun `missing zero and negative configuration disable support`() {
        for (extra in listOf("", ",\"supportBonusPercent\":0", ",\"supportBonusPercent\":-25")) {
            val troop = create(extra)
            assertEquals(0, troop.supportBonusPercent)
            assertFalse(troop.hasSupport)
        }
    }

    @Test
    fun `support configuration survives serialization`() {
        val troop = create(",\"supportBonusPercent\":25")
        val serializer = json()
        val restored = serializer.fromJson(Troop::class.java, serializer.toJson(troop))
        assertEquals(25, restored.supportBonusPercent)
        assertTrue(restored.hasSupport)
    }

    @Test
    fun `old serialized troop has no implicit support`() {
        val restored = json().fromJson(Troop::class.java, """{"unitName":"Spearman","amount":3}""")
        assertEquals(0, restored.supportBonusPercent)
        assertFalse(restored.hasSupport)
    }

    @Test
    fun `shipped tactical rules enable support only for swordsman and spearman`() {
        val workingDirectory = java.io.File(System.getProperty("user.dir")).absoluteFile
        val file = generateSequence(workingDirectory) { it.parentFile }
            .flatMap { directory ->
                sequenceOf(
                    java.io.File(directory, "android/assets/jsons/Civ V - Gods & Kings/Units.json"),
                    java.io.File(directory, "jsons/Civ V - Gods & Kings/Units.json")
                )
            }
            .firstOrNull { it.isFile }
            ?: error("Cannot find tactical Units.json from $workingDirectory")
        val units = json().fromJson(Array<BaseUnit>::class.java, file.readText())
        val ruleset = Ruleset().apply { units.forEach { this.units[it.name] = it } }
        val source = RulesetTroopDefinitionSource(ruleset)
        assertEquals(
            setOf("Swordsman", "Spearman"),
            units.filter { it.supportBonusPercent > 0 }.map { it.name }.toSet()
        )
        for (name in listOf("Swordsman", "Spearman")) {
            assertEquals(25, TroopFactory.create(name, 10, source).supportBonusPercent)
        }
    }
}
