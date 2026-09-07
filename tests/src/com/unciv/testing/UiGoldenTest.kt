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

    @Test
    fun splitTroopEmpty() {
        UiGoldenTestRunner.main(arrayOf("--scenario=split-troop-empty"))
    }

    @Test
    fun splitTroopOccupied() {
        UiGoldenTestRunner.main(arrayOf("--scenario=split-troop-occupied"))
    }

    @Test
    fun armyTroopInfo() {
        UiGoldenTestRunner.main(arrayOf("--scenario=army-troop-info"))
    }

    @Test
    fun arena() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--scenario=arena-start",
                "--scenario=arena-retry",
                "--scenario=arena-complete"
            )
        )
    }
}
