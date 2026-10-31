package com.unciv.testing.victory

import com.unciv.testing.city.CityGameMigrationTest
import com.unciv.testing.city.CityGamePersistenceTest
import com.unciv.testing.city.CityOwnershipCharacterizationTest
import com.unciv.testing.city.NeutralCityBattleOutcomeTest
import com.unciv.testing.city.NeutralCityRoundScheduleTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Регрессия городских контрактов, затронутых сохранением результата и остановкой цепочки ходов. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CityGameMigrationTest::class,
    CityGamePersistenceTest::class,
    CityOwnershipCharacterizationTest::class,
    NeutralCityBattleOutcomeTest::class,
    NeutralCityRoundScheduleTest::class
)
class VictoryRegressionSuite
