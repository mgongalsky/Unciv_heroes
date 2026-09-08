package com.unciv.testing.pure.domain.arena

import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.pure.domain.arena.*
import org.junit.Assert.*
import org.junit.Test

class ArenaMixedRunTest {
    @Test
    fun productionRunKeepsBothFormatsAndBonusProgressionAcrossTiers() {
        val first = ArenaBattleSetup.newRun(42L)
        val second = ArenaBattleSetup.newRun(42L)
        for (bonus in listOf(30, 20, 10, 0, 0)) {
            assertEquals(first.encounters, second.encounters)
            assertEquals(bonus, first.playerBonusPercent)
            assertEquals(3, first.encounters.size)
            assertEquals(2, first.encounters.count { it.isMixed })
            for (battle in first.encounters.filter { it.isMixed }) {
                val template =
                    ArenaBattleSetup.mixedMatchups.single { "mixed:${it.id}" == battle.id }
                assertEquals(template.player.stacks(bonus), battle.playerArmy)
                assertEquals(template.opponent.stacks(), battle.opponentArmy)
            }
            repeat(3) {
                assertTrue(first.finishBattle(first.beginBattle()!!, ArenaRun.Outcome.VICTORY))
                assertTrue(second.finishBattle(second.beginBattle()!!, ArenaRun.Outcome.VICTORY))
            }
            assertTrue(first.advanceTier())
            assertTrue(second.advanceTier())
        }
    }

    @Test
    fun mixedRetryPreservesBothArmiesAndRejectsStaleResults() {
        val run = ArenaBattleSetup.newRun(42L)
        while (!run.encounters[run.completedBattles].isMixed) {
            run.finishBattle(run.beginBattle()!!, ArenaRun.Outcome.VICTORY)
        }
        val index = run.completedBattles
        val expected = run.encounters[index]
        var previous: ArenaRun.Attempt? = null
        for (outcome in listOf(
            ArenaRun.Outcome.DEFEAT,
            ArenaRun.Outcome.DRAW,
            ArenaRun.Outcome.ABANDONED
        )) {
            val attempt = run.beginBattle()!!
            assertNull(run.beginBattle())
            assertEquals(expected, attempt.encounter)
            previous?.let { assertFalse(run.finishBattle(it, ArenaRun.Outcome.VICTORY)) }
            assertFalse(run.finishBattle(attempt.copy(), ArenaRun.Outcome.VICTORY))
            assertTrue(run.finishBattle(attempt, outcome))
            assertEquals(index, run.completedBattles)
            assertFalse(run.advanceTier())
            previous = attempt
        }
        assertEquals(expected, run.beginBattle()!!.encounter)
    }
}
