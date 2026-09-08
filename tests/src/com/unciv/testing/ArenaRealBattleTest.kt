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

    private fun simulate(
        matchup: ArenaMatchup,
        seed: Long,
        slots: Int,
        bonusPercent: Int = 30
    ): BattleSimulationResult {
        val ruleset = ArenaBattleSetup.ruleset()
        val playerCount =
                ((matchup.playerCount.toLong() * (100L + bonusPercent) + 99) / 100).toInt()
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
            player,
            opponent,
            field,
            SeededBattleRandom(seed),
            moraleProbability = 0.1,
            luckProbability = 0.05
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
            manager,
            policy,
            policy,
            maxTurns = 1000,
            maxTurnsWithoutProgress = 100,
            seed = seed
        ).run()
    }

    @Test
    fun peasantArcherBalanceDiagnostic() {
        val ruleset = ArenaBattleSetup.ruleset()
        val report =
                StringBuilder("Peasant vs Archer; production field 14x8; climate 0.3/0.5/0.6; seeds 0..19; luck=0.05 morale=0.1; AIBattlePolicy on both sides\n")
        for (name in listOf("Peasant", "Archer")) {
            val unit = ruleset.units.getValue(name)
            report.append("$name: health=${unit.health} damage=${unit.damage} speed=${unit.speed} ranged=${unit.rangedStrength} formationHealth=${unit.formationHealthPercent} formationReduction=${unit.formationDamageReductionPercent}\n")
        }
        report.append("First attack counts are measured before the first chosen melee attack, excluding battles that never reach one.\n")
        report.append("peasants archers slots wins draws unfinished reachedAttack avgPeasantsAtAttack avgShotsBeforeAttack avgActions\n")
        val failures = mutableListOf<String>()
        for (count in listOf(20, 26, 40, 60, 80)) for (slots in listOf(1, 4, 5)) {
            var wins = 0
            var draws = 0
            var unfinished = 0
            var contacts = 0
            var peasantsAtContact = 0
            var shotsAtContact = 0
            var actions = 0
            for (seed in 0L until 20L) {
                val player = ArenaBattleSetup.createArmy("Peasant", count, ruleset, slots)
                val enemy = ArenaBattleSetup.createArmy("Archer", 5, ruleset, slots)
                val climate = ClimateParameters(0.3, 0.5, 0.6)
                val field = MapGenerator(ruleset).generateBattlefield(14, 8, climate, climate)
                if (count == 20 && slots == 1 && seed == 0L) {
                    report.append("FIELD: ").append(field.values.groupingBy {
                        "${it.baseTerrain}/${it.terrainFeatures.joinToString(",")}"
                    }.eachCount().toSortedMap()).append('\n')
                }
                val manager = BattleManager(
                    player, enemy, field, SeededBattleRandom(seed),
                    moraleProbability = 0.1, luckProbability = 0.05
                )
                manager.initializeBattle()
                val ai = AIBattlePolicy(manager)
                var firstAttack = false
                var shots = 0
                val observed = object : com.unciv.pure.application.battle.BattlePolicy {
                    override fun chooseCommand(troopId: Int): com.unciv.pure.application.battle.BattleCommand? {
                        val command = ai.chooseCommand(troopId)
                        if (!firstAttack && command is com.unciv.pure.application.battle.BattleCommand.Attack) {
                            firstAttack = true
                            contacts++
                            peasantsAtContact += player.getAllTroops().filterNotNull()
                                .sumOf { it.currentAmount }
                            shotsAtContact += shots
                        }
                        if (!firstAttack && command is com.unciv.pure.application.battle.BattleCommand.Shoot) shots++
                        return command
                    }
                }
                val result = BattleSimulationRunner(
                    manager, observed, observed,
                    maxTurns = 1000, maxTurnsWithoutProgress = 100, seed = seed
                ).run()
                actions += result.turns
                if (result.winnerIsAttacker == true) wins++
                if (result.termination == BattleTermination.MUTUAL_DEFEAT) draws++
                if (result.termination != BattleTermination.VICTORY && result.termination != BattleTermination.MUTUAL_DEFEAT) {
                    unfinished++
                    failures.add("count=$count slots=$slots seed=$seed: ${result.termination}")
                }
                if (count == 26 && slots == 4 && seed == 0L) {
                    report.append("TRACE 26 vs 5, slots=4, seed=0:\n")
                    result.events.forEach { report.append(it).append('\n') }
                }
            }
            report.append("$count 5 $slots $wins $draws $unfinished $contacts ")
            report.append(if (contacts == 0) "n/a n/a" else "${peasantsAtContact.toDouble() / contacts} ${shotsAtContact.toDouble() / contacts}")
            report.append(" ${actions / 20.0}\n")
        }
        val projectRoot =
                generateSequence(java.io.File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .firstOrNull { java.io.File(it, "android/assets").isDirectory }
                    ?: error("Cannot locate project root for arena diagnostic report")
        val output = java.io.File(projectRoot, "tests/balance-results/arena-peasant-diagnostic.txt")
        output.parentFile.mkdirs()
        output.writeText(report.toString())
        println(report)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun peasantArcherCalibration() {
        val report =
                StringBuilder("Production 14x8 field, climate 0.3/0.5/0.6; AIBattlePolicy both sides; seeds 20..59; no additional bonus\npeasants archers slots wins samples unfinished\n")
        val failures = mutableListOf<String>()
        for (count in 28..40 step 2) for (slots in 4..5) {
            var wins = 0
            var unfinished = 0
            for (seed in 20L until 60L) {
                val result = simulate(
                    ArenaMatchup("Peasant", "Archer", count, 5),
                    seed,
                    slots,
                    bonusPercent = 0
                )
                if (result.winnerIsAttacker == true) wins++
                if (result.termination != BattleTermination.VICTORY && result.termination != BattleTermination.MUTUAL_DEFEAT) {
                    unfinished++
                    failures.add("count=$count slots=$slots seed=$seed: ${result.termination}")
                }
            }
            report.append("$count 5 $slots $wins 40 $unfinished\n")
        }
        val root =
                generateSequence(java.io.File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .firstOrNull { java.io.File(it, "android/assets").isDirectory }
                    ?: error("Cannot locate project root")
        val output = java.io.File(root, "tests/balance-results/arena-peasant-calibration.txt")
        output.parentFile.mkdirs()
        output.writeText(report.toString())
        println(report)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun peasantArcherTierWinRates() {
        val matchup =
                ArenaBattleSetup.matchups.single { it.playerUnit == "Peasant" && it.opponentUnit == "Archer" }
        val report =
                StringBuilder("Production field 14x8; climate 0.3/0.5/0.6; AIBattlePolicy both sides; validation seeds 60..159\nbonus peasants archers slots wins samples unfinished\n")
        val failures = mutableListOf<String>()
        for (bonus in listOf(30, 20, 10, 0)) for (slots in 4..5) {
            var wins = 0
            var unfinished = 0
            val encounter = com.unciv.pure.domain.arena.ArenaGenerator.generate(
                listOf(matchup), 0L, com.unciv.pure.domain.arena.ArenaTierConfig(1, bonus)
            ).single()
            for (seed in 60L until 160L) {
                val result = simulate(matchup, seed, slots, bonus)
                if (result.winnerIsAttacker == true) wins++
                if (result.termination != BattleTermination.VICTORY && result.termination != BattleTermination.MUTUAL_DEFEAT) unfinished++
            }
            report.append("$bonus ${encounter.playerCount} ${matchup.opponentCount} $slots $wins 100 $unfinished\n")
            if (unfinished != 0) failures.add("bonus=$bonus slots=$slots: $unfinished unfinished battles")
            // Tier 1 should be forgiving under the reference policy; later tiers must remain winnable.
            val minimumWins = if (bonus == 30) 90 else 1
            if (wins < minimumWins) failures.add("bonus=$bonus slots=$slots: $wins/100 wins, expected at least $minimumWins")
        }
        val root =
                generateSequence(java.io.File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
                    .firstOrNull { java.io.File(it, "android/assets").isDirectory }
                    ?: error("Cannot locate project root")
        val output = java.io.File(root, "tests/balance-results/arena-peasant-tier-validation.txt")
        output.parentFile.mkdirs()
        output.writeText(report.toString())
        println(report)
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun standaloneBattleMatchesGameAdapter() {
        val ruleset = ArenaBattleSetup.ruleset()
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        for (slots in 4..5) for (seed in 0L until 10L) {
            val gameField = MapGenerator(ruleset).generateBattlefield(14, 8, climate, climate)
            val gameManager = BattleManager(
                ArenaBattleSetup.createArmy("Peasant", 47, ruleset, slots),
                ArenaBattleSetup.createArmy("Archer", 5, ruleset, slots),
                gameField, SeededBattleRandom(seed), moraleProbability = 0.1, luckProbability = 0.05
            )
            gameManager.initializeBattle()
            fun stacks(name: String, count: Int) = ArenaArmyDistribution.split(count, slots).map {
                com.unciv.logic.battle.StandaloneBattleSetup.Stack(name, it)
            }

            val standalone = com.unciv.logic.battle.StandaloneBattleSetup.create(
                stacks("Peasant", 47), stacks("Archer", 5), ruleset, SeededBattleRandom(seed),
                luckProbability = 0.05, moraleProbability = 0.1
            )
            val label = "slots=$slots seed=$seed"
            for (tile in gameField.values) {
                val other =
                        standalone.battleField.getTileAt(tile.position) as com.unciv.logic.battle.IBattleTile
                assertEquals(label, tile.isImpassible(), other.isImpassible())
                assertEquals(
                    label,
                    tile.neighbors.map { it.position }.toList(),
                    other.neighbors.map { it.position }.toList()
                )
            }
            fun troopIds(manager: BattleManager) =
                    (manager.getAttackerArmy().getAllTroops() + manager.getDefenderArmy()
                        .getAllTroops())
                        .filterNotNull().mapIndexed { index, troop -> troop.id to index }.toMap()

            val gameIds = troopIds(gameManager)
            val standaloneIds = troopIds(standalone)
            fun normalize(events: List<BattleEvent>, ids: Map<Int, Int>): List<String> =
                    events.map { event ->
                        Regex("(troopId|attackerId|defenderId|nextTroopId)=([0-9]+)").replace(event.toString()) { match ->
                            "${match.groupValues[1]}=${ids.getValue(match.groupValues[2].toInt())}"
                        }
                    }

            val gameEvents = mutableListOf<BattleEvent>()
            val gameAi = com.unciv.ai.AIBattle(gameManager) { gameEvents.add(it) }
            var actions = 0
            while (gameManager.isBattleOn() && actions < 1000) {
                val troop = requireNotNull(gameManager.getCurrentTroop())
                val result = gameAi.performTurn(troop)
                assertTrue("Game AI rejected action: $label $result", result?.success == true)
                gameManager.completeAction(result)
                actions++
            }
            assertFalse("Game adapter did not finish: $label", gameManager.isBattleOn())
            val policy = AIBattlePolicy(standalone)
            val result = BattleSimulationRunner(standalone, policy, policy, seed = seed).run()
            assertEquals(label, BattleTermination.VICTORY, result.termination)
            assertEquals(label, actions, result.turns)
            assertEquals(
                label,
                gameManager.getBattleResult()?.winningArmy === gameManager.getAttackerArmy(),
                result.winnerIsAttacker
            )
            assertEquals(
                label,
                normalize(gameEvents, gameIds),
                normalize(result.events, standaloneIds)
            )
            fun state(manager: BattleManager, ids: Map<Int, Int>): List<String> =
                    (manager.getAttackerArmy().getAllTroops() + manager.getDefenderArmy()
                        .getAllTroops()).map { troop ->
                        if (troop == null) "empty" else
                            "${ids.getValue(troop.id)}:${troop.currentAmount}:${troop.currentHealth}:${troop.formation.current}:${
                                manager.getTroopTile(
                                    troop
                                )?.toPoint()
                            }"
                    }
            assertEquals(label, state(gameManager, gameIds), state(standalone, standaloneIds))
        }
    }
    private fun mixedManager(
        encounter: com.unciv.pure.domain.arena.ArenaBattleDefinition,
        seed: Long
    ): BattleManager {
        val ruleset = ArenaBattleSetup.ruleset()
        val player = ArenaBattleSetup.createArmyFromStacks(
            encounter.playerArmy,
            ruleset,
            encounter.troopSlots
        )
        val opponent = ArenaBattleSetup.createArmyFromStacks(
            encounter.opponentArmy,
            ruleset,
            encounter.troopSlots
        )
        assertEquals(
            encounter.playerArmy.map { it.unitName to it.count },
            player.getAllTroops().filterNotNull().map { it.unitName to it.currentAmount })
        assertEquals(
            encounter.opponentArmy.map { it.unitName to it.count },
            opponent.getAllTroops().filterNotNull().map { it.unitName to it.currentAmount })
        val climate = ClimateParameters(0.3, 0.5, 0.6)
        val manager = BattleManager(
            player, opponent, MapGenerator(ruleset).generateBattlefield(14, 8, climate, climate),
            SeededBattleRandom(seed), moraleProbability = 0.1, luckProbability = 0.05
        )
        manager.initializeBattle()
        val troops = (player.getAllTroops() + opponent.getAllTroops()).filterNotNull()
        val tiles = troops.map { troop ->
            val tile = manager.getTroopTile(troop)
            assertNotNull("Unplaced mixed troop: ${troop.unitName}", tile)
            assertSame(troop, tile!!.getTroop())
            tile
        }
        assertEquals(troops.size, tiles.distinct().size)
        assertEquals(troops.size, manager.getTurnQueue().size)
        return manager
    }

    @Test
    fun mixedTemplatesFinishAcrossTierBonuses() {
        for (template in ArenaBattleSetup.mixedMatchups) for (bonus in listOf(30, 20, 10, 0)) {
            val encounter = com.unciv.pure.domain.arena.ArenaBattleDefinition.mixed(template, bonus)
            val batch =
                    com.unciv.pure.application.battle.BattleBatchSimulator.run(0L until 20L) { seed ->
                        val manager = mixedManager(encounter, seed)
                        val policy = AIBattlePolicy(manager)
                        BattleSimulationRunner(
                            manager, policy, policy,
                            maxTurns = 1000, maxTurnsWithoutProgress = 100, seed = seed
                        ).run()
                    }
            println(
                "ARENA MIXED ${template.id} bonus=$bonus seeds=0..19 " +
                        "attackerWins=${batch.attackerWins} defenderWins=${batch.defenderWins} " +
                        "stalemates=${batch.stalemates} maxTurns=${batch.maxTurnTerminations} " +
                        "averageActions=${batch.averageTurns} medianActions=${batch.medianTurns}"
            )
            batch.results.forEachIndexed { index, result ->
                assertTrue(
                    "${template.id} bonus=$bonus seed=$index: ${result.termination}",
                    result.termination == BattleTermination.VICTORY ||
                            result.termination == BattleTermination.MUTUAL_DEFEAT
                )
                assertTrue(result.events.any { it is BattleEvent.TroopAttacked || it is BattleEvent.TroopShot })
            }
        }
    }

    @Test
    fun mixedSeedRepeatsNormalizedTranscript() {
        for (template in ArenaBattleSetup.mixedMatchups) {
            val encounter = com.unciv.pure.domain.arena.ArenaBattleDefinition.mixed(template, 30)
            fun transcript(): Pair<BattleSimulationResult, List<String>> {
                val manager = mixedManager(encounter, 42L)
                val ids = (manager.getAttackerArmy().getAllTroops() + manager.getDefenderArmy()
                    .getAllTroops())
                    .filterNotNull().mapIndexed { index, troop -> troop.id to index }.toMap()
                val policy = AIBattlePolicy(manager)
                val result = BattleSimulationRunner(
                    manager, policy, policy,
                    maxTurns = 1000, maxTurnsWithoutProgress = 100, seed = 42L
                ).run()
                val events = result.events.map { event ->
                    Regex("(troopId|attackerId|defenderId|nextTroopId)=([0-9]+)").replace(event.toString()) { match ->
                        "${match.groupValues[1]}=${ids.getValue(match.groupValues[2].toInt())}"
                    }
                }
                return result to events
            }

            val first = transcript()
            val second = transcript()
            assertEquals(template.id, first.first.termination, second.first.termination)
            assertEquals(template.id, first.first.winnerIsAttacker, second.first.winnerIsAttacker)
            assertEquals(template.id, first.first.turns, second.first.turns)
            assertEquals(template.id, first.second, second.second)
        }
    }

    @Test
    fun mixedRetriesCreateIndependentFullStrengthArmies() {
        val ruleset = ArenaBattleSetup.ruleset()
        for (template in ArenaBattleSetup.mixedMatchups) {
            val encounter = com.unciv.pure.domain.arena.ArenaBattleDefinition.mixed(template, 30)
            for (stacks in listOf(encounter.playerArmy, encounter.opponentArmy)) {
                val first =
                        ArenaBattleSetup.createArmyFromStacks(stacks, ruleset, encounter.troopSlots)
                val damaged = first.getAllTroops().filterNotNull()
                damaged.forEach {
                    it.currentAmount = 1
                    it.currentHealth = 1
                    it.formation.current = 0
                }
                val retry =
                        ArenaBattleSetup.createArmyFromStacks(stacks, ruleset, encounter.troopSlots)
                val fresh = retry.getAllTroops().filterNotNull()
                assertNotSame(first, retry)
                assertEquals(
                    stacks.map { it.unitName to it.count },
                    fresh.map { it.unitName to it.currentAmount })
                assertEquals(fresh.size, fresh.map { it.id }.distinct().size)
                fresh.forEachIndexed { index, troop ->
                    assertNotSame(damaged[index], troop)
                    assertEquals(troop.maxHealth, troop.currentHealth)
                    assertEquals(troop.formation.maximum, troop.formation.current)
                    assertEquals(1, damaged[index].currentAmount)
                }
            }
        }
    }
}
