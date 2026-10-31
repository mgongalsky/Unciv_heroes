package com.unciv.testing.victory

import com.unciv.json.json
import com.unciv.logic.GameInfo
import com.unciv.logic.MapSaver
import com.unciv.pure.domain.city.*
import com.unciv.pure.domain.scenario.*
import com.unciv.pure.domain.victory.CaptureCityObjective
import com.unciv.testing.city.CityTestFixture
import org.junit.Assert.*
import org.junit.Test

/** Проверяет реальные границы сериализации карты и партии отдельно от чистых правил. */
class VictoryPersistenceTest {
    private fun settings() = ScenarioVictorySettings(
        rules = arrayListOf(ScenarioVictoryRule("CaptureCity", ScenarioVictoryMode.Required)),
        captureObjectives = arrayListOf(CaptureCityObjective("target", arrayListOf("one")))
    )

    /** JSON и сжатая карта сохраняют настройки и цели без создания игровых городов. */
    @Test fun mapSettingsRoundTripInBothFormats() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(ScenarioCity(id = "target", name = "Fort",
                position = CityPosition(0f, 0f), typeMode = ScenarioCityTypeMode.Fixed, fixedType = "Castle"))
            map.scenarioPlayerSlots.add(ScenarioPlayerSlot("one"))
            map.scenarioVictorySettings = settings()
            val previousZip = MapSaver.saveZipped
            try {
                for (zipped in listOf(false, true)) {
                    MapSaver.saveZipped = zipped
                    val loaded = MapSaver.mapFromSavedString(MapSaver.mapToSavedString(map), false)
                    assertEquals(map.scenarioVictorySettings, loaded.scenarioVictorySettings)
                    loaded.scenarioVictorySettings!!.captureObjectives.single().eligibleSlotIds.clear()
                    assertEquals(listOf("one"), map.scenarioVictorySettings!!.captureObjectives.single().eligibleSlotIds)
                    assertEquals(0, fixture.game.getCities().count())
                }
            } finally { MapSaver.saveZipped = previousZip }
        }
    }

    /** Результат и назначения слотов переживают сохранение партии без повторной проверки захвата. */
    @Test fun gameResultAndParticipantsRoundTrip() {
        CityTestFixture().use { fixture ->
            fixture.game.tileMap.scenarioVictorySettings = settings()
            fixture.game.scenarioVictoryParticipants["one"] = "Rome"
            fixture.game.victoryWinner = "Rome"
            fixture.game.victoryType = "CaptureCity"
            fixture.game.oneMoreTurnMode = true
            val loaded = json().fromJson(GameInfo::class.java, json().toJson(fixture.game))
            assertEquals("Rome", loaded.victoryWinner)
            assertEquals("CaptureCity", loaded.victoryType)
            assertEquals(mapOf("one" to "Rome"), loaded.scenarioVictoryParticipants)
            assertEquals(settings(), loaded.tileMap.scenarioVictorySettings)
            assertTrue(loaded.oneMoreTurnMode)
        }
    }

    /** Копия партии не разделяет изменяемые настройки и таблицу назначений с оригиналом. */
    @Test fun gameCloneSeparatesVictorySettingsAndParticipants() {
        CityTestFixture().use { fixture ->
            fixture.game.tileMap.scenarioVictorySettings = settings()
            fixture.game.scenarioVictoryParticipants["one"] = "Rome"
            fixture.game.victoryWinner = "Rome"
            fixture.game.victoryType = "CaptureCity"
            val copy = fixture.game.clone()
            assertEquals("Rome", copy.victoryWinner)
            assertEquals("CaptureCity", copy.victoryType)
            copy.scenarioVictoryParticipants["one"] = "Greece"
            copy.tileMap.scenarioVictorySettings!!.rules.single().mode = ScenarioVictoryMode.Forbidden
            copy.tileMap.scenarioVictorySettings!!.captureObjectives.single().eligibleSlotIds.clear()
            assertEquals("Rome", fixture.game.scenarioVictoryParticipants["one"])
            assertEquals(settings(), fixture.game.tileMap.scenarioVictorySettings)
        }
    }

    /** Карта прежнего формата без новых полей не получает ограничений или победителя. */
    @Test fun legacyMapHasNoVictoryRestrictions() {
        val loaded = MapSaver.mapFromSavedString("""{"mapParameters":{"mapFormatVersion":2},"tileList":[]}""", false)
        assertNull(loaded.scenarioVictorySettings)
        val game = GameInfo()
        assertEquals("", game.victoryWinner)
        assertEquals("", game.victoryType)
        assertTrue(game.scenarioVictoryParticipants.isEmpty())
    }
}
