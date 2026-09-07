package com.unciv.testing.simulations

import com.unciv.pure.application.battle.BattleTermination
import java.io.File
import java.util.Base64
import java.util.UUID

/** Arguments: preset|Units.json, battles-per-cell, maximum-amount, first-seed, report-directory. */
object BattleBalanceCli {
    private fun emit(vararg fields: Any) {
        println((listOf("BALANCE1") + fields.map { it.toString() }).joinToString("\t"))
        System.out.flush()
    }

    fun encode(text: String): String =
            Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            require(args.size == 5) { "Expected source, battles-per-cell, maximum-amount, first-seed, report-directory" }
            val config = BalanceExperimentConfig(args[1].toInt(), args[2].toInt(), args[3].toLong())
            val units =
                    if (args[0] == "preset") BalanceUnitSources.preset else BalanceUnitSources.load(
                        File(args[0])
                    )
            val ruleset = BalanceUnitSources.ruleset(units)
            val directory = File(args[4], "run-${System.currentTimeMillis()}-${UUID.randomUUID()}")
            check(directory.mkdirs()) { "Cannot create report directory: $directory" }
            File(directory, "parameters.txt").writeText(
                "Source: ${args[0]}\n$config\n" +
                        "Scenario: game BattleManager; shared AI; production 14x8 terrain; climate=0.3/0.5/0.6; terrainSeed=42; squads=4\n" +
                        "Luck=0.05; morale=0.1; maxTurns=1000; noProgressLimit=100\n" + units.joinToString(
                    "\n"
                )
            )
            val report = File(directory, "results.csv")
            emit("START", config.totalBattles(units.size), config.totalCells(units.size))
            units.forEachIndexed { index, unit ->
                emit(
                    "UNIT",
                    index,
                    encode(unit.name),
                    unit.speed,
                    unit.health,
                    unit.damage,
                    unit.rangedStrength
                )
            }
            report.bufferedWriter().use { writer ->
                writer.appendLine("attacker,defender,attackerAmount,defenderAmount,simulations,attackerWins,defenderWins,stalemates,maxTurns,other,averageTurns")
                val seeds = config.seeds()
                for ((ai, attacker) in units.withIndex()) for ((di, defender) in units.withIndex()) {
                    for (a in 1..config.maxAmount) for (d in 1..config.maxAmount) {
                        val batch = BattleBalanceSimulation.simulateBatch(
                            attacker.name, a, defender.name, d, seeds,
                            luckProbability = 0.05, moraleProbability = 0.1, ruleset = ruleset
                        )
                        val other = batch.results.count {
                            it.termination != BattleTermination.VICTORY &&
                                    it.termination != BattleTermination.STALEMATE &&
                                    it.termination != BattleTermination.MAX_TURNS
                        }
                        val metrics = listOf(
                            a, d, batch.simulations, batch.attackerWins, batch.defenderWins,
                            batch.stalemates, batch.maxTurnTerminations, other, batch.averageTurns
                        )
                        writer.appendLine(
                            (listOf(
                                attacker.name,
                                defender.name
                            ) + metrics).joinToString(",")
                        )
                        writer.flush()
                        emit("CELL", ai, di, *metrics.toTypedArray())
                    }
                }
            }
            emit("DONE", encode(report.absolutePath))
        } catch (failure: Exception) {
            emit("ERROR", encode(failure.message ?: failure.javaClass.simpleName))
            failure.printStackTrace(System.err)
            kotlin.system.exitProcess(1)
        }
    }
}
