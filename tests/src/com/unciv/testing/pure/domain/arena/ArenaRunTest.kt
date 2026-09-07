package com.unciv.testing.pure.domain.arena

import com.unciv.pure.domain.arena.*
import org.junit.Assert.*
import org.junit.Test

class ArenaRunTest {
    private val matchups = (1..6).map { ArenaMatchup("Player$it", "Opponent$it", 20, 15) }
    private fun newRun() = ArenaRun.generated(matchups, 42L)
    private fun winTier(run: ArenaRun) {
        repeat(3) { assertTrue(run.finishBattle(run.beginBattle()!!, ArenaRun.Outcome.VICTORY)) }
    }

    @Test
    fun threeDistinctBattlesReceiveThirtyPercentBonus() {
        val encounters = newRun().encounters
        assertEquals(3, encounters.size)
        assertEquals(3, encounters.map { it.matchup }.distinct().size)
        assertTrue(encounters.all { it.playerCount == 26 && it.matchup.opponentCount == 15 })
        assertEquals(encounters, newRun().encounters)
    }

    @Test
    fun bonusRoundsUp() {
        val pair = listOf(ArenaMatchup("Archer", "Spearman", 3, 2))
        assertEquals(
            4,
            ArenaGenerator.generate(pair, 0, ArenaTierConfig(1, 30)).single().playerCount
        )
        assertEquals(
            3,
            ArenaGenerator.generate(pair, 0, ArenaTierConfig(1, 0)).single().playerCount
        )
    }

    @Test
    fun defeatsDrawsAndLeavingKeepProgressAndComposition() {
        val run = newRun()
        run.finishBattle(run.beginBattle()!!, ArenaRun.Outcome.VICTORY)
        val expected = run.encounters[1]
        for (outcome in listOf(
            ArenaRun.Outcome.DEFEAT,
            ArenaRun.Outcome.DRAW,
            ArenaRun.Outcome.ABANDONED
        )) {
            val attempt = run.beginBattle()!!
            assertEquals(expected, attempt.encounter)
            assertTrue(run.finishBattle(attempt, outcome))
            assertEquals(1, run.completedBattles)
            assertEquals(1, run.tier)
            assertEquals(outcome, run.lastOutcome)
            assertFalse(run.advanceTier())
        }
        assertEquals(expected, run.beginBattle()!!.encounter)
    }

    @Test
    fun duplicateAndCopiedResultsCannotAdvanceProgress() {
        val run = newRun()
        val first = run.beginBattle()!!
        assertNull(run.beginBattle())
        assertFalse(run.advanceTier())
        assertTrue(run.finishBattle(first, ArenaRun.Outcome.VICTORY))
        val second = run.beginBattle()!!
        assertFalse(run.finishBattle(first, ArenaRun.Outcome.VICTORY))
        assertFalse(run.finishBattle(second.copy(), ArenaRun.Outcome.VICTORY))
        assertEquals(1, run.completedBattles)
        assertTrue(run.finishBattle(second, ArenaRun.Outcome.VICTORY))
    }

    @Test
    fun thirdVictoryUnlocksNextTierWithoutStartingAFourthBattle() {
        val run = newRun()
        assertFalse(run.advanceTier())
        winTier(run)
        assertTrue(run.isComplete)
        assertNull(run.beginBattle())
        assertEquals(1, run.tier)
        val retainedByMenu = run
        assertTrue(run.advanceTier())
        assertSame(run, retainedByMenu)
        assertEquals(2, retainedByMenu.tier)
        assertEquals(0, retainedByMenu.completedBattles)
        assertNull(run.lastOutcome)
        assertFalse(run.advanceTier())
        assertEquals(0, run.beginBattle()!!.battleIndex)
    }

    @Test
    fun bonusesDecreaseByTenPointsAndStayAtZero() {
        val run = newRun()
        for ((index, bonus) in listOf(30, 20, 10, 0, 0, 0).withIndex()) {
            assertEquals(index + 1, run.tier)
            assertEquals(bonus, run.playerBonusPercent)
            assertEquals(3, run.encounters.size)
            assertTrue(run.encounters.all { it.playerCount == 20 + bonus / 5 })
            assertTrue(run.encounters.all { it.matchup.opponentCount == 15 })
            assertEquals(listOf(4, 5, 4), run.encounters.map { it.troopSlots })
            winTier(run)
            assertTrue(run.advanceTier())
        }
        assertEquals(0, ArenaGenerator.bonusForTier(Int.MAX_VALUE))
    }

    @Test
    fun oldTierResultsCannotFinishNewTierAttempts() {
        val run = newRun()
        repeat(2) { run.finishBattle(run.beginBattle()!!, ArenaRun.Outcome.VICTORY) }
        val oldAttempt = run.beginBattle()!!
        run.finishBattle(oldAttempt, ArenaRun.Outcome.VICTORY)
        run.advanceTier()
        val nextAttempt = run.beginBattle()!!
        assertTrue(nextAttempt.id > oldAttempt.id)
        assertFalse(run.finishBattle(oldAttempt, ArenaRun.Outcome.VICTORY))
        assertEquals(0, run.completedBattles)
        assertTrue(run.finishBattle(nextAttempt, ArenaRun.Outcome.DEFEAT))
        assertEquals(nextAttempt.encounter, run.beginBattle()!!.encounter)
        assertEquals(20, run.playerBonusPercent)
    }

    @Test
    fun tierGenerationRemainsDeterministic() {
        val first = newRun()
        val second = newRun()
        repeat(5) {
            assertEquals(first.encounters, second.encounters)
            winTier(first)
            winTier(second)
            assertTrue(first.advanceTier())
            assertTrue(second.advanceTier())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun insufficientMatchupsAreRejected() {
        ArenaGenerator.generate(matchups.take(2), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicatePairsAreRejected() {
        ArenaGenerator.generate(matchups + matchups.first(), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun bonusCannotOverflow() {
        ArenaGenerator.generate(
            listOf(ArenaMatchup("Archer", "Spearman", Int.MAX_VALUE, 1)),
            0,
            ArenaTierConfig(1)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyRunIsRejected() {
        ArenaRun(emptyList())
    }
}
