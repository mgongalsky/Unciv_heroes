package com.unciv.testing.victory

import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Интеграционные проверки побед: реальные адаптеры, запуск, сохранения, редактор и границы ходов. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    VictoryLegacyCharacterizationTest::class,
    VictoryPersistenceTest::class,
    ScenarioVictoryEditorCommandsTest::class,
    CaptureCityIntegrationTest::class,
    ScenarioVictoryStartupTest::class,
    VictoryTurnFlowTest::class,
    VictorySelectionPersistenceTest::class
)
class VictoryTestSuite
