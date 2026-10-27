package com.unciv.testing.city

import com.unciv.UncivGame
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.map.MapUnit
import com.unciv.models.GameConstants
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.domain.troop.TroopFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.context.GlobalContext

class CityFixtureIsolationTest {
    @Test
    fun repeatedFoundingRestoresGlobalsIncludingAfterFailure() {
        val fields = listOf(
            UncivGame::class.java to "Current",
            MapUnit::class.java to "_monsterCivInfo",
            MapUnit::class.java to "currID",
            GameConstants::class.java to "_testData",
            ArmyInfo::class.java to "_defaultMaxSlots",
            TroopFactory::class.java to "nextId"
        ).map { (type, name) -> type.getDeclaredField(name).apply { isAccessible = true } }
        val before = fields.map { it.get(null) }
        val cached = HashMap(RulesetCache)
        val snapshots = mutableListOf<List<Any>>()
        val sentinel = IllegalStateException("Failure after founding")

        repeat(2) { iteration ->
            try {
                CityTestFixture().use { fixture ->
                    val city = fixture.foundCity()
                    snapshots.add(listOf(
                        city.name,
                        city.population.population,
                        city.tiles.map { "${it.x},${it.y}" }.sorted(),
                        city.cityConstructions.builtBuildings.sorted(),
                        city.garrisonInfo.getAllTroops().map { it?.id },
                        fixture.civilization.citiesCreated
                    ))
                    if (iteration == 1) throw sentinel
                }
            } catch (failure: IllegalStateException) {
                assertSame(sentinel, failure)
            }
            fields.forEachIndexed { index, field ->
                if (field.type.isPrimitive) assertEquals(before[index], field.get(null))
                else assertSame(before[index], field.get(null))
            }
            assertEquals(cached.keys, RulesetCache.keys)
            cached.forEach { (name, ruleset) -> assertSame(ruleset, RulesetCache[name]) }
            assertNull(GlobalContext.getOrNull())
        }
        assertEquals(2, snapshots.size)
        assertEquals(snapshots[0], snapshots[1])
    }
}
