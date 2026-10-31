package com.unciv.testing.city

import com.unciv.json.json
import com.unciv.logic.MapSaver
import com.unciv.logic.UncivShowableException
import com.unciv.logic.map.ScenarioMapValidation
import com.unciv.pure.domain.city.*
import org.junit.Assert.*
import org.junit.Test

/** Проверяет ошибки сценария на границе загрузки карты и совместимости с выбранными правилами. */
class ScenarioCityMapValidationTest {
    /** Ссылка на отсутствующий слот вызывает понятный отказ без создания города или подмены нейтральностью. */
    @Test
    fun loaderRejectsMissingOwnerSlotInsteadOfMakingCityNeutral() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(ScenarioCity(id = "broken", name = "Broken",
                owner = ScenarioCityOwner.PlayerSlot, ownerSlotId = "missing"))
            try {
                MapSaver.mapFromSavedString(json().toJson(map), false)
                fail("Missing slot must be rejected")
            } catch (expected: UncivShowableException) {
                assertTrue(expected.message.orEmpty().contains("unknown player slot missing"))
            }
            assertEquals(ScenarioCityOwner.PlayerSlot, map.scenarioCities.single().owner)
            assertEquals(0, fixture.game.getCities().count())
        }
    }

    /** Структурно корректный город может быть несовместим с правилами; отсутствующие определения сообщаются без удаления. */
    @Test
    fun rulesetCompatibilityReportsMissingBuildingsAndTroops() {
        CityTestFixture().use { fixture ->
            val map = fixture.game.tileMap
            map.scenarioCities.add(ScenarioCity(id = "town", name = "Town",
                typeMode = ScenarioCityTypeMode.Fixed, fixedType = "test-type",
                buildings = arrayListOf("MissingBuilding"),
                garrison = arrayListOf(ScenarioCityTroop("MissingTroop", 4))))
            ScenarioMapValidation.validateStructure(map)
            val errors = map.getRulesetIncompatibility(fixture.ruleset)
            assertTrue(errors.any { it.contains("MissingBuilding") })
            assertTrue(errors.any { it.contains("MissingTroop") })
            assertEquals(listOf("MissingBuilding"), map.scenarioCities.single().buildings)
        }
    }
}
