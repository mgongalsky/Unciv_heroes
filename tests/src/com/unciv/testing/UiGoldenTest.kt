package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

class UiGoldenTest {
    @Test
    fun battleTroops() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-troops"))
    }

    @Test
    fun battleResultAttackerVictory() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-result-attacker-victory"))
    }
}
