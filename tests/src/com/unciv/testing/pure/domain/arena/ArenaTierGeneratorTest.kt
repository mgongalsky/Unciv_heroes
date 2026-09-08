package com.unciv.testing.pure.domain.arena

import com.unciv.pure.domain.arena.*
import org.junit.Assert.*
import org.junit.Test

class ArenaTierGeneratorTest {
    private val pairs = listOf(ArenaMatchup("Spearman", "Archer", 20, 36))
    private fun army(first: String, second: String) = ArenaArmyComposition(
        listOf(
            ArenaUnitGroup(first, 10, 2), ArenaUnitGroup(second, 7, 2)
        )
    )

    private val mixed = listOf(
        ArenaMixedMatchup("infantry", army("Spearman", "Archer"), army("Swordsman", "Archer")),
        ArenaMixedMatchup("cavalry", army("Horseman", "Spearman"), army("Horseman", "Swordsman")),
        ArenaMixedMatchup("combined", army("Horseman", "Archer"), army("Spearman", "Archer"))
    )

    @Test
    fun everySeedIncludesBothFormatsWithoutDuplicateEncounters() {
        for (seed in 0L until 50L) {
            val tier = ArenaTierGenerator.generate(pairs, mixed, seed, 30)
            assertEquals(3, tier.size)
            assertEquals(2, tier.count { it.isMixed })
            assertEquals(3, tier.map { it.id }.distinct().size)
            assertEquals(tier, ArenaTierGenerator.generate(pairs, mixed, seed, 30))
            for (battle in tier) {
                assertTrue(battle.playerArmy.size <= 5)
                assertTrue(battle.opponentArmy.size <= 5)
                if (battle.isMixed) {
                    assertEquals(2, battle.playerArmy.map { it.unitName }.distinct().size)
                    assertEquals(2, battle.opponentArmy.map { it.unitName }.distinct().size)
                }
            }
        }
    }

    @Test
    fun tierBonusChangesOnlyPlayerCountsAndKeepsHomogeneousCalibration() {
        for (bonus in listOf(30, 20, 10, 0)) {
            val tier = ArenaTierGenerator.generate(pairs, mixed, 42L, bonus)
            val homogeneous = tier.single { !it.isMixed }
            assertEquals(20 + bonus / 5, homogeneous.playerArmy.sumOf { it.count })
            assertEquals(36, homogeneous.opponentArmy.sumOf { it.count })
            for (battle in tier.filter { it.isMixed }) {
                val template = mixed.single { "mixed:${it.id}" == battle.id }
                assertEquals(template.player.stacks(bonus), battle.playerArmy)
                assertEquals(template.opponent.stacks(), battle.opponentArmy)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun insufficientMixedPoolIsRejected() {
        ArenaTierGenerator.generate(pairs, mixed.take(1), 0L, 30)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateMixedIdsAreRejected() {
        ArenaTierGenerator.generate(pairs, listOf(mixed.first(), mixed.first()), 0L, 30)
    }
}
