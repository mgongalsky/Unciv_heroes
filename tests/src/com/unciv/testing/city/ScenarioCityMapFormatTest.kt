package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.MapFormatMigration
import com.unciv.logic.MapSaver
import com.unciv.logic.UncivShowableException
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет хранение описаний городов в карте без создания действующих игровых городов. */
class ScenarioCityMapFormatTest {
    /** Заполняет разные поля описания, чтобы их потеря при чтении или копировании была заметна. */
    private fun city() = ScenarioCity(
        id = "town-1", position = CityPosition(2f, 0f), name = "Riverford",
        owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "player-1",
        population = 3, foodStored = 17, buildings = arrayListOf("Granary"),
        garrison = arrayListOf(ScenarioCityTroop("Spearman", 5)),
        territory = arrayListOf(CityPosition(2f, 0f), CityPosition(3f, 0f)),
        isCapital = true, isOriginalCapital = true
    )

    /** Оба формата карты сохраняют описания и слоты; чтение и превью не запускают основание городов. */
    @Test
    fun descriptionsRoundTripInBothFormatsWithoutCreatingCities() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("player-1", "First player"))
            map.scenarioCities.add(city())
            val previousZip = MapSaver.saveZipped
            try {
                for (zipped in listOf(false, true)) {
                    MapSaver.saveZipped = zipped
                    val saved = MapSaver.mapToSavedString(map)
                    val loaded = MapSaver.mapFromSavedString(saved, false)
                    assertEquals(map.scenarioCities, loaded.scenarioCities)
                    assertEquals(map.scenarioPlayerSlots, loaded.scenarioPlayerSlots)
                    assertEquals(MapFormatMigration.currentVersion,
                        MapSaver.mapParametersFromSavedString(saved).mapFormatVersion)
                    ScenarioCityValidation.validate(loaded.scenarioCities, loaded.scenarioPlayerSlots,
                        loaded.values.map { CityPosition(it.position.x, it.position.y) }.toSet())
                    assertFalse(loaded.values.any { it.isCityCenter() })
                    assertEquals(0, fixture.game.getCities().count())
                }
            } finally {
                MapSaver.saveZipped = previousZip
            }
        }
    }

    /** Изменения вложенных списков, стеков гарнизона и слотов копии не затрагивают исходную карту. */
    @Test
    fun clonedDescriptionsDoNotShareMutableCollectionsOrStacks() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(city())
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("player-1", "First player"))
            val copy = map.clone()
            copy.scenarioCities.single().apply {
                name = "Changed"
                buildings.clear()
                territory.clear()
                garrison.single().amount = 99
            }
            copy.scenarioPlayerSlots.single().name = "Changed"
            assertEquals(city(), map.scenarioCities.single())
            assertEquals("First player", map.scenarioPlayerSlots.single().name)
        }
    }

    /** Старая карта получает пустые коллекции сценария; неизвестная будущая версия явно отклоняется. */
    @Test
    fun legacyMapsHaveNoDescriptionsAndFutureVersionIsRejected() {
        val legacy = """{"mapParameters":{"mapFormatVersion":1},"tileList":[]}"""
        val loaded = MapSaver.mapFromSavedString(legacy, false)
        assertTrue(loaded.scenarioCities.isEmpty())
        assertTrue(loaded.scenarioPlayerSlots.isEmpty())
        val future = """{"mapParameters":{"mapFormatVersion":${MapFormatMigration.currentVersion + 1}},"tileList":[]}"""
        try {
            MapSaver.mapFromSavedString(future, false)
            fail("Future format must be rejected")
        } catch (expected: UncivShowableException) {
            assertTrue(expected.message.orEmpty().contains("format version"))
        }
    }

    /** Описания обязаны ссылаться на существующие слоты и не могут делить одну территориальную клетку. */
    @Test
    fun missingSlotAndConflictingTerritoryAreRejected() {
        val positions = setOf(CityPosition(2f, 0f), CityPosition(3f, 0f))
        fun reject(cities: List<ScenarioCity>, slots: List<ScenarioPlayerSlot>, text: String) {
            try {
                ScenarioCityValidation.validate(cities, slots, positions)
                fail("Invalid scenario must be rejected")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty(), expected.message.orEmpty().contains(text))
            }
        }
        reject(listOf(city()), emptyList(), "unknown player slot")
        val other = city().copy(id = "town-2", position = CityPosition(3f, 0f),
            isCapital = false, isOriginalCapital = false, territory = arrayListOf(CityPosition(3f, 0f)))
        reject(listOf(city(), other), listOf(ScenarioPlayerSlot("player-1")), "already claimed")
    }
}
