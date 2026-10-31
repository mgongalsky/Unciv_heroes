package com.unciv.testing.pure.domain.city

import com.unciv.pure.domain.city.CityOwnership
import com.unciv.pure.domain.city.NeutralCityEntryRules
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision
import org.junit.Assert.*
import org.junit.Test

/** Проверяет чистое решение о входе в нейтральный город без карты, перемещения и боевых эффектов. */
class NeutralCityEntryRulesTest {
    /** Допущенный герой получает пустой город, но любое положительное число защитников требует боя. */
    @Test
    fun heroAcquiresEmptyCityAndMustFightEvenOneLivingDefender() {
        assertEquals(Decision.ACQUIRE, decide(true, 0))
        assertEquals(Decision.FIGHT_GARRISON, decide(true, 1))
        assertEquals(Decision.FIGHT_GARRISON, decide(true, Int.MAX_VALUE))
        repeat(3) { assertEquals(Decision.FIGHT_GARRISON, decide(true, 1)) }
    }

    /** Неподходящий юнит не получает город и не начинает бой независимо от наличия гарнизона. */
    @Test
    fun ineligibleUnitCannotAcquireOrAttackThroughThisEntryRule() {
        assertEquals(Decision.INELIGIBLE_UNIT, decide(false, 0))
        assertEquals(Decision.INELIGIBLE_UNIT, decide(false, 1))
    }

    /** Город игрока и ещё не привязанный город не обрабатываются как нейтральные. */
    @Test
    fun ownedAndUnboundCitiesRemainOutsideNeutralEntry() {
        for (owner in listOf(CityOwnership.Player("Rome"), null)) {
            assertEquals(Decision.NOT_NEUTRAL,
                NeutralCityEntryRules.decide(owner, true, 0))
            assertEquals(Decision.NOT_NEUTRAL,
                NeutralCityEntryRules.decide(owner, true, 1))
        }
    }

    /** Отрицательное число защитников считается ошибкой входных данных. */
    @Test(expected = IllegalArgumentException::class)
    fun negativeDefenderCountIsRejected() {
        decide(true, -1)
    }

    /** Фиксирует нейтральность, оставляя тестам только допуск героя и число живых защитников. */
    private fun decide(hero: Boolean, defenders: Int) =
        NeutralCityEntryRules.decide(CityOwnership.Neutral, hero, defenders)
}
