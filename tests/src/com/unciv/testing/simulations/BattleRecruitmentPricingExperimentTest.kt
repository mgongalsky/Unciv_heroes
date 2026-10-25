package com.unciv.testing.simulations

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertTrue
import org.junit.Test

/** Explicit pricing experiment; reports preserve inputs and every measured probe. */
class BattleRecruitmentPricingExperimentTest {
    @Test
    fun measureFiveStackMatchups() {
        val root = generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
            .first { File(it, "android/assets/jsons/Civ V - Gods & Kings/Units.json").isFile }
        val source = File(root, "android/assets/jsons/Civ V - Gods & Kings/Units.json")
        val bytes = source.readBytes()
        val units = BalanceUnitSources.parse(bytes.toString(Charsets.UTF_8))
        val ruleset = BalanceUnitSources.ruleset(units)
        val directory = File(root, "tests/balance-results/recruitment-500-${System.currentTimeMillis()}")
        check(directory.mkdirs())
        File(directory, "Units.json").writeBytes(bytes)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        File(directory, "parameters.txt").writeText(
            "Source=${source.absolutePath}\nSHA256=$hash\n" +
                "Attacker amount=20; defender range=1..2000; seeds=0..3; troopSlots=5\n" +
                "Production BattleManager and AI; field=14x8; terrainSeed=42; climate=0.3/0.5/0.6\n" +
                "luckProbability=0.15; moraleProbability=0.15; maxTurns=1000; noProgressLimit=100\n" +
                "Boundary search assumes a decreasing crossing; midpoint is an estimate.\n" +
                units.joinToString("\n")
        )
        println("PRICING_REPORT=${directory.absolutePath}")
        File(directory, "boundaries.csv").bufferedWriter().use { summary ->
            File(directory, "probes.csv").bufferedWriter().use { samples ->
                summary.appendLine("attacker,defender,status,defendersPerAttacker,attackerAmount,lowerDefenders,upperDefenders,probes")
                samples.appendLine("attacker,defender,attackerAmount,defenderAmount,simulations,attackerWins,defenderWins,stalemates,maxTurns,other,averageTurns")
                for ((ai, attacker) in units.withIndex()) for ((di, defender) in units.withIndex()) {
                    val result = BalanceBoundarySearch.run(20, 2000) { a, d ->
                        val batch = BattleBalanceSimulation.simulateBatch(
                            attacker.name, a, defender.name, d, (0L..3L).toList(),
                            0.15, 0.15, ruleset, troopSlots = 5
                        )
                        val other = batch.simulations - batch.attackerWins - batch.defenderWins -
                            batch.stalemates - batch.maxTurnTerminations
                        samples.appendLine(listOf(attacker.name, defender.name, a, d, batch.simulations,
                            batch.attackerWins, batch.defenderWins, batch.stalemates,
                            batch.maxTurnTerminations, other, batch.averageTurns).joinToString(","))
                        samples.flush()
                        BalanceCell(ai, di, a, d, batch.simulations, batch.attackerWins,
                            batch.defenderWins, batch.stalemates, batch.maxTurnTerminations, other,
                            batch.averageTurns)
                    }
                    val line = listOf(attacker.name, defender.name, result.status,
                        result.defendersPerAttacker ?: "", result.attackerAmount,
                        result.lowerDefenders ?: "", result.upperDefenders ?: "", result.probes.size).joinToString(",")
                    summary.appendLine(line)
                    summary.flush()
                    println(line)
                }
            }
        }
        assertTrue(File(directory, "boundaries.csv").readLines().size == 26)
    }

