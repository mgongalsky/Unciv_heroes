package com.unciv.testing

import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.arena.ArenaBattleSetup
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.logic.map.ClimateParameters
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.RulesetCache
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.application.battle.BattleSimulationResult
import com.unciv.pure.application.battle.BattleTermination
import com.unciv.pure.domain.arena.ArenaArmyDistribution
import com.unciv.pure.domain.arena.ArenaMatchup
import com.unciv.testing.pure.testModule
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ArenaRealBattleTest {
    private var application: com.badlogic.gdx.backends.headless.HeadlessApplication? = null

    @Before
    fun setUp() {
        application = com.badlogic.gdx.backends.headless.HeadlessApplication(
            object : com.badlogic.gdx.ApplicationAdapter() {},
            com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration()
        )
        GameConstants.setTestingInstance(
            GameConstantsData(
                luckProbability = 0.05, moraleProbability = 0.1, armySize = 5
            )
        )
        RulesetCache.loadRulesets()
        val ruleset = ArenaBattleSetup.ruleset()
        startKoin { allowOverride(true); modules(module { single { ruleset } }, testModule) }
    }

    @After
    fun tearDown() {
        stopKoin()
        GameConstants.clearTestingInstance()
        application?.exit()
        application = null
    }

    @Test
    fun allArenaPairsFinishOnProductionField() {
        val failures = mutableListOf<String>()
        for (slots in 4..5) for (matchup in ArenaBattleSetup.matchups) {
            var wins = 0
            var draws = 0
            var turns = 0
            for (seed in 0L until 20L) {
                val result = simulate(matchup, seed, slots)
                turns += result.turns
                if (result.winnerIsAttacker == true) wins++
                if (result.termination == BattleTermination.MUTUAL_DEFEAT) draws++
                if (result.termination != BattleTermination.VICTORY &&
                        result.termination != BattleTermination.MUTUAL_DEFEAT
                ) {
                    failures.add("${matchup.playerUnit}/${matchup.opponentUnit} slots=$slots seed=$seed: ${result.termination}")
                }
                assertTrue(
                    "No combat events for $matchup slots=$slots seed=$seed",
                    result.events.any {
                        it is BattleEvent.TroopAttacked || it is BattleEvent.TroopShot
                    })
            }
            println("ARENA ${matchup.playerUnit}/${matchup.opponentUnit} slots=$slots: playerWins=$wins/20 draws=$draws averageActions=${turns / 20.0}")
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun sameSeedRepeatsOutcomeOnProductionField() {
        for (slots in 4..5) for (matchup in ArenaBattleSetup.matchups) {
            val first = simulate(matchup, 42L, slots)
            val second = simulate(matchup, 42L, slots)
            assertEquals("$matchup slots=$slots", first.termination, second.termination)
            assertEquals("$matchup slots=$slots", first.winnerIsAttacker, second.winnerIsAttacker)
            assertEquals("$matchup slots=$slots", first.turns, second.turns)
        }
    }

    @Test
    fun retryCreatesIndependentFullStrengthTroops() {
        val ruleset = ArenaBattleSetup.ruleset()
        for (slots in 4..5) {
            val first = ArenaBattleSetup.createArmy("Spearman", 26, ruleset, slots)
            val damaged = first.getAllTroops().filterNotNull()
            damaged.forEach {
                it.currentAmount = 1
                it.currentHealth = 1
                it.formation.current = 0
            }
            val retry = ArenaBattleSetup.createArmy("Spearman", 26, ruleset, slots)
            val fresh = retry.getAllTroops().filterNotNull()
            assertNotSame(first, retry)
            assertEquals(ArenaArmyDistribution.split(26, slots), fresh.map { it.currentAmount })
            assertEquals(slots, fresh.map { it.id }.distinct().size)
            fresh.forEachIndexed { index, troop ->
                assertNotSame(damaged[index], troop)
                assertEquals(troop.maxHealth, troop.currentHealth)
                assertEquals(troop.formation.maximum, troop.formation.current)
                assertEquals(1, damaged[index].currentAmount)
            }
        }
    }

    private fun simulate(matchup: ArenaMatchup, seed: Long, slots: Int): BattleSimulationResult {
        val ruleset = ArenaBattleSetup.ruleset()
        val playerCount = ((matchup.playerCount.toLong() * 130 + 99) / 100).toInt()
        val player = ArenaBattleSetup.createArmy(matchup.playerUnit, playerCount, ruleset, slots)
        val opponent = ArenaBattleSetup.createArmy(
            matchup.opponentUnit,
            matchup.opponentCount,
            ruleset,
            slots
        )
        assertEquals(
            ArenaArmyDistribution.split(playerCount, slots),
            player.getAllTroops().filterNotNull().map { it.currentAmount })
        assertEquals(
            ArenaArmyDistribution.split(matchup.opponentCount, slots),
            opponent.getAllTroops().filterNotNull().map { it.currentAmount })
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        val field = MapGenerator(ruleset).generateBattlefield(14, 8, climate, climate)
        val manager = BattleManager(
            player, opponent, field, SeededBattleRandom(seed),
            moraleProbability = 0.1, luckProbability = 0.05
        )
        manager.initializeBattle()
        val troops = player.getAllTroops().filterNotNull() + opponent.getAllTroops().filterNotNull()
        val tiles = troops.map { troop ->
            val tile = manager.getTroopTile(troop)
            assertNotNull("Troop was not placed: ${troop.unitName} slots=$slots", tile)
            assertSame(troop, tile!!.getTroop())
            tile
        }
        assertEquals("Overlapping starting positions", troops.size, tiles.distinct().size)
        assertEquals(troops.size, manager.getTurnQueue().size)
        val policy = AIBattlePolicy(manager)
        return BattleSimulationRunner(
            manager, policy, policy,
            maxTurns = 1000, maxTurnsWithoutProgress = 100, seed = seed
        ).run()
    }
}
