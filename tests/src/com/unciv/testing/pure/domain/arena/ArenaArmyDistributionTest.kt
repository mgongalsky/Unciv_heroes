package com.unciv.testing.pure.domain.arena

import com.unciv.pure.domain.arena.*
import org.junit.Assert.*
import org.junit.Test

class ArenaArmyDistributionTest {
    @Test
    fun splitsPreserveTotalsAndHaveNoEmptyStacks() {
        for (total in listOf(1, 2, 3, 4, 5, 26, 221, Int.MAX_VALUE)) {
            for (slots in 1..5) {
                val stacks = ArenaArmyDistribution.split(total, slots)
                assertEquals(minOf(total, slots), stacks.size)
                assertEquals(total.toLong(), stacks.sumOf { it.toLong() })
                assertTrue(stacks.all { it > 0 })
                assertTrue(stacks.maxOrNull()!! - stacks.minOrNull()!! <= 1)
            }
        }
        assertEquals(listOf(7, 7, 6, 6), ArenaArmyDistribution.split(26, 4))
        assertEquals(listOf(6, 5, 5, 5, 5), ArenaArmyDistribution.split(26, 5))
    }

    @Test
    fun generatedSeriesVariesSquadsAndRetryPreservesComposition() {
        val pairs = (1..5).map { ArenaMatchup("Player$it", "Opponent$it", 20, 15) }
        val encounters = ArenaGenerator.generate(pairs, 42L)
        assertEquals(listOf(4, 5, 4), encounters.map { it.troopSlots })
        assertEquals(encounters, ArenaGenerator.generate(pairs, 42L))
        val run = ArenaRun(encounters)
        run.finishBattle(run.beginBattle()!!, ArenaRun.Outcome.VICTORY)
        val first = run.beginBattle()!!
        run.finishBattle(first, ArenaRun.Outcome.DEFEAT)
        val retry = run.beginBattle()!!
        assertEquals(first.encounter.playerStacks, retry.encounter.playerStacks)
        assertEquals(first.encounter.opponentStacks, retry.encounter.opponentStacks)
        assertEquals(5, retry.encounter.playerStacks.size)
        assertEquals(1, run.completedBattles)
    }

    @Test
    fun invalidCountsAndSlotsAreRejected() {
        for ((total, slots) in listOf(0 to 4, -1 to 4, 26 to 0, 26 to 6)) {
            try {
                ArenaArmyDistribution.split(total, slots)
                fail("Accepted total=$total slots=$slots")
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
