package com.unciv.testing.victory

import com.unciv.logic.map.ScenarioCityEditorCommands
import com.unciv.logic.map.ScenarioVictoryEditorCommands
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет публикацию черновика и сохранение ссылочной целостности команд редактора. */
class ScenarioVictoryEditorCommandsTest {
    /** Применённые настройки отделены от черновика; отказ сохраняет прежнюю настройку. */
    @Test fun applyCopiesDraftAndRejectsInvalidChangesAtomically() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            val type = fixture.ruleset.victories.keys.first()
            val draft = ScenarioVictorySettings(rules = arrayListOf(ScenarioVictoryRule(type)))
            assertTrue(ScenarioVictoryEditorCommands.apply(map, fixture.ruleset, draft))
            val before = map.scenarioVictorySettings!!.deepCopy()
            draft.rules.single().victoryType = "Missing"
            try {
                ScenarioVictoryEditorCommands.apply(map, fixture.ruleset, draft)
                fail("Unknown victory must be rejected")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, map.scenarioVictorySettings)
            assertFalse(ScenarioVictoryEditorCommands.apply(map, fixture.ruleset, before))
        }
    }

    /** Город и слот защищены целью даже при нейтральном начальном владельце города. */
    @Test fun referencedCityAndSlotCannotBeDeleted() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(ScenarioCity(id = "target", name = "Fort",
                typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            map.scenarioVictorySettings = ScenarioVictorySettings(captureObjectives = arrayListOf(
                CaptureCityObjective("target", arrayListOf("one"))
            ))
            try {
                ScenarioCityEditorCommands.removeCity(map, "target")
                fail("Referenced city must remain")
            } catch (_: IllegalArgumentException) { }
            try {
                ScenarioCityEditorCommands.removeSlot(map, "one")
                fail("Referenced slot must remain")
            } catch (_: IllegalArgumentException) { }
            assertEquals("target", map.scenarioCities.single().id)
            assertEquals("one", map.scenarioPlayerSlots.single().id)
            map.scenarioVictorySettings = null
            assertTrue(ScenarioCityEditorCommands.removeCity(map, "target"))
            assertTrue(ScenarioCityEditorCommands.removeSlot(map, "one"))
        }
    }
}
