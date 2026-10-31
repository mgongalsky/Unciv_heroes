package com.unciv.testing.city

import com.unciv.logic.GameInfo
import com.unciv.logic.city.CityInfo
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.city.CityOwnership
import org.junit.Assert.*
import org.junit.Test

/** Проверяет привязку нейтрального города к миру без создания цивилизации для его обслуживания. */
class NeutralCityBindingTest {
    /** Нейтральному городу доступны мир и правила; его копия сохраняет нейтральность и независимую армию. */
    @Test
    fun neutralCityBindsToWorldAndCopiesWithoutCivilization() {
        val game = GameInfo()
        val city = CityInfo()
        city.state.ownership = CityOwnership.Neutral
        city.state.garrison = Army(2)
        city.bindNeutralWorld(game)

        assertSame(game, city.gameInfo)
        assertSame(game.ruleSet, city.getRuleset())
        assertNull(city.ownerOrNull)
        assertNull(city.garrisonInfo.ownerOrNull)
        assertTrue(game.civilizations.isEmpty())

        val copy = city.clone()
        assertEquals(CityOwnership.Neutral, copy.state.ownership)
        assertNull(copy.ownerOrNull)
        assertNull(copy.garrisonInfo.ownerOrNull)
        assertNotSame(city.state.garrison, copy.state.garrison)
        assertEquals(2, copy.state.garrison!!.maxSlots)
        assertTrue(game.civilizations.isEmpty())
    }

    /** Привязка нейтрального мира не должна неявно отнимать город игрока. */
    @Test
    fun neutralBindingRejectsPlayerCityWithoutChangingOwner() {
        CityTestFixture().use { fixture ->
            val city = fixture.foundCity()
            try {
                city.bindNeutralWorld(fixture.game)
                fail("Player city must not be silently neutralized")
            } catch (_: IllegalArgumentException) {
                assertSame(fixture.civilization, city.ownerOrNull)
                assertSame(fixture.civilization, city.garrisonInfo.ownerOrNull)
            }
        }
    }
}
