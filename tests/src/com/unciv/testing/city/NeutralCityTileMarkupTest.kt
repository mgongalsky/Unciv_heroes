package com.unciv.testing.city

import com.badlogic.gdx.math.Vector2
import com.unciv.UncivGame
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Защищает описание клеток нейтрального города от обращения к отсутствующему владельцу. */
class NeutralCityTileMarkupTest {
    /** Игрок и редактор могут прочитать центр и территорию, включая режим просмотра всей карты. */
    @Test
    fun neutralCenterAndTerritoryCanBeInspectedByPlayerAndEditor() {
        CityTestFixture().use { fixture ->
            val city = CityInfo().apply {
                name = "Neutral castle"
                state.ownership = CityOwnership.Neutral
                location = Vector2(3f, 0f)
                tiles.add(location)
                tiles.add(fixture.game.tileMap[location].neighbors.first().position.cpy())
                // Заведомо неизвестная постройка выявляет случайный расчёт производства при формировании описания.
                cityConstructions.constructionQueue.add("Missing neutral construction")
                bindNeutralWorld(fixture.game)
                setTransients()
            }
            val before = city.cityConstructions.constructionQueue.toList()
            for (debug in listOf(false, true)) {
                UncivGame.Current.viewEntireMapForDebug = debug
                for (tile in city.getTiles()) {
                    assertFalse(tile.toMarkup(fixture.civilization).isEmpty())
                    assertFalse(tile.toMarkup(null).isEmpty())
                    assertNull(tile.getOwner())
                    assertSame(city, tile.getCity())
                }
            }
            assertEquals(before, city.cityConstructions.constructionQueue)
            assertTrue(fixture.civilization.cities.isEmpty())
        }
    }
}
