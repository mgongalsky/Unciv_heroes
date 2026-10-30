package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

/** Checks the form and nested editor tools in one GL session without approving a baseline. */
class ScenarioCityEditorRenderTest {
    @Test
    fun applyRenderNestedToolsAndRecreation() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-editor", "--scenario=city-editor-tabs", "--scenario=scenario-player-slots"))
    }
}
