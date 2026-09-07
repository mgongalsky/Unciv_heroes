package com.unciv.testing.simulations

import com.badlogic.gdx.Gdx
import com.unciv.pure.application.battle.BattleTermination
import org.junit.Assert.*
import org.junit.Test

class StandaloneBattleWithoutApplicationTest {
    @Test
    fun loadsGameDataAndRunsBattlesWithoutApplicationOrGraphics() {
        assertNull("This test must run without a GDX application", Gdx.app)
        assertNull(Gdx.graphics)
        assertNull(Gdx.gl)
        val definitions = BalanceUnitSources.preset
        val ruleset = BalanceUnitSources.ruleset(definitions)
        val archer = ruleset.units.getValue("Archer")
        assertEquals(
            definitions.single { it.name == "Archer" }.formationHealthPercent,
            archer.formationHealthPercent
        )
        for (slots in 4..5) {
            val batch = BattleBalanceSimulation.simulateBatch(
                "Peasant", 47, "Archer", 5, 0L until 20L,
                luckProbability = 0.05, moraleProbability = 0.1,
                ruleset = ruleset, troopSlots = slots
            )
            assertEquals(20, batch.simulations)
            assertTrue("Battles must finish through game rules", batch.results.all {
                it.termination == BattleTermination.VICTORY || it.termination == BattleTermination.MUTUAL_DEFEAT
            })
            assertTrue(
                "Tier 1 regression: slots=$slots wins=${batch.attackerWins}/20",
                batch.attackerWins >= 18
            )
        }
        assertNull(Gdx.app)
        assertNull(Gdx.graphics)
        assertNull(Gdx.gl)
    }
}
