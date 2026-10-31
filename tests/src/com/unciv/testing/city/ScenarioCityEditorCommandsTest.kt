package com.unciv.testing.city

import com.unciv.logic.MapSaver
import com.unciv.logic.map.ScenarioCityEditorCommands
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет команды изменения описаний карты независимо от формы редактора и создания игровых городов. */
class ScenarioCityEditorCommandsTest {
    /** Несовместимые после смены правил города можно исправлять по одному, сохраняя остальные описания. */
    @Test
    fun incompatibleCitiesCanBeRepairedOneAtATime() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(city().apply { buildings.add("Missing building") })
            map.scenarioCities.add(city().apply {
                id = "other"
                position = CityPosition(3f, 0f)
                buildings.add("Missing building")
            })
            val repaired = map.scenarioCities.first().deepCopy().apply {
                buildings.remove("Missing building")
            }
            assertTrue(ScenarioCityEditorCommands.putCity(map, fixture.ruleset, repaired))
            assertTrue("Missing building" in map.scenarioCities.last().buildings)
            assertEquals(2, map.scenarioCities.size)
        }
    }

    /** Гарнизон сверх вместимости армии отклоняется до добавления описания в карту. */
    @Test
    fun oversizedGarrisonIsRejectedWithoutChangingTheMap() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            val draft = city().apply {
                garrison = ArrayList((0..com.unciv.models.GameConstants.armySize).map {
                    ScenarioCityTroop("Spearman", 5)
                })
            }
            try {
                ScenarioCityEditorCommands.putCity(map, fixture.ruleset, draft)
                fail("Oversized garrison must be rejected")
            } catch (_: IllegalArgumentException) { }
            assertTrue(map.scenarioCities.isEmpty())
        }
    }

    /** Корректный нейтральный черновик со зданием и одним стеком для проверки копирования вложенных данных. */
    private fun city() = ScenarioCity(id = "town", name = "Town",
        typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Default",
        buildings = arrayListOf("Granary"), garrison = arrayListOf(ScenarioCityTroop("Spearman", 5)))

    /** Повторный Apply не дублирует город; сохранённое описание не разделяет изменяемый гарнизон с черновиком. */
    @Test
    fun singleCommandsCopyDraftAndDoNotDoubleRegister() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            val draft = city()
            assertTrue(ScenarioCityEditorCommands.putCity(map, fixture.ruleset, draft))
            assertFalse(ScenarioCityEditorCommands.putCity(map, fixture.ruleset, draft))
            draft.garrison.single().amount = 99
            assertEquals(5, map.scenarioCities.single().garrison.single().amount)
            val update = map.scenarioCities.single().deepCopy().apply { name = "Updated" }
            assertTrue(ScenarioCityEditorCommands.putCity(map, fixture.ruleset, update))
            assertEquals(1, map.scenarioCities.size)
            assertEquals(0, fixture.game.getCities().count())
            assertTrue(ScenarioCityEditorCommands.removeCity(map, "town"))
            assertFalse(ScenarioCityEditorCommands.removeCity(map, "town"))
        }
    }

    /** Неверное население и удаление используемого слота отклоняются без изменения прежнего описания. */
    @Test
    fun invalidDraftAndUsedSlotRemovalLeaveMapUnchanged() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            ScenarioCityEditorCommands.putSlot(map, ScenarioPlayerSlot("first", "First player"))
            val draft = city().apply { owner = ScenarioCityOwner.PlayerSlot; ownerSlotId = "first" }
            ScenarioCityEditorCommands.putCity(map, fixture.ruleset, draft)
            val before = map.scenarioCities.single().deepCopy()
            try {
                ScenarioCityEditorCommands.putCity(map, fixture.ruleset,
                    draft.deepCopy().apply { population = 0 })
                fail("Invalid population must be rejected")
            } catch (_: IllegalArgumentException) { }
            try {
                ScenarioCityEditorCommands.removeSlot(map, "first")
                fail("Referenced slot must be retained")
            } catch (_: IllegalArgumentException) { }
            assertEquals(before, map.scenarioCities.single())
            assertEquals(1, map.scenarioPlayerSlots.size)
        }
    }

    /** Применённые описания переживают копирование для сохранения и повторное открытие карты. */
    @Test
    fun editedDescriptionsSurviveSaveCloneAndReopening() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            ScenarioCityEditorCommands.putCity(map, fixture.ruleset, city())
            val cloneForSave = map.clone().apply { setTransients(setUnitCivTransients = false) }
            val previousZip = MapSaver.saveZipped
            try {
                MapSaver.saveZipped = false
                val loaded = MapSaver.mapFromSavedString(MapSaver.mapToSavedString(cloneForSave), false)
                assertEquals(map.scenarioCities, loaded.scenarioCities)
                loaded.scenarioCities.single().buildings.clear()
                assertEquals(listOf("Granary"), map.scenarioCities.single().buildings)
            } finally { MapSaver.saveZipped = previousZip }
        }
    }
}
