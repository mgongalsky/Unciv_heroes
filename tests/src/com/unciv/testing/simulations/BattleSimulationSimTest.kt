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
        val maxAmount = 20
        val report = mutableListOf<String>()
        fun record(line: String = "") {
            report += line
            println(line)
        }

        record("Legend: each cell is attacker decisive win rate; S=stalemate, M=max turns")
        record("Grid: 1..$maxAmount vs 1..$maxAmount; seeds per cell: ${seeds.size}; luck=5%; morale=10%; maxTurns=300")

        val summary = mutableListOf<String>()
        archetypes.forEachIndexed { attackerIndex, attacker ->
            archetypes.drop(attackerIndex + 1).forEach { defender ->
                val points = mutableListOf<BalancePoint>()
                record()
                record("=== ${attacker.name} (rows) vs ${defender.name} (columns) ===")
                record("A\\D | " + (1..maxAmount).joinToString(" | ") { "%3d".format(it) })
                for (attackerAmount in 1..maxAmount) {
                    val row = (1..maxAmount).map { defenderAmount ->
                        val batch =
                                simulateBatch(attacker, attackerAmount, defender, defenderAmount)
                        assertEquals(seeds.size, batch.simulations)
                        val point = BalancePoint(attackerAmount, defenderAmount, batch)
                        points += point
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
                val predictedRatio = defender.nominalForce / attacker.nominalForce
                summary += "%s vs %s: measured A:D=%d:%d (ratio %.3f:1, A wins %.0f%% decisive), nominal A:D=%.2f:1, stalemates=%d, maxTurns=%d".format(
                    attacker.name,
                    defender.name,
                    boundary.attackerAmount,
                    boundary.defenderAmount,
                    boundary.attackerAmount.toDouble() / boundary.defenderAmount,
                    boundary.attackerDecisiveWinRate * 100.0,
                    predictedRatio,
                    boundary.batch.stalemates,
                    boundary.batch.maxTurnTerminations
                )
            }
        }

        record()
        record("=== MEASURED EQUILIBRIUM SUMMARY ===")
        summary.forEach(::record)

        val projectRoot =
                generateSequence(java.io.File(System.getProperty("user.dir"))) { it.parentFile }
                    .first { java.io.File(it, "settings.gradle.kts").isFile }
        val reportFile = java.io.File(projectRoot, "troop-balance-report.txt")
        reportFile.writeText(report.joinToString(System.lineSeparator()))
        record("Report written to ${reportFile.absolutePath}")
    }

    private fun simulateBatch(
        attacker: Archetype,
        attackerAmount: Int,
        defender: Archetype,
        defenderAmount: Int
    ): BattleBatchResult = BattleBatchSimulator.run(seeds) { seed ->
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        val manager = TestableBattleManager(
            attackerArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 1).apply {
                addUnits(attacker.name, attackerAmount)
            },
            defenderArmy = ArmyInfo(FakeCivilizationInfo(), maxSlots = 1).apply {
                addUnits(defender.name, defenderAmount)
            },
            battleField = FakeGridBattleField(grid),
            random = SeededBattleRandom(seed),
            allTilesReachable = true,
            useRealMovement = false,
            moraleProbability = GameConstants.moraleProbability,
            luckProbability = GameConstants.luckProbability
        )
        val attackerTroop = manager.getAttackerArmy().getAllTroops().filterNotNull().single()
        val defenderTroop = manager.getDefenderArmy().getAllTroops().filterNotNull().single()
        manager.placeTroop(attackerTroop, grid[0][0])
        manager.placeTroop(defenderTroop, grid[0][13])
        manager.initializeTurnQueue()
        val policy = AIBattlePolicy(manager)
        BattleSimulationRunner(
            manager = manager,
            attackerPolicy = policy,
            defenderPolicy = policy,
            maxTurns = 300,
            maxTurnsWithoutProgress = 30,
            seed = seed
        ).run()
    }

    @Test
    fun `generate seeded mirror balance matrices for identical troops`() {
        val maxAmount = 20
        val report = mutableListOf<String>()
        fun record(line: String = "") {
            report += line
            println(line)
        }

        record("Identical troop mirror matches")
        record("Grid: 1..$maxAmount vs 1..$maxAmount; seeds per cell: ${seeds.size}; luck=5%; morale=10%; maxTurns=300")
        val summary = mutableListOf<String>()

        archetypes.forEach { archetype ->
            val points = mutableListOf<BalancePoint>()
            record()
            record("=== ${archetype.name} attacker (rows) vs ${archetype.name} defender (columns) ===")
            record("A\\D | " + (1..maxAmount).joinToString(" | ") { "%3d".format(it) })
            for (attackerAmount in 1..maxAmount) {
                val row = (1..maxAmount).map { defenderAmount ->
                    val batch = simulateBatch(archetype, attackerAmount, archetype, defenderAmount)
                    assertEquals(seeds.size, batch.simulations)
                    val point = BalancePoint(attackerAmount, defenderAmount, batch)
                    points += point
                    val suffix = when {
                        batch.maxTurnTerminations > 0 -> "M"
                        batch.stalemates > 0 -> "S"
                        else -> ""
                    }
                    "%3.0f%s".format(point.attackerDecisiveWinRate * 100.0, suffix)
                }
                record(" %2d | %s".format(attackerAmount, row.joinToString(" | ")))
            }

            val equalAmountResults = points.filter { it.attackerAmount == it.defenderAmount }
            val equalAverage = equalAmountResults.map { it.attackerDecisiveWinRate }.average()
            val closest = points.minWithOrNull(
                compareBy<BalancePoint> { it.distanceFromEven }
                    .thenBy { it.attackerAmount + it.defenderAmount }
                    .thenBy { it.attackerAmount }
            )!!
            summary += "%s: equal-count attacker average=%.1f%%; closest measured A:D=%d:%d (%.1f%% attacker wins); stalemates=%d; maxTurns=%d".format(
                archetype.name,
                equalAverage * 100.0,
                closest.attackerAmount,
                closest.defenderAmount,
                closest.attackerDecisiveWinRate * 100.0,
                closest.batch.stalemates,
                closest.batch.maxTurnTerminations
            )
        }

        record()
        record("=== MIRROR SUMMARY ===")
        summary.forEach(::record)

        val projectRoot =
                generateSequence(java.io.File(System.getProperty("user.dir"))) { it.parentFile }
                    .first { java.io.File(it, "settings.gradle.kts").isFile }
        java.io.File(projectRoot, "troop-mirror-balance-report.txt")
            .writeText(report.joinToString(System.lineSeparator()))
    }
}

