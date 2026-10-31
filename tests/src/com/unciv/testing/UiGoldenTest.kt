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
}
