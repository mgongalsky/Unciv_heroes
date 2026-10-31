package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

/** Проверяет производственные формы в настоящей GL-сессии без утверждения визуальных эталонов. */
class ScenarioStoryEditorRenderTest {
    @Test
    fun applyRejectAndReopenDrafts() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=hero-editor", "--scenario=story-editor"))
    }
}
