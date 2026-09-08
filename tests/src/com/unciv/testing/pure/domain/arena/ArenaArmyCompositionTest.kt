package com.unciv.testing.pure.domain.arena

import com.unciv.pure.domain.arena.*
import org.junit.Assert.*
import org.junit.Test

class ArenaArmyCompositionTest {
    @Test
    fun bonusRoundsOncePerKindBeforeSplitting() {
        val army = ArenaArmyComposition(
            listOf(
                ArenaUnitGroup("Spearman", 3, 3),
                ArenaUnitGroup("Archer", 7, 2)
            )
        )
        assertEquals(
            listOf(
                ArenaStack("Spearman", 2), ArenaStack("Spearman", 1),
                ArenaStack("Spearman", 1), ArenaStack("Archer", 5), ArenaStack("Archer", 5)
            ), army.stacks(30)
        )
        assertEquals(army.stacks(30), army.stacks(30))
        assertEquals(listOf(3, 7), army.groups.map { it.count })
        assertTrue(army.isMixed)
    }

    @Test
    fun scarceFightersNeverCreateEmptyStacks() {
        val army = ArenaArmyComposition(
            listOf(
                ArenaUnitGroup("Horseman", 1, 3), ArenaUnitGroup("Archer", 2, 2)
            )
        )
        assertEquals(
            listOf(
                ArenaStack("Horseman", 1), ArenaStack("Archer", 1), ArenaStack("Archer", 1)
            ), army.stacks()
        )
        assertEquals(5, army.stacks(200).size)
    }

    @Test
    fun slotCountDoesNotChangeBonusTotals() {
        for (bonus in listOf(0, 10, 20, 30)) {
            for (slots in 1..5) {
                val army = ArenaArmyComposition(listOf(ArenaUnitGroup("Spearman", 20, slots)))
                assertEquals(20 + bonus / 5, army.stacks(bonus).sumOf { it.count })
                assertFalse(army.isMixed)
            }
        }
    }

    @Test
    fun inputListChangesCannotChangeComposition() {
        val groups = mutableListOf(ArenaUnitGroup("Archer", 20, 4))
        val army = ArenaArmyComposition(groups)
        groups.clear()
        assertEquals(4, army.stacks().size)
    }

    @Test
    fun invalidCompositionAndBonusAreRejected() {
        fun rejects(action: () -> Unit) {
            try {
                action()
                fail("Invalid arena composition was accepted")
            } catch (_: IllegalArgumentException) {
            }
        }
        rejects { ArenaStack("", 1) }
        rejects { ArenaStack("Archer", 0) }
        rejects { ArenaUnitGroup("Archer", 1, 0) }
        rejects { ArenaUnitGroup("Archer", 1, 6) }
        rejects { ArenaArmyComposition(emptyList()) }
        rejects {
            ArenaArmyComposition(
                listOf(
                    ArenaUnitGroup("Archer", 2, 3), ArenaUnitGroup("Spearman", 2, 3)
                )
            )
        }
        rejects {
            ArenaArmyComposition(
                listOf(
                    ArenaUnitGroup("Archer", 2, 1), ArenaUnitGroup("Archer", 3, 1)
                )
            )
        }
        val army = ArenaArmyComposition(listOf(ArenaUnitGroup("Archer", Int.MAX_VALUE, 5)))
        assertEquals(Int.MAX_VALUE.toLong(), army.stacks().sumOf { it.count.toLong() })
        rejects { army.stacks(1) }
        rejects { army.stacks(-1) }
        rejects { army.stacks(1001) }
        rejects { ArenaMixedMatchup("invalid", army, army) }
    }
}
