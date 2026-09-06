package com.unciv.testing.simulations

import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.testing.pure.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.util.UUID

/** Headless boundary search. Arguments match BattleBalanceCli, but amount is the fixed attacker scale.
 * BOUNDARY1 records: START pairCount; SAMPLE uses BalanceCell fields; RESULT contains
 * attackerIndex, defenderIndex, status, attackerAmount, lowerDefenders, upperDefenders, probeCount.
 * Missing bounds use zero. DONE contains the base64 path of the boundary report.
 */
object BattleBoundaryCli {
    private fun emit(vararg fields: Any) {
        println((listOf("BOUNDARY1") + fields.map { it.toString() }).joinToString("\t"))
        System.out.flush()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            require(args.size == 5) { "Expected source, battles per probe, attacker scale, first seed, reports directory" }
            val config = BalanceExperimentConfig(args[1].toInt(), args[2].toInt(), args[3].toLong())
            val units =
                if (args[0] == "preset") BalanceUnitSources.preset else BalanceUnitSources.load(
                    File(args[0])
                )
            val maxDefenders = config.maxAmount * 100
            val directory =
                File(args[4], "boundary-${System.currentTimeMillis()}-${UUID.randomUUID()}")
            check(directory.mkdirs()) { "Cannot create $directory" }
            File(directory, "parameters.txt").writeText(
                "Source: ${args[0]}\n$config\nAttacker scale=${config.maxAmount}; defender search=1..$maxDefenders\n" +
                        "Near-half band=45..55%; integer bracket midpoint is an estimate, not a simulated fractional army.\n" +
                        "Seeded samples assume a single decreasing win-rate crossing as defenders increase.\n" +
                        "Scenario: sim-test 14x8 grid; allTilesReachable=true; useRealMovement=false\n" +
                        "Luck=0.05; morale=0.1; maxTurns=300; noProgressLimit=30\n" + units.joinToString(
                    "\n"
                )
            )
            val report = File(directory, "boundaries.csv")
            emit("START", units.size * units.size)
            val ruleset = BalanceUnitSources.ruleset(units)
            GameConstants.setTestingInstance(
                GameConstantsData(
                    luckProbability = 0.05,
                    moraleProbability = 0.1,
                    armySize = 1
                )
            )
            try {
                startKoin {
                    allowOverride(true); modules(
                    module { single<Ruleset> { ruleset } },
                    testModule
                )
                }
                report.bufferedWriter().use { summary ->
                    File(directory, "probes.csv").bufferedWriter().use { samples ->
                        summary.appendLine("attacker,defender,status,defendersPerAttacker,attackerAmount,lowerDefenders,upperDefenders,probes")
                        samples.appendLine("attacker,defender,attackerAmount,defenderAmount,simulations,attackerWins,defenderWins,stalemates,maxTurns,other,averageTurns")
                        val seeds = config.seeds()
                        for ((ai, attacker) in units.withIndex()) {
                            for ((di, defender) in units.withIndex()) {
                                val result = BalanceBoundarySearch.run(
                                    config.maxAmount,
                                    maxDefenders
                                ) { a, d ->
                                    val batch = BattleBalanceSimulation.simulateBatch(
                                        attacker.name, a, defender.name, d, seeds, 0.05, 0.1
                                    )
                                    val other =
                                        batch.simulations - batch.attackerWins - batch.defenderWins -
                                                batch.stalemates - batch.maxTurnTerminations
                                    val cell = BalanceCell(
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
                                    val metrics = listOf(
                                        a,
                                        d,
                                        cell.simulations,
                                        cell.attackerWins,
                                        cell.defenderWins,
                                        cell.stalemates,
                                        cell.maxTurns,
                                        cell.other,
                                        cell.averageTurns
                                    )
                                    samples.appendLine(
                                        (listOf(
                                            attacker.name,
                                            defender.name
                                        ) + metrics).joinToString(",")
                                    )
                                    samples.flush()
                                    emit("SAMPLE", ai, di, *metrics.toTypedArray())
                                    cell
                                }
                                summary.appendLine(
                                    listOf(
                                        attacker.name,
                                        defender.name,
                                        result.status,
                                        result.defendersPerAttacker ?: "",
                                        result.attackerAmount,
                                        result.lowerDefenders ?: "",
                                        result.upperDefenders ?: "",
                                        result.probes.size
                                    ).joinToString(",")
                                )
                                summary.flush()
                                emit(
                                    "RESULT",
                                    ai,
                                    di,
                                    result.status,
                                    result.attackerAmount,
                                    result.lowerDefenders ?: 0,
                                    result.upperDefenders ?: 0,
                                    result.probes.size
                                )
                            }
                        }
                    }
                }
                emit("DONE", BattleBalanceCli.encode(report.absolutePath))
            } finally {
                stopKoin()
                GameConstants.clearTestingInstance()
            }
        } catch (failure: Exception) {
            emit("ERROR", BattleBalanceCli.encode(failure.message ?: failure.javaClass.simpleName))
            failure.printStackTrace(System.err)
            kotlin.system.exitProcess(1)
        }
    }
}
