package com.unciv.testing.city

import com.unciv.logic.GameInfo
import com.unciv.logic.UncivFiles
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import org.junit.Assert.*
import org.junit.Test

class CitySaveFormatTest {
    @Test
    fun plainAndCompressedSavesRestoreCitiesAndPermitOwnershipTransfer() {
        CityTestFixture().use { fixture ->
            val previousMonsterGame = CivilizationInfo.monsterGameInfo
            CivilizationInfo.monsterGameInfo = GameInfo()
            try {
                val original = fixture.foundCity()
                val source = fixture.game.clone()
                source.getCities().single().previousOwner = "Egypt"
                source.turns = 7
                val recipient = CivilizationInfo("Greece").apply {
                    playerType = PlayerType.Human
                    tech.techsResearched.addAll(fixture.civilization.tech.techsResearched)
                }
                source.civilizations.add(recipient)

                for (compressed in listOf(false, true)) {
                    val encoded = UncivFiles.gameInfoToString(source, forceZip = compressed)
                    val loaded = UncivFiles.gameInfoFromString(encoded)
                    assertEquals(GameInfo.CURRENT_COMPATIBILITY_VERSION, loaded.version)
                    assertEquals(source.gameId, loaded.gameId)
                    assertEquals(7, loaded.turns)
                    val city = loaded.getCities().single()
                    val rome = loaded.getCivilization("Rome")
                    val greece = loaded.getCivilization("Greece")
                    assertEquals(original.id, city.id)
                    assertEquals("Egypt", city.previousOwner)
                    assertSame(rome, city.civInfo)
                    assertSame(rome, city.garrisonInfo.civInfo)
                    val army = city.garrisonInfo
                    val territory = city.tiles.toSet()

                    city.moveToCiv(greece)

                    assertSame(greece, city.civInfo)
                    assertTrue(rome.cities.isEmpty())
                    assertSame(city, greece.cities.single())
                    assertSame(city, loaded.getCities().single())
                    assertEquals(original.id, city.id)
                    assertEquals("Rome", city.previousOwner)
                    assertEquals(7, city.turnAcquired)
                    assertEquals(territory, city.tiles)
                    for (position in territory) {
                        assertSame(city, loaded.tileMap[position].getCity())
                        assertSame(greece, loaded.tileMap[position].getOwner())
                    }
                    assertSame(army, city.garrisonInfo)
                    assertSame(greece, army.civInfo)
                }
            } finally {
                CivilizationInfo.monsterGameInfo = previousMonsterGame
            }
        }
    }
}
