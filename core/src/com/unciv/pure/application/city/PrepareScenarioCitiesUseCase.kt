package com.unciv.pure.application.city

import com.unciv.pure.application.scenario.ValidateScenarioUseCase

import com.unciv.pure.application.scenario.ScenarioPlayerBindings

import com.unciv.pure.domain.city.*

/** Готовит стартовые данные миссии без публикации городов в игровом мире. */
object PrepareScenarioCitiesUseCase {
    data class Input(
        val cities: List<ScenarioCity>,
        val slots: List<ScenarioPlayerSlot>,
        val mapPositions: Set<CityPosition>,
        val participants: List<Pair<String, String>>
    )

    /** description принадлежит результату; его изменения не затрагивают авторскую карту. */
    data class PreparedCity(
        val description: ScenarioCity,
        val ownerId: String?,
        val resolvedType: String
    )

    fun execute(input: Input): List<PreparedCity> {
        ValidateScenarioUseCase.execute(input.cities, input.slots, input.mapPositions)
        val bindings = ScenarioPlayerBindings.resolve(input.slots, input.participants)
        require(bindings.values.none { it.isBlank() }) { "Scenario participant has an empty ID" }
        require(bindings.values.distinct().size == bindings.size) {
            "A participant occupies multiple scenario slots"
        }
        return ScenarioCityCapitals.resolve(input.cities).map { city ->
            val owner = if (city.owner == ScenarioCityOwner.PlayerSlot)
                bindings.getValue(city.ownerSlotId) else null
            val type = when (city.typeMode) {
                ScenarioCityTypeMode.Fixed -> city.fixedType
                ScenarioCityTypeMode.FromStartingPlayer -> requireNotNull(owner)
            }
            PreparedCity(city, owner, type)
        }
    }
}
