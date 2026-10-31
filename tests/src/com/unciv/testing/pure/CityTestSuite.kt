package com.unciv.testing.pure

import com.unciv.testing.pure.application.city.PrepareScenarioCitiesUseCaseTest
import com.unciv.testing.pure.application.scenario.*
import com.unciv.testing.pure.application.victory.ProcessCityCapturedUseCaseTest
import com.unciv.testing.pure.domain.army.CityArmyCopyTest
import com.unciv.testing.pure.domain.city.*
import com.unciv.testing.pure.domain.victory.CaptureCityObjectiveTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Чистые проверки городов, сценарных целей и подготовки запуска без движка, DI и файлов. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CityStateTest::class, CityRegistryTest::class, CityArmyCopyTest::class,
    CityPopulationRulesTest::class, NeutralCityEntryRulesTest::class,
    ScenarioCityCapitalsTest::class, ScenarioCityRulesTest::class,
    ScenarioPlayerSlotDefaultsTest::class, ScenarioPlayerBindingsTest::class,
    ScenarioCityValidationTest::class, PrepareScenarioCitiesUseCaseTest::class,
    CaptureCityObjectiveTest::class, ProcessCityCapturedUseCaseTest::class,
    VictoryOptionsTest::class, ScenarioVictoryValidationTest::class
)
class CityTestSuite
