package com.unciv.integration.maps

import com.unciv.logic.MapFormatMigration
import com.unciv.logic.MapSaver
import com.unciv.logic.map.MapUnit
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.domain.troop.HardcodedTroopDefinitionSource
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File

/** Contract tests for historical map files loaded through the production map loader. */
class MapCompatibilityTests {

    @Before
    fun setUpDependencies() {
        GameConstants.setTestingInstance(GameConstantsData(armySize = 4))
        MapUnit.setTestingInstance(FakeCivilizationInfo())
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"
                unitType = "Melee"
                health = 100
            }
        }
        startKoin {
            modules(module {
                single { ruleset }
                single<ITroopDefinitionSource> {
                    HardcodedTroopDefinitionSource(5, 10, 100, 0, false)
                }
            })
        }
    }

    @After
    fun tearDownDependencies() {
        stopKoin()
        GameConstants.clearTestingInstance()
    }

    @Test
    fun historicalMapsRemainReadable() {
        val failures = mutableListOf<String>()
        for (mapName in listOf("Campaign01", "ArmyTest")) {
            try {
                val mapFile = File("maps", mapName)
                assertTrue("Historical map fixture $mapName must exist", mapFile.isFile)
                val map = MapSaver.mapFromSavedString(mapFile.readText())
                assertTrue("Historical map $mapName must contain tiles", map.values.isNotEmpty())
                assertEquals(MapFormatMigration.currentVersion, map.mapParameters.mapFormatVersion)
            } catch (exception: Exception) {
                val causes = generateSequence<Throwable>(exception) { it.cause }.toList()
                val root = causes.last()
                val stack = root.stackTrace.take(12).joinToString("\n") { "  at $it" }
                failures += "$mapName: ${causes.joinToString(" -> ") { "${it::class.java.simpleName}: ${it.message}" }}\n$stack"
            }
        }
        assertTrue(
            "Historical maps failed to load:\n${failures.joinToString("\n")}",
            failures.isEmpty()
        )
    }
}
