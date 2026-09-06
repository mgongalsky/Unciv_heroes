package com.unciv.testing.simulations

import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.testing.pure.testModule
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.File
import java.util.Base64
import java.util.UUID

/** Standalone headless worker. stdout carries versioned tab-separated records, stderr diagnostics.
 * Arguments: preset|Units.json, battles-per-cell, maximum-amount, first-seed, report-directory.
 * CELL fields: attacker index, defender index, attacker amount, defender amount,
 * simulations, attacker wins, defender wins, stalemates, max turns, other, average turns.
 */
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
            val directory = File(args[4], "run-${System.currentTimeMillis()}-${UUID.randomUUID()}")
            check(directory.mkdirs()) { "Cannot create report directory: $directory" }
            File(directory, "parameters.txt").writeText(
                "Source: ${args[0]}\n$config\nLuck=0.05; morale=0.1; maxTurns=300; noProgressLimit=30\n" +
                        "Scenario: sim-test 14x8 grid; allTilesReachable=true; useRealMovement=false\n" +
                        units.joinToString("\n")
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
                report.bufferedWriter().use { writer ->
                    writer.appendLine("attacker,defender,attackerAmount,defenderAmount,simulations,attackerWins,defenderWins,stalemates,maxTurns,other,averageTurns")
                    val seeds = config.seeds()
                    for ((attackerIndex, attacker) in units.withIndex()) {
                        for ((defenderIndex, defender) in units.withIndex()) {
                            for (attackerAmount in 1..config.maxAmount) {
                                for (defenderAmount in 1..config.maxAmount) {
                                    val batch = BattleBalanceSimulation.simulateBatch(
                                        attacker.name,
                                        attackerAmount,
                                        defender.name,
                                        defenderAmount,
                                        seeds,
                                        luckProbability = 0.05,
                                        moraleProbability = 0.1
                                    )
                                    val other = batch.results.count {
                                        it.termination != BattleTermination.VICTORY &&
                                                it.termination != BattleTermination.STALEMATE &&
                                                it.termination != BattleTermination.MAX_TURNS
                                    }
                                    val metrics = listOf(
                                        attackerAmount, defenderAmount, batch.simulations,
                                        batch.attackerWins, batch.defenderWins, batch.stalemates,
                                        batch.maxTurnTerminations, other, batch.averageTurns
                                    )
                                    writer.appendLine(
                                        (listOf(
                                            attacker.name,
                                            defender.name
                                        ) + metrics).joinToString(",")
                                    )
                                    writer.flush()
                                    emit(
                                        "CELL",
                                        attackerIndex,
                                        defenderIndex,
                                        *metrics.toTypedArray()
                                    )
                                }
                            }
                        }
                    }
                }
                emit("DONE", encode(report.absolutePath))
            } finally {
                stopKoin()
                GameConstants.clearTestingInstance()
            }
        } catch (failure: Exception) {
            emit("ERROR", encode(failure.message ?: failure.javaClass.simpleName))
            failure.printStackTrace(System.err)
            kotlin.system.exitProcess(1)
        }
    }
}
