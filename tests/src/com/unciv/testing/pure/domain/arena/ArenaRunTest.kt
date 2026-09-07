package com.unciv.testing.pure.domain.arena

import com.unciv.pure.domain.arena.ArenaEncounter
import com.unciv.pure.domain.arena.ArenaGenerator
import com.unciv.pure.domain.arena.ArenaMatchup
import com.unciv.pure.domain.arena.ArenaRun
import com.unciv.pure.domain.arena.ArenaTierConfig
import org.junit.Assert.*
import org.junit.Test

class ArenaRunTest {
    private val matchups = (1..6).map { ArenaMatchup("Player$it", "Opponent$it", 20, 15) }
    private fun newRun() = ArenaRun(ArenaGenerator.generate(matchups, 42L))

    @Test
    fun `tier contains five different directed matchups and thirty percent more player units`() {
        val encounters = ArenaGenerator.generate(matchups, 42L)
        assertEquals(5, encounters.size)
        assertEquals(5, encounters.map { it.matchup }.distinct().size)
        assertTrue(encounters.all { it.playerCount == 26 && it.matchup.opponentCount == 15 })
        assertEquals(encounters, ArenaGenerator.generate(matchups, 42L))
    }

    @Test
    fun `fractional bonus rounds up and zero bonus preserves count`() {
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
    fun `defeat draw and leaving allow retry with the same armies and keep earlier victories`() {
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
            assertEquals(outcome, run.lastOutcome)
        }
        assertEquals(expected, run.beginBattle()!!.encounter)
    }

    @Test
    fun `duplicate or stale battle results cannot advance the tier`() {
        val run = newRun()
        val first = run.beginBattle()!!
        assertNull(run.beginBattle())
        assertTrue(run.finishBattle(first, ArenaRun.Outcome.VICTORY))
        val second = run.beginBattle()!!
        assertFalse(run.finishBattle(first, ArenaRun.Outcome.VICTORY))
        assertFalse(run.finishBattle(second.copy(), ArenaRun.Outcome.VICTORY))
        assertEquals(1, run.completedBattles)
        assertTrue(run.finishBattle(second, ArenaRun.Outcome.VICTORY))
        assertEquals(2, run.completedBattles)
    }

    @Test
    fun `fifth victory completes the tier and no sixth battle starts`() {
        val run = newRun()
        repeat(5) { index ->
            assertFalse(run.isComplete)
            val attempt = run.beginBattle()!!
            assertEquals(index, attempt.battleIndex)
            assertTrue(run.finishBattle(attempt, ArenaRun.Outcome.VICTORY))
        }
        assertTrue(run.isComplete)
        assertNull(run.beginBattle())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `insufficient matchups are rejected`() {
        ArenaGenerator.generate(matchups.take(4), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate directed pairs are rejected`() {
        ArenaGenerator.generate(matchups + matchups.first(), 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bonus cannot overflow army size`() {
        ArenaGenerator.generate(
            listOf(ArenaMatchup("Archer", "Spearman", Int.MAX_VALUE, 1)),
            0,
            ArenaTierConfig(1)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty run is rejected`() {
        ArenaRun(emptyList<ArenaEncounter>())
    }
}
