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

    @Test
    fun battleFlanking() {
        UiGoldenTestRunner.main(arrayOf("--scenario=battle-flanking"))
    }

    @Test
    fun networkBattle() {
        UiGoldenTestRunner.main(arrayOf("--scenario=network-room", "--scenario=network-battle"))
    }

    @Test
    fun armySetupLaptop() {
        UiGoldenTestRunner.main(arrayOf("--scenario=army-setup-laptop"))
    }

    /** Сравнивает снимок формы города с платформенным эталоном; отсутствие эталона не считается успехом. */
    @Test
    fun cityEditor() {
        UiGoldenTestRunner.main(arrayOf("--scenario=city-editor"))
    }

    /** Проверяет команды и размеры формы; захват кадра отдельно не утверждает визуальный эталон. */
    @Test
    fun victoryEditorRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=victory-editor"))
    }

    /** Сравнивает форму побед с платформенным эталоном. */
    @Test
    fun victoryEditor() {
        UiGoldenTestRunner.main(arrayOf("--scenario=victory-editor"))
    }

    /** Проверяет реальные ограничения и выбор меню; capture-only не утверждает визуальный эталон. */
    @Test
    fun victorySelectionRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=victory-selection"))
    }

    /** Проверяет команды и размеры прежней формы без подмены отсутствующего визуального эталона. */
    @Test
    fun cityEditorRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-editor"))
    }

    /** Проверяет фоновой снимок и независимые поля; захват не утверждает визуальный эталон. */
    @Test
    fun terrainPlaygroundRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=terrain-playground"))
    }

    /** Сравнивает песочницу с просмотренным платформенным эталоном. */
    @Test
    fun terrainPlayground() {
        UiGoldenTestRunner.main(arrayOf("--scenario=terrain-playground"))
    }

    /** Проверяет реальные ползунки и Reset; захват не является сравнением с эталоном. */
    @Test
    fun terrainParametersRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=terrain-parameters"))
    }

    @Test
    fun terrainParameters() {
        UiGoldenTestRunner.main(arrayOf("--scenario=terrain-parameters"))
    }

    /** Проверяет слои, кисть, wrap и загрузку настоящего редактора; capture не утверждает baseline. */
    @Test
    fun terrainEditorLayersRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=terrain-editor-layers"))
    }

    /** Проверяет отрисовку боевых заливок после выделения общего Drawable; baseline не обновляется. */
    @Test
    fun battleHexFillRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=battle-flanking",
                "--scenario=battle-tile-highlights"
            )
        )
    }

    /** Сравнивает производственное окно сюжетного выбора с платформенным эталоном. */
    @Test
    fun storyDialogue() {
        UiGoldenTestRunner.main(arrayOf("--scenario=story-dialogue"))
    }

    /** Сравнивает формы героев и сюжета с платформенными эталонами. */
    @Test
    fun storyEditors() {
        UiGoldenTestRunner.main(arrayOf("--scenario=hero-editor", "--scenario=story-editor"))
    }

    /** Проверяет рендеринг окон армии; захват кадров не утверждает визуальные эталоны. */
    @Test
    fun armyDialogsRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=split-troop-empty",
                "--scenario=split-troop-occupied",
                "--scenario=army-troop-info"
            )
        )
    }

    /** Повторное создание приложения не должно использовать остановленный GL-диспетчер. */
    @Test
    fun repeatedApplicationRender() {
        repeat(2) {
            UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=army-troop-info"))
        }
    }

    /** Проверяет создание экранов с вкладками; визуальные эталоны не обновляются. */
    @Test
    fun tabbedEditorsRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=city-editor-tabs",
                "--scenario=scenario-player-slots"
            )
        )
    }

    /** Захватывает окно результата боя без изменения или утверждения эталона. */
    @Test
    fun battleResultRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=battle-result-attacker-victory"
            )
        )
    }

    /** Проверяет узкую/широкую разметку, длинные потери, прокрутку и Continue без обновления эталона. */
    @Test
    fun battleResultLayoutRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=battle-result-layout"))
    }

    /** Рендерит характеристики и прогноз боя; визуальные эталоны не изменяются. */
    @Test
    fun battleInformationRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=battle-troop-info",
                "--scenario=battle-threat-preview"
            )
        )
    }

    /** Проверяет настоящий обзор героя и пересоздание экрана без обновления визуального эталона. */
    @Test
    fun heroOverviewRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=hero-overview"))
    }

    /** Сравнивает пиксели повторных запусков между собой, не утверждая визуальный эталон. */
    @Test
    fun repeatedLegacyBackgroundRender() {
        val root = java.io.File(System.getProperty("golden.projectRoot") ?: "../..")
        val platform = System.getProperty("os.name").lowercase()
            .replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val actual = java.io.File(root, "tests/golden/results/$platform/battle-troops-actual.png")
        var firstPixels: IntArray? = null
        var firstWidth = 0
        var firstHeight = 0
        repeat(3) { iteration ->
            UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=battle-troops"))
            val image = checkNotNull(javax.imageio.ImageIO.read(actual))
            val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            if (iteration == 0) {
                firstPixels = pixels
                firstWidth = image.width
                firstHeight = image.height
            } else {
                check(image.width == firstWidth && image.height == firstHeight)
                check(pixels.contentEquals(checkNotNull(firstPixels))) {
                    "battle-troops pixels changed between application launches 1 and ${iteration + 1}"
                }
            }
        }
    }

    /** Checks confirmation layout and callbacks without updating visual baselines. */
    @Test
    fun confirmPopupRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=confirm-popup-layout"))
    }

    /** Checks text prompt layout, validation and callbacks without approving a visual baseline. */
    @Test
    fun askTextPopupRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=ask-text-popup-layout"))
    }

    /** Checks number prompt layout, trade adjustments and validation without updating baselines. */
    @Test
    fun askNumberPopupRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=ask-number-popup-layout"))
    }

    /** Loads a real world and captures CityScreen without approving a visual baseline. */
    @Test
    fun cityScreenRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-screen"))
    }

    /** Проверяет границы уведомлений, прокрутку и повторное закрытие без обновления эталонов. */
    @Test
    fun toastPopupRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=toast-popup-layout"))
    }

    /** Проверяет переключение страниц и границы компактного города без утверждения эталона. */
    @Test
    fun compactCityScreenRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=city-screen-compact"))
    }

    /** Проверяет компактный город в настоящем портретном окне 360×480. */
    @Test
    fun compactCityPortraitRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only", "--scenario=city-screen-compact", "--compact-city-portrait"
            )
        )
    }

    /** Проверяет размещение статистики и обработчик конвертации без утверждения визуального эталона. */
    @Test
    fun empireOverviewStatsRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=empire-overview-stats"))
    }

    /** Проверяет границы и прокрутку главного меню; захват не утверждает визуальный эталон. */
    @Test
    fun mainMenuRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=main-menu"))
    }

    /** Проверяет главное меню в портретном окне без обновления эталонов. */
    @Test
    fun mainMenuPortraitRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=main-menu",
                "--main-menu-portrait"
            )
        )
    }

    /** Проверяет главное меню в широком окне без обновления эталонов. */
    @Test
    fun mainMenuWideRender() {
        UiGoldenTestRunner.main(
            arrayOf(
                "--capture-only",
                "--scenario=main-menu",
                "--main-menu-wide"
            )
        )
    }

    /** Проверяет освобождение страниц нативного шрифта и повторный dispose в GL-контексте. */
    @Test
    fun nativeFontLifecycleRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=native-font-lifecycle"))
    }

    /** Проверяет переключение и disabled-состояния Royal-флажков без утверждения эталона. */
    @Test
    fun royalCheckBoxRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=royal-checkbox"))
    }

    /** Проверяет ввод и disabled для обеих ориентаций Royal-ползунка. */
    @Test
    fun royalSliderRender() {
        UiGoldenTestRunner.main(arrayOf("--capture-only", "--scenario=royal-slider"))
    }
}