    @Test
    fun verifyEqualBudgetCandidates() {
        val root =
                generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .first {
                        File(
                            it,
                            "android/assets/jsons/Civ V - Gods & Kings/Units.json"
                        ).isFile
                    }
        val source = File(root, "android/assets/jsons/Civ V - Gods & Kings/Units.json")
        val units = BalanceUnitSources.load(source)
        val ruleset = BalanceUnitSources.ruleset(units)
        val prices = units.associate { it.name to requireNotNull(ruleset.units[it.name]).battleCost.also { price -> require(price > 0) } }
        val directory =
                File(root, "tests/balance-results/equal-budget-500-${System.currentTimeMillis()}")
        check(directory.mkdirs())
        File(directory, "Units.json").writeBytes(source.readBytes())
        File(directory, "parameters.txt").writeText(
            "Candidate prices=$prices\nBudget=500; slots=5; seeds=100..115\n" +
                    "Production field=14x8; terrainSeed=42; luckProbability=0.15; moraleProbability=0.15\n" +
                    "Counts=floor(500/price); rounding remainder is unspent.\n" + units.joinToString(
                "\n"
            )
        )
        println("EQUAL_BUDGET_REPORT=${directory.absolutePath}")
        File(directory, "results.csv").bufferedWriter().use { writer ->
            writer.appendLine("attacker,defender,attackerAmount,defenderAmount,attackerWins,defenderWins,mutualDefeats,unresolved")
            for ((attacker, attackPrice) in prices) for ((defender, defendPrice) in prices) {
                val batch = BattleBalanceSimulation.simulateBatch(
                    attacker, 500 / attackPrice, defender, 500 / defendPrice,
                    (100L..115L).toList(), 0.15, 0.15, ruleset, troopSlots = 5
                )
                val mutual =
                        batch.results.count { it.termination == com.unciv.pure.application.battle.BattleTermination.MUTUAL_DEFEAT }
                val unresolved = batch.results.count {
                    it.termination != com.unciv.pure.application.battle.BattleTermination.VICTORY &&
                            it.termination != com.unciv.pure.application.battle.BattleTermination.MUTUAL_DEFEAT
                }
                val line = listOf(
                    attacker, defender, 500 / attackPrice, 500 / defendPrice,
                    batch.attackerWins, batch.defenderWins, mutual, unresolved
                ).joinToString(",")
                writer.appendLine(line)
                writer.flush()
                println(line)
            }
        }
        File(directory, "diagnostics.csv").bufferedWriter().use { writer ->
            writer.appendLine("attacker,defender,attackerAmount,defenderAmount,seed,termination,turns")
            val probes = listOf(
                Triple("Peasant", "Peasant", 18), Triple("Swordsman", "Archer", 56),
                Triple("Swordsman", "Spearman", 26), Triple("Spearman", "Horseman", 10)
            )
            for ((attacker, defender, count) in probes) {
                val batch = BattleBalanceSimulation.simulateBatch(
                    attacker, 20, defender, count,
                    (0L..3L).toList(), 0.15, 0.15, ruleset, troopSlots = 5
                )
                batch.results.forEach { result ->
                    writer.appendLine(
                        listOf(
                            attacker, defender, 20, count, result.seed,
                            result.termination, result.turns
                        ).joinToString(",")
                    )
                }
            }
        }
        assertTrue(File(directory, "results.csv").readLines().size == 26)
    }

    @Test
    fun measureBudgetScaleBoundaries() {
        val root =
                generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .first {
                        File(
                            it,
                            "android/assets/jsons/Civ V - Gods & Kings/Units.json"
                        ).isFile
                    }
        val source = File(root, "android/assets/jsons/Civ V - Gods & Kings/Units.json")
        val ruleset = BalanceUnitSources.ruleset(BalanceUnitSources.load(source))
        val scales = linkedMapOf(
            "Peasant" to 500, "Archer" to 100, "Spearman" to 62,
            "Swordsman" to 41, "Horseman" to 29
        )
        val directory =
                File(root, "tests/balance-results/budget-scale-500-${System.currentTimeMillis()}")
        check(directory.mkdirs())
        File(directory, "Units.json").writeBytes(source.readBytes())
        File(
            directory,
            "parameters.txt"
        ).writeText("Attacker amounts=$scales\nSeeds=200..207; slots=5; defender range=1..5000\nProduction field and AI; luck=0.15; morale=0.15\n")
        println("BUDGET_SCALE_REPORT=${directory.absolutePath}")
        File(directory, "boundaries.csv").bufferedWriter().use { summary ->
            File(directory, "probes.csv").bufferedWriter().use { samples ->
                summary.appendLine("attacker,defender,status,defendersPerAttacker,attackerAmount,lowerDefenders,upperDefenders")
                samples.appendLine("attacker,defender,attackerAmount,defenderAmount,attackerWins,defenderWins,mutualDefeats,unresolved")
                for ((ai, entry) in scales.entries.withIndex()) for ((di, defender) in scales.keys.withIndex()) {
                    val attacker = entry.key
                    val result = BalanceBoundarySearch.run(entry.value, 5000) { a, d ->
                        val batch = BattleBalanceSimulation.simulateBatch(
                            attacker, a, defender, d,
                            (200L..207L).toList(), 0.15, 0.15, ruleset, troopSlots = 5
                        )
                        val mutual =
                                batch.results.count { it.termination == com.unciv.pure.application.battle.BattleTermination.MUTUAL_DEFEAT }
                        val other = batch.simulations - batch.attackerWins - batch.defenderWins -
                                batch.stalemates - batch.maxTurnTerminations
                        val unresolved =
                                batch.simulations - batch.attackerWins - batch.defenderWins - mutual
                        samples.appendLine(
                            listOf(
                                attacker, defender, a, d, batch.attackerWins,
                                batch.defenderWins, mutual, unresolved
                            ).joinToString(",")
                        )
                        samples.flush()
                        BalanceCell(
                            ai,
                            di,
                            a,
                            d,
                            batch.simulations,
                            batch.attackerWins,
                            batch.defenderWins,
                            batch.stalemates,
                            batch.maxTurnTerminations,
                            other,
                            batch.averageTurns
                        )
                    }
                    val line = listOf(
                        attacker,
                        defender,
                        result.status,
                        result.defendersPerAttacker ?: "",
                        result.attackerAmount,
                        result.lowerDefenders ?: "",
                        result.upperDefenders ?: ""
                    ).joinToString(",")
                    summary.appendLine(line)
                    summary.flush()
                    println(line)
                }
            }
        }
        assertTrue(File(directory, "boundaries.csv").readLines().size == 26)
    }
}
