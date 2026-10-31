package com.unciv.testing.pure

import com.unciv.testing.pure.application.mapgeneration.TerrainGenerationPipelineTest
import com.unciv.testing.pure.application.mapgeneration.TerrainGridFactoryTest
import com.unciv.testing.pure.application.mapgeneration.TerrainMetricsTest
import com.unciv.testing.pure.domain.mapgeneration.ClimateGeneratorTest
import com.unciv.testing.pure.domain.mapgeneration.ElevationGeneratorTest
import com.unciv.testing.pure.domain.mapgeneration.LatitudeRangeTest
import com.unciv.testing.pure.domain.mapgeneration.PerlinNoiseTest
import com.unciv.testing.pure.domain.mapgeneration.VegetationGeneratorTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/** Чистое ядро поверхности: без графики, файлов, ruleset и игрового мира. */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    PerlinNoiseTest::class,
    ClimateGeneratorTest::class,
    ElevationGeneratorTest::class,
    VegetationGeneratorTest::class,
    LatitudeRangeTest::class,
    TerrainGenerationPipelineTest::class,
    TerrainGridFactoryTest::class,
    TerrainMetricsTest::class
)
class TerrainGenerationTestSuite
