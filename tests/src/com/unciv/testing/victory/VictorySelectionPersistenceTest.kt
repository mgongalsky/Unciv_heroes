package com.unciv.testing.victory

import com.unciv.json.json
import com.unciv.models.metadata.GameParameters
import org.junit.Assert.*
import org.junit.Test

/** Проверяет совместимость пользовательского выбора и флагов, используемых сценарным запуском. */
class VictorySelectionPersistenceTest {
    /** Явно пустой выбор переживает копирование и JSON, не превращаясь в первоначальные defaults. */
    @Test fun explicitEmptySelectionSurvivesCloneAndJson() {
        val parameters = GameParameters().apply { victorySelectionInitialized = true }
        val copy = parameters.clone()
        val restored = json().fromJson(GameParameters::class.java, json().toJson(copy))
        assertTrue(copy.victorySelectionInitialized)
        assertTrue(restored.victorySelectionInitialized)
        assertTrue(restored.victoryTypes.isEmpty())
        copy.victoryTypes.add("CaptureCity")
        assertTrue(parameters.victoryTypes.isEmpty())
    }

    /** Копирование для применения политики не сбрасывает ранее выбранные параметры игры. */
    @Test fun cloningPreservesAllGameplayFlags() {
        val parameters = GameParameters().apply {
            noCityRazing = true
            noBarbarians = true
            ragingBarbarians = true
            oneCityChallenge = true
            godMode = true
            nuclearWeaponsEnabled = false
            religionEnabled = false
            espionageEnabled = true
            noStartBias = true
            isOnlineMultiplayer = true
            anyoneCanSpectate = false
        }
        val copy = parameters.clone()
        assertTrue(copy.noCityRazing && copy.noBarbarians && copy.ragingBarbarians && copy.oneCityChallenge)
        assertTrue(copy.godMode && copy.espionageEnabled && copy.noStartBias && copy.isOnlineMultiplayer)
        assertFalse(copy.nuclearWeaponsEnabled || copy.religionEnabled || copy.anyoneCanSpectate)
    }
}
