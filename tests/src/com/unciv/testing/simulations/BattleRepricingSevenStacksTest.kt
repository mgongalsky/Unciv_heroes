package com.unciv.testing.simulations

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Explicit repricing experiment using current rules, seven stacks and budget-scale armies. */
class BattleRepricingSevenStacksTest {
    @Test
    fun measureCurrentBudgetScaleBoundaries() {
        val root = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
            .first { File(it, "android/assets/jsons/Civ V - Gods & Kings/Units.json").isFile }
        val source = File(root, "android/assets/jsons/Civ V - Gods & Kings/Units.json")
        val bytes = source.readBytes()
        val units = BalanceUnitSources.parse(bytes.toString(Charsets.UTF_8))
        val ruleset = BalanceUnitSources.ruleset(units)
        val prices = units.associate { unit ->
            unit.name to requireNotNull(ruleset.units[unit.name]).battleCost.also { require(it > 0) }
        }
        val scales = prices.mapValues { (_, price) -> (500 / price).coerceAtLeast(1) }
        val directory = File(root, "tests/balance-results/repricing-seven-500-${System.currentTimeMillis()}")
        check(directory.mkdirs())
        File(directory, "Units.json").writeBytes(bytes)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        File(directory, "parameters.txt").writeText(
            "Source=${source.absolutePath}\nSHA256=$hash\n" +
                "Current prices=$prices\nAttacker amounts=$scales\n" +
                "Budget=500; troopSlots=7; seeds=200..207; defender range=1..5000\n" +
                "Production BattleManager, setup and AI; luck=0.15; morale=0.15\n" +
                "maxTurns=1000; noProgressLimit=100\n" +
                "Boundary search assumes a decreasing crossing; midpoint is an estimate.\n" +
                "Pricing fit: log(price_attacker)-log(price_defender)=log(defendersPerAttacker); Peasant=1.\n" +
                units.joinToString("\n")
        )
        println("REPRICING_REPORT=${directory.absolutePath}")
        var completed = 0
        File(directory, "boundaries.csv").bufferedWriter().use { summary ->
            File(directory, "probes.csv").bufferedWriter().use { samples ->
                summary.appendLine("attacker,defender,status,defendersPerAttacker,attackerAmount,lowerDefenders,upperDefenders,probes")
                samples.appendLine("attacker,defender,attackerAmount,defenderAmount,simulations,attackerWins,defenderWins,mutualDefeats,stalemates,maxTurns,other,averageTurns")
                for ((ai, attacker) in units.withIndex()) for ((di, defender) in units.withIndex()) {
                    val result = BalanceBoundarySearch.run(scales.getValue(attacker.name), 5000) { a, d ->
                        val batch = BattleBalanceSimulation.simulateBatch(
                            attacker.name, a, defender.name, d, (200L..207L).toList(),
                            0.15, 0.15, ruleset, troopSlots = 7
                        )
                        val mutual = batch.results.count {
                            it.termination == com.unciv.pure.application.battle.BattleTermination.MUTUAL_DEFEAT
                        }
                        val other = batch.simulations - batch.attackerWins - batch.defenderWins -
                            batch.stalemates - batch.maxTurnTerminations
                        samples.appendLine(listOf(
                            attacker.name, defender.name, a, d, batch.simulations,
                            batch.attackerWins, batch.defenderWins, mutual, batch.stalemates,
                            batch.maxTurnTerminations, other - mutual, batch.averageTurns
                        ).joinToString(","))
                        samples.flush()
                        BalanceCell(ai, di, a, d, batch.simulations, batch.attackerWins,
                            batch.defenderWins, batch.stalemates, batch.maxTurnTerminations,
                            other, batch.averageTurns)
                    }
                    val line = listOf(
                        attacker.name, defender.name, result.status,
                        result.defendersPerAttacker ?: "", result.attackerAmount,
                        result.lowerDefenders ?: "", result.upperDefenders ?: "", result.probes.size
                    ).joinToString(",")
                    summary.appendLine(line)
                    summary.flush()
                    completed++
                    println("PAIR $completed/${units.size * units.size}: $line")
                }
            }
        }
        assertEquals(units.size * units.size, completed)
        assertEquals(completed + 1, File(directory, "boundaries.csv").readLines().size)
    }
}
