package com.unciv.testing.simulations

import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleBatchResult
import com.unciv.pure.application.battle.BattleBatchSimulator
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.FakeGridBattleField
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.math.pow

class BattleSimulationSimTest {
    private data class Archetype(
        val name: String,
        val speed: Int,
        val health: Int,
        val damage: Int,
        val rangedStrength: Int = 0
    ) {
        val nominalForce: Double
            get() = health * (if (rangedStrength > 0) rangedStrength else damage) * speed.toDouble()
                .pow(0.3)
    }

    private data class BalancePoint(
        val attackerAmount: Int,
        val defenderAmount: Int,
        val batch: BattleBatchResult
    ) {
        val decisiveBattles: Int get() = batch.attackerWins + batch.defenderWins
        val attackerDecisiveWinRate: Double
            get() = if (decisiveBattles == 0) 0.5 else batch.attackerWins.toDouble() / decisiveBattles
        val distanceFromEven: Double get() = kotlin.math.abs(attackerDecisiveWinRate - 0.5)
    }

    private val archetypes = listOf(
        Archetype("Peasant", speed = 4, health = 5, damage = 2),
        Archetype("Swordsman", speed = 7, health = 50, damage = 15),
        Archetype("Archer", speed = 3, health = 15, damage = 5, rangedStrength = 7),
        Archetype("Spearman", speed = 5, health = 40, damage = 10),
        Archetype("Horseman", speed = 10, health = 60, damage = 20)
    )
    private val seeds = (0L until 20L).toList()

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.05, moraleProbability = 0.1, armySize = 1)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            unitTypes["Ranged"] = UnitType().apply { name = "Ranged" }
            archetypes.forEach { archetype ->
                units[archetype.name] = BaseUnit().apply {
                    name = archetype.name
                    unitType = if (archetype.rangedStrength > 0) "Ranged" else "Melee"
                    speed = archetype.speed
                    health = archetype.health
                    damage = archetype.damage
                    rangedStrength = archetype.rangedStrength
                }
            }
        }
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    @Test
    fun `generate seeded twenty by twenty balance matrices for all troop matchups`() {
        generateBalanceMatrices(mirror = false)
    }

    private fun simulateBatch(
        attacker: Archetype,
        attackerAmount: Int,
        defender: Archetype,
        defenderAmount: Int
    ): BattleBatchResult = BattleBalanceSimulation.simulateBatch(
        attacker = attacker.name,
        attackerAmount = attackerAmount,
        defender = defender.name,
        defenderAmount = defenderAmount,
        seeds = seeds,
        luckProbability = GameConstants.luckProbability,
        moraleProbability = GameConstants.moraleProbability
    )

    @Test
    fun `generate seeded mirror balance matrices for identical troops`() {
        generateBalanceMatrices(mirror = true)
    }
    private fun generateBalanceMatrices(mirror: Boolean) {
        val maxAmount = 20
        val matchups = if (mirror) {
            archetypes.map { it to it }
        } else {
            archetypes.flatMapIndexed { index, attacker ->
                archetypes.drop(index + 1).map { defender -> attacker to defender }
            }
        }
        val label = if (mirror) "Mirror" else "Balance"
        val totalBatches = matchups.size * maxAmount * maxAmount
        val totalSimulations = totalBatches * seeds.size
        val startedAt = System.nanoTime()
        var lastProgressAt = startedAt
        var completedBatches = 0
        var completedSimulations = 0
        val outcomes = mutableMapOf<BattleTermination, Int>()
        val report = mutableListOf<String>()
        val summary = mutableListOf<String>()
        val summaryTitle =
                if (mirror) "=== MIRROR SUMMARY ===" else "=== MEASURED EQUILIBRIUM SUMMARY ==="
        val projectRoot =
                generateSequence(java.io.File(System.getProperty("user.dir"))) { it.parentFile }
                    .first { java.io.File(it, "settings.gradle.kts").isFile }
        val reportFile = java.io.File(
            projectRoot,
            if (mirror) "troop-mirror-balance-report.txt" else "troop-balance-report.txt"
        )

        fun elapsed(): String = "%.1fs".format((System.nanoTime() - startedAt) / 1_000_000_000.0)
        fun record(line: String = "") {
            report += line
        }

        println("[$label] Planned: ${matchups.size} matchups, $totalBatches cells, $totalSimulations simulations (${seeds.size} seeds/cell).")
        println("[$label] Grid: 1..$maxAmount vs 1..$maxAmount; luck=5%; morale=10%; maxTurns=300.")
        println("[$label] Completed: 0/$totalSimulations (0%); elapsed ${elapsed()}")
        println("[$label] Report destination: ${reportFile.absolutePath}")
        if (mirror) record("Identical troop mirror matches")
        record("Legend: each cell is attacker decisive win rate; S=stalemate, M=max turns")
        record("Grid: 1..$maxAmount vs 1..$maxAmount; seeds per cell: ${seeds.size}; luck=5%; morale=10%; maxTurns=300")

        matchups.forEachIndexed { matchupIndex, (attacker, defender) ->
            val matchup = "${attacker.name} vs ${defender.name}"
            println("[$label] Matchup ${matchupIndex + 1}/${matchups.size}: $matchup")
            val points = mutableListOf<BalancePoint>()
            record()
            record("=== ${attacker.name} (rows) vs ${defender.name} (columns) ===")
            record("A\\D | " + (1..maxAmount).joinToString(" | ") { "%3d".format(it) })
            for (attackerAmount in 1..maxAmount) {
                val row = (1..maxAmount).map { defenderAmount ->
                    val batch = simulateBatch(attacker, attackerAmount, defender, defenderAmount)
                    assertEquals(seeds.size, batch.simulations)
                    completedBatches++
                    completedSimulations += batch.simulations
                    batch.results.forEach { result ->
                        outcomes[result.termination] = (outcomes[result.termination] ?: 0) + 1
                    }
                    val point = BalancePoint(attackerAmount, defenderAmount, batch)
                    points += point
                    val now = System.nanoTime()
                    val matchupFinished = attackerAmount == maxAmount && defenderAmount == maxAmount
                    if (completedBatches % (maxAmount * 5) == 0 ||
                            now - lastProgressAt >= 2_000_000_000L || matchupFinished
                    ) {
                        println(
                            "[$label] Completed: $completedSimulations/$totalSimulations " +
                                    "(%.1f%%); cells $completedBatches/$totalBatches; %s; elapsed %s".format(
                                        completedSimulations * 100.0 / totalSimulations,
                                        matchup,
                                        elapsed()
                                    )
                        )
                        lastProgressAt = now
                    }
                    val suffix = when {
                        batch.maxTurnTerminations > 0 -> "M"
                        batch.stalemates > 0 -> "S"
                        else -> ""
                    }
                    "%3.0f%s".format(point.attackerDecisiveWinRate * 100.0, suffix)
                }
                record(" %2d | %s".format(attackerAmount, row.joinToString(" | ")))
            }

            val boundary = points.minWithOrNull(
                compareBy<BalancePoint> { it.distanceFromEven }
                    .thenBy { it.attackerAmount + it.defenderAmount }
                    .thenBy { it.attackerAmount }
            )!!
            summary += if (mirror) {
                val equalAverage = points.filter { it.attackerAmount == it.defenderAmount }
                    .map { it.attackerDecisiveWinRate }.average()
                "%s: equal-count attacker average=%.1f%%; closest measured A:D=%d:%d (%.1f%% attacker wins); stalemates=%d; maxTurns=%d".format(
                    attacker.name, equalAverage * 100.0,
                    boundary.attackerAmount, boundary.defenderAmount,
                    boundary.attackerDecisiveWinRate * 100.0,
                    boundary.batch.stalemates, boundary.batch.maxTurnTerminations
                )
            } else {
                "%s vs %s: measured A:D=%d:%d (ratio %.3f:1, A wins %.0f%% decisive), nominal A:D=%.2f:1, stalemates=%d, maxTurns=%d".format(
                    attacker.name, defender.name,
                    boundary.attackerAmount, boundary.defenderAmount,
                    boundary.attackerAmount.toDouble() / boundary.defenderAmount,
                    boundary.attackerDecisiveWinRate * 100.0,
                    defender.nominalForce / attacker.nominalForce,
                    boundary.batch.stalemates, boundary.batch.maxTurnTerminations
                )
            }
        }

        record()
        record(summaryTitle)
        summary.forEach(::record)
        val totals = "[$label] Outcomes (all $completedSimulations simulations): " +
                BattleTermination.values().joinToString(", ") { "$it=${outcomes[it] ?: 0}" }
        record(totals)
        reportFile.writeText(report.joinToString(System.lineSeparator()))
        println(summaryTitle)
        summary.forEach { println(it) }
        println(totals)
        println("[$label] Finished: $completedSimulations/$totalSimulations simulations; elapsed ${elapsed()}")
        println("[$label] Report written to ${reportFile.absolutePath}")
    }
}

