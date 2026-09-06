package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

class UiGoldenTest {
    @Test
    fun battleTroops() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-troops"))
    }

    @Test
    fun battleTurnQueue() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-turn-queue"))
    }

    @Test
    fun battleTroopInfo() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-troop-info"))
    }

    @Test
    fun battleThreatPreview() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-threat-preview"))
    }

    @Test
    fun battleResultAttackerVictory() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-result-attacker-victory"))
    }

    @Test
    fun battleTileHighlights() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-tile-highlights"))
    }
}
