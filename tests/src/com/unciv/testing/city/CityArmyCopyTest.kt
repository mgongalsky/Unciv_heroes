package com.unciv.testing.city

import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.deepCopy
import com.unciv.pure.domain.battle.FlankingProfile
import com.unciv.pure.domain.battle.RangedShotProfile
import com.unciv.pure.domain.troop.Formation
import com.unciv.pure.domain.troop.Troop
import org.junit.Assert.*
import org.junit.Test

/** Защищает глубокое копирование активной армии города: боевые значения сохраняются, изменения отделены. */
class CityArmyCopyTest {
    /** Копия сохраняет пустые слоты, потери, строй и способности; её изменение не затрагивает оригинал. */
    @Test
    fun copyPreservesSlotsCombatStateAndCapabilities() {
        val troop = Troop("Guard", 20, 5, 12, 100, 7, true, 16, 42, 19,
            Formation(30, 80), 10, 25, 15).apply {
            flanking = FlankingProfile(7, 12, 18)
            rangedShot = RangedShotProfile(4, 30)
            ignoresMovementFormationPenalty = true
            isCavalry = true
            antiCavalryDamageBonusPercent = 35
        }
        val army = Army(4)
        army.setTroopAt(1, troop)
        val copy = army.deepCopy()
        val copied = copy.getTroopAt(1)!!
        assertEquals(4, copy.maxSlots)
        assertNull(copy.getTroopAt(0))
        assertNull(copy.getTroopAt(2))
        assertNull(copy.getTroopAt(3))
        assertEquals(listOf("Guard", 20, 5, 12, 100, 7, true, 16, 42, 19, 10, 25, 15),
            listOf(copied.unitName, copied.amount, copied.speed, copied.damage, copied.maxHealth,
                copied.rangedStrength, copied.isSelfFeeding, copied.currentAmount, copied.currentHealth,
                copied.id, copied.formationHealthPercent, copied.formationDamageReductionPercent,
                copied.supportBonusPercent))
        assertEquals(troop.flanking, copied.flanking)
        assertEquals(troop.rangedShot, copied.rangedShot)
        assertTrue(copied.ignoresMovementFormationPenalty)
        assertTrue(copied.isCavalry)
        assertEquals(35, copied.antiCavalryDamageBonusPercent)
        assertEquals(troop.formation, copied.formation)
        assertNotSame(troop, copied)
        assertNotSame(troop.formation, copied.formation)
        assertNotSame(army.getAllTroops(), copy.getAllTroops())
        copied.amount = 3
        copied.currentAmount = 2
        copied.currentHealth = 1
        copied.formation.current = 0
        copy.setTroopAt(1, null)
        assertSame(troop, army.getTroopAt(1))
        assertEquals(20, troop.amount)
        assertEquals(16, troop.currentAmount)
        assertEquals(42, troop.currentHealth)
        assertEquals(30, troop.formation.current)
    }

    /** Пустая армия остаётся пустой, а сломанный строй не восстанавливается конструктором копии. */
    @Test
    fun emptyAndBrokenFormationSurviveCopy() {
        assertTrue(Army(0).deepCopy().getAllTroops().isEmpty())
        val army = Army(2)
        army.setTroopAt(0, Troop("Guard", 10, maxHealth = 100,
            formation = Formation(0, 100), formationHealthPercent = 10,
            formationDamageReductionPercent = 20))
        assertEquals(Formation(0, 100), army.deepCopy().getTroopAt(0)!!.formation)
    }
}
