package com.unciv.testing.simulations

import com.unciv.logic.army.ArmyInfo
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.testing.pure.fakes.FakeBattleRandom
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeBattleFieldBuilder
import com.unciv.testing.pure.fakes.FakeGridBattleField
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.fakes.TestableBattleManager
import com.unciv.testing.pure.testModule
import com.unciv.ai.AIBattle
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class BattleSimulationSimTest {

    private lateinit var fakeRuleset: Ruleset

    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.05,
                moraleProbability = 0.1,
                armySize = 5
            )
        )

        fakeRuleset = Ruleset().apply {
            val sword = UnitType().apply { name = "Sword" }
            val archery = UnitType().apply { name = "Archery" }
            val mounted = UnitType().apply { name = "Mounted" }
            unitTypes["Sword"] = sword
            unitTypes["Archery"] = archery
            unitTypes["Mounted"] = mounted

            units["Warrior"] = BaseUnit().apply {
                name = "Warrior"; unitType = "Sword"
                speed = 5; health = 20; damage = 5
            }
            units["Archer"] = BaseUnit().apply {
                name = "Archer"; unitType = "Archery"
                speed = 3; health = 15; damage = 5; rangedStrength = 7
            }
            units["Peasant"] = BaseUnit().apply {
                name = "Peasant"; unitType = "Sword"
                speed = 4; health = 5; damage = 2
            }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Sword"
                speed = 5; health = 40; damage = 10
            }
            units["Horseman"] = BaseUnit().apply {
                name = "Horseman"; unitType = "Mounted"
                speed = 10; health = 60; damage = 20
            }
        }

        startKoin {
            allowOverride(true)
            modules(
                module { single { fakeRuleset } },
                testModule
            )
        }
    }

    @After
    fun tearDown() {
        GameConstants.clearTestingInstance()
        stopKoin()
    }

    data class SimResult(val winner: String, val turns: Int)

    private fun simulateBattle(
        attackerSetup: ArmyInfo.() -> Unit,
        defenderSetup: ArmyInfo.() -> Unit
    ): SimResult {
        val grid = FakeBattleFieldBuilder.buildGrid(14, 8)
        val battleField = FakeGridBattleField(grid)
        val civInfo = FakeCivilizationInfo()

        val attackerArmy = ArmyInfo(civInfo, maxSlots = 5).apply(attackerSetup)
        val defenderArmy = ArmyInfo(civInfo, maxSlots = 5).apply(defenderSetup)

        val manager = TestableBattleManager(
            attackerArmy = attackerArmy,
            defenderArmy = defenderArmy,
            battleField = battleField,
            random = FakeBattleRandom(List(10000) { Math.random() }),
            allTilesReachable = true,
            useRealMovement = false
        )

        attackerArmy.getAllTroops().filterNotNull().forEachIndexed { i, troop ->
            manager.placeTroop(troop, grid[i % 8][0])
        }
        defenderArmy.getAllTroops().filterNotNull().forEachIndexed { i, troop ->
            manager.placeTroop(troop, grid[i % 8][13])
        }

        manager.initializeTurnQueue()
        val ai = AIBattle(manager)
        var turns = 0

        while (manager.isBattleOn() && turns < 500) {
            val troop = manager.getCurrentTroop() ?: break
            ai.performTurn(troop)
            manager.advanceTurn()
            turns++
        }

        val winner = if (attackerArmy.getAllTroops().any { (it?.currentAmount ?: 0) > 0 })
            "attacker" else "defender"
        return SimResult(winner, turns)
    }

    private fun runMatchup(name: String, attackerSetup: ArmyInfo.() -> Unit, defenderSetup: ArmyInfo.() -> Unit) {
        val results = (1..100).map { simulateBattle(attackerSetup, defenderSetup) }
        val attackerWins = results.count { it.winner == "attacker" }
        println("=== $name ===")
        println("Attacker wins: $attackerWins/100  Defender wins: ${100 - attackerWins}/100")
        println("Avg turns: ${"%.1f".format(results.map { it.turns }.average())}")
        println("Min turns: ${results.minOf { it.turns }}  Max turns: ${results.maxOf { it.turns }}")
        println()
    }

    @Test
    fun `simulate various matchups`() {
        runMatchup("Spearmen vs Spearmen",
            attackerSetup = { addUnits("Spearman", 10) },
            defenderSetup = { addUnits("Spearman", 10) }
        )
        runMatchup("Warriors vs Archers",
            attackerSetup = { addUnits("Warrior", 15) },
            defenderSetup = { addUnits("Archer", 10) }
        )
        runMatchup("Horsemen vs Spearmen",
            attackerSetup = { addUnits("Horseman", 5) },
            defenderSetup = { addUnits("Spearman", 20) }
        )
        runMatchup("Mixed (Spearmen + Archers) vs Pure Archers",
            attackerSetup = { addUnits("Spearman", 10); addUnits("Archer", 5) },
            defenderSetup = { addUnits("Archer", 15) }
        )
        runMatchup("Peasants vs Warriors",
            attackerSetup = { addUnits("Peasant", 30) },
            defenderSetup = { addUnits("Warrior", 10) }
        )
        runMatchup("Horsemen vs Archers",
            attackerSetup = { addUnits("Horseman", 5) },
            defenderSetup = { addUnits("Archer", 15) }
        )
    }
}
