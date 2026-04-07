package com.unciv.testing.pure.fakes

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.troop.Troop

class TestableBattleManager(
    attackerArmy: ArmyInfo,
    defenderArmy: ArmyInfo,
    battleField: IBattleField,
    random: IBattleRandom,
    private val allTilesReachable: Boolean = true,
    private val useRealMovement: Boolean = false
) : BattleManager(attackerArmy, defenderArmy, battleField, random, moraleProbability = 0.3, luckProbability = 0.3) {

    private val fakeTroopPositions = mutableMapOf<Troop, FakeBattleTile>()

    fun placeTroop(troop: Troop, tile: FakeBattleTile) {
        tile.setTroop(troop)
        troopPositions[troop] = tile  // пишем в родительский map
    }

    override fun isReachableInCurrentTurn(troop: Troop, targetTile: INavigableTile): Boolean {
        if (useRealMovement) {
            val startTile = fakeTroopPositions[troop] ?: return false
            val reachable = MovementRangeUseCase.execute(
                startTile = startTile,
                unitMovement = troop.speed.toFloat(),
                context = BattleMovementContext()
            )
            return reachable.containsKey(targetTile)
        }
        return allTilesReachable
    }

    override fun getTroopCurrentTile(troop: Troop): IBattleTile? = fakeTroopPositions[troop]

    override fun moveTroop(troop: Troop, targetTile: IBattleTile) {
        fakeTroopPositions[troop]?.setTroop(null)
        (targetTile as? FakeBattleTile)?.let {
            it.setTroop(troop)
            fakeTroopPositions[troop] = it
        }
    }
}
