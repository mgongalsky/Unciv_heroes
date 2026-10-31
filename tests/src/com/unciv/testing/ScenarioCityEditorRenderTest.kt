package com.unciv.testing

import com.unciv.app.desktop.UiGoldenTestRunner
import org.junit.Test

/** Проверяет форму города и связанные инструменты в одной настоящей GL-сессии без утверждения эталонов. */
class ScenarioCityEditorRenderTest {
    /** Выполняет сценарии формы, вкладок, слотов и миникарты; capture-only сохраняет снимки без golden-сравнения. */
    @Test
    fun applyRenderNestedToolsAndRecreation() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-editor", "--scenario=city-editor-tabs", "--scenario=scenario-player-slots", "--scenario=neutral-city-minimap"))
    }
}
