package com.unciv.testing.city

import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.CitySaveValidation
import com.unciv.models.ruleset.CityEvent
import org.junit.Assert.*
import org.junit.Test

/** Проверяет ссылки сохранённого строительства на определения выбранного набора правил. */
class CityDefinitionValidationTest {
    /** Разрешены здания, юниты, события, бесконечные проекты и пустой элемент очереди. */
    @Test
    fun acceptsBuildingsUnitsEventsPerpetualProjectsAndEmptyQueueEntry() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            val eventName = "Validation test event"
            fixture.ruleset.cityEvents[eventName] = CityEvent()
            city.cityConstructions.preparedCityEvents.add(eventName)
            city.cityConstructions.constructionQueue.addAll(listOf("Granary", "Scout", eventName, "Nothing", ""))
            city.cityConstructions.inProgressConstructions["Granary"] = 23
            CitySaveValidation.validateDefinitions(listOf(city), fixture.ruleset)
            assertEquals(23, city.cityConstructions.inProgressConstructions["Granary"])
            assertEquals(5, city.cityConstructions.constructionQueue.size)
        }
    }

    /** Неизвестные определения дают ошибку с ID города, но не удаляют здания, очередь или накопленное производство. */
    @Test
    fun rejectsUnknownDefinitionsWithCityIdWithoutDiscardingData() {
        CityTestFixture().use { fixture ->
            val cases = listOf<Pair<String, (CityInfo) -> Unit>>(
                "unknown building" to { it.cityConstructions.builtBuildings.add("Missing definition"); Unit },
                "unknown city event" to { it.cityConstructions.preparedCityEvents.add("Missing definition"); Unit },
                "unknown queued construction" to { it.cityConstructions.constructionQueue.add("Missing definition"); Unit },
                "unknown construction with stored production" to {
                    it.cityConstructions.inProgressConstructions["Missing definition"] = 23
                }
            )
            for ((diagnostic, corrupt) in cases) {
                val city = CityInfo().apply { id = "validation-city" }
                corrupt(city)
                val failure = try {
                    CitySaveValidation.validateDefinitions(listOf(city), fixture.ruleset)
                    null
                } catch (error: IllegalArgumentException) {
                    error
                }
                assertNotNull(diagnostic, failure)
                val message = failure!!.message.orEmpty()
                assertTrue(message, message.contains(diagnostic))
                assertTrue(message, message.contains(city.id))
                assertTrue(message, message.contains("Missing definition"))
                val constructions = city.cityConstructions
                // Проверка сообщает о несовместимости, сохраняя проблемные данные для исправления.
                assertTrue("Validation must preserve invalid data for diagnosis",
                    "Missing definition" in constructions.builtBuildings ||
                        "Missing definition" in constructions.preparedCityEvents ||
                        "Missing definition" in constructions.constructionQueue ||
                        constructions.inProgressConstructions["Missing definition"] == 23)
            }
        }
    }
}
