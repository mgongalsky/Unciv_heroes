package com.unciv.testing.pure.application.battle

import com.badlogic.gdx.math.Vector2
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.BattleSimulationRunner
import com.unciv.models.GameConstants
import com.unciv.models.GameConstantsData
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.ruleset.unit.UnitType
import com.unciv.pure.application.battle.BattleEvent
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop
import com.unciv.testing.pure.fakes.FakeBattleField
import com.unciv.testing.pure.fakes.FakeBattleTile
import com.unciv.testing.pure.fakes.FakeCivilizationInfo
import com.unciv.testing.pure.testModule
import com.unciv.pure.application.battle.BattleSimulationResult
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module


class BattleSimulationDeterminismTest {
    @Before
    fun setUp() {
        GameConstants.setTestingInstance(
            GameConstantsData(luckProbability = 0.5, moraleProbability = 0.0, armySize = 5)
        )
        val ruleset = Ruleset().apply {
            unitTypes["Melee"] = UnitType().apply { name = "Melee" }
            units["Spearman"] = BaseUnit().apply {
                name = "Spearman"; unitType = "Melee"; speed = 5; health = 100; damage = 10
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
    fun `same seed produces same AI battle outcome and normalized transcript`() {
        val first = simulate(987654321L)
        val second = simulate(987654321L)

        assertEquals(first.termination, second.termination)
        assertEquals(first.winnerIsAttacker, second.winnerIsAttacker)
        assertEquals(first.turns, second.turns)
        assertEquals(first.events.map(::normalize), second.events.map(::normalize))
        assertEquals(987654321L, first.seed)
        assertEquals(987654321L, second.seed)
    }

    private fun simulate(seed: Long): BattleSimulationResult {
        val attackerTile = FakeBattleTile(Vector2(0f, 0f))
        val defenderTile = FakeBattleTile(Vector2(1f, 0f))
        attackerTile.addNeighbor(defenderTile)
        defenderTile.addNeighbor(attackerTile)
        val civ = FakeCivilizationInfo()
        val attackerArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 8) }
        val defenderArmy = ArmyInfo(civ, 5).apply { addUnits("Spearman", 8) }
        val manager = DeterministicManager(
            attackerArmy,
            defenderArmy,
            FakeBattleField(listOf(attackerTile, defenderTile)),
            SeededBattleRandom(seed)
        )
        manager.place(attackerArmy.getAllTroops().filterNotNull().first(), attackerTile)
        manager.place(defenderArmy.getAllTroops().filterNotNull().first(), defenderTile)
        manager.initializeTurnQueue()
        val policy = AIBattlePolicy(manager)

        return BattleSimulationRunner(
            manager,
            attackerPolicy = policy,
            defenderPolicy = policy,
            maxTurns = 200,
            maxTurnsWithoutProgress = 20,
            seed = seed
        ).run()
    }

    private fun normalize(event: BattleEvent): String = when (event) {
        is BattleEvent.TroopMoved ->
            "move:${event.from}:${event.to}:${event.isMorale}"

        is BattleEvent.TroopAttacked ->
            "attack:${event.defenderRemainingAmount}:${event.isLuck}:${event.isMorale}:${event.defenderDied}"

        is BattleEvent.TroopShot ->
            "shoot:${event.defenderRemainingAmount}:${event.isLuck}:${event.isMorale}:${event.defenderDied}"

        is BattleEvent.TurnAdvanced -> "turn"
        is BattleEvent.BattleEnded -> "end:${event.winnerIsAttacker}"
        BattleEvent.TurnSkipped -> "skip"
    }

    private class DeterministicManager(
        attackerArmy: ArmyInfo,
        defenderArmy: ArmyInfo,
        battleField: IBattleField,
        random: IBattleRandom
    ) : BattleManager(
        attackerArmy,
        defenderArmy,
        battleField,
        random,
        moraleProbability = 0.0,
        luckProbability = 0.5
    ) {
        fun place(troop: Troop, tile: FakeBattleTile) {
            tile.receiveTroop(troop)
            setTroopPosition(troop, tile)
        }

        override fun isReachableInCurrentTurn(
            troop: Troop,
            targetTile: INavigableTile
        ): Boolean = true
    }
}
