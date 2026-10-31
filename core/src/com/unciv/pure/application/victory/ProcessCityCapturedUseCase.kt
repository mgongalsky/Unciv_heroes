package com.unciv.pure.application.victory

import com.unciv.pure.domain.victory.CaptureCityObjective

/** Проверяет завершённый захват по снимку мира; не меняет владение, результат партии или интерфейс. */
object ProcessCityCapturedUseCase {
    fun execute(
        capturedCityId: String,
        capturingPlayerId: String,
        capturingSlotId: String,
        objectives: List<CaptureCityObjective>,
        cityOwners: Map<String, String?>,
        enabledVictoryTypes: Collection<String>,
        alreadyWon: Boolean
    ): Boolean {
        if (alreadyWon || CaptureCityObjective.VICTORY_TYPE !in enabledVictoryTypes) return false
        // Вызов после постороннего захвата не превращает начальное владение или торговлю в победу.
        if (capturedCityId.isBlank() || objectives.none {
                it.cityId == capturedCityId && capturingSlotId in it.eligibleSlotIds
            }) return false
        return CaptureCityObjective.isCompleted(objectives, capturingSlotId, capturingPlayerId, cityOwners)
    }
}
