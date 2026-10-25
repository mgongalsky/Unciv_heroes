package com.unciv.testing.simulations

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import com.unciv.pure.application.battle.BattleTermination

/** Compare candidate prices using identical independent seeds. Reports retain each candidate. */
class BattleRepricingValidationTest {
    @Test
    fun compareEqualBudgetPrices() {
        val root = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
            .first { File(it, "android/assets/jsons/Civ V - Gods & Kings/Units.json").isFile }
        val source = File(root, "android/assets/jsons/Civ V - Gods & Kings/Units.json")
        val bytes = source.readBytes()
        val units = BalanceUnitSources.parse(bytes.toString(Charsets.UTF_8))
        val ruleset = BalanceUnitSources.ruleset(units)
        val candidates = linkedMapOf(
            "intermediate" to linkedMapOf("Peasant" to 1, "Archer" to 4, "Spearman" to 9, "Swordsman" to 12, "Horseman" to 16),
            "cavalry15" to linkedMapOf("Peasant" to 1, "Archer" to 4, "Spearman" to 9, "Swordsman" to 12, "Horseman" to 15)
        )
        val directory = File(root, "tests/balance-results/repricing-validation-${System.currentTimeMillis()}")
        check(directory.mkdirs())
        File(directory, "Units.json").writeBytes(bytes)
        File(directory, "parameters.txt").writeText(
            "Budget=500; troopSlots=7; seeds=1000..1031\n" +
                "Production setup and AI; luck=0.15; morale=0.15; maxTurns=1000; noProgressLimit=100\n" +
                "Counts=floor(500/price); remainder unspent; same seeds for both candidates.\n" +
                "Candidates=$candidates\n" + units.joinToString("\n")
        )
        println("VALIDATION_REPORT=${directory.absolutePath}")
        var completed = 0
        File(directory, "results.csv").bufferedWriter().use { summary ->
            File(directory, "battles.csv").bufferedWriter().use { details ->
                summary.appendLine("candidate,attacker,defender,attackerAmount,defenderAmount,attackerWins,defenderWins,mutualDefeats,unresolved,averageTurns")
                details.appendLine("candidate,attacker,defender,seed,termination,winnerIsAttacker,turns")
                for ((label, prices) in candidates) {
                    for ((attacker, attackPrice) in prices) for ((defender, defendPrice) in prices) {
                        val batch = BattleBalanceSimulation.simulateBatch(
                            attacker, 500 / attackPrice, defender, 500 / defendPrice,
                            (1000L..1031L).toList(), 0.15, 0.15, ruleset, troopSlots = 7
                        )
                        val mutual = batch.results.count { it.termination == BattleTermination.MUTUAL_DEFEAT }
                        val unresolved = batch.results.count {
                            it.termination != BattleTermination.VICTORY && it.termination != BattleTermination.MUTUAL_DEFEAT
                        }
                        val line = listOf(label, attacker, defender, 500 / attackPrice, 500 / defendPrice,
                            batch.attackerWins, batch.defenderWins, mutual, unresolved, batch.averageTurns).joinToString(",")
                        summary.appendLine(line)
                        summary.flush()
                        batch.results.forEach {
                            details.appendLine(listOf(label, attacker, defender, it.seed, it.termination,
                                it.winnerIsAttacker, it.turns).joinToString(","))
                        }
                        details.flush()
                        completed++
                        println("PAIR $completed/50: $line")
                    }
                }
            }
        }
        assertEquals(50, completed)
        assertEquals(1601, File(directory, "battles.csv").readLines().size)
    }
}
