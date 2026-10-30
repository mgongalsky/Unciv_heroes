package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

/** Checks form actions and layout in real GL without approving a visual baseline. */
class ScenarioCityEditorRenderTest {
    @Test
    fun applyAndRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-editor"))
    }
}
