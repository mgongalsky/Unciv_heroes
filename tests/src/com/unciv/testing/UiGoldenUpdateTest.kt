package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

/** Explicitly regenerates platform-specific UI golden baselines. Run intentionally, never as regression verification. */
class UiGoldenUpdateTest {
    @Test
    fun battleTroops() {
        UiGoldenTestRunner.main(arrayOf("--update", "--scenario=battle-troops"))
    }

    @Test
    fun battleResultAttackerVictory() {
        UiGoldenTestRunner.main(arrayOf("--update", "--scenario=battle-result-attacker-victory"))
    }
}
