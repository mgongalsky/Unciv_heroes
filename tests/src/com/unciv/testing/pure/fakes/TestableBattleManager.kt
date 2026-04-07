package com.unciv.testing.pure.fakes

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.battle.BattleManager
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
) : BattleManager(
    attackerArmy, defenderArmy, battleField, random,
    moraleProbability = 0.0,
    luckProbability = 0.0
) {

    fun placeTroop(troop: Troop, tile: FakeBattleTile) {
        tile.setTroop(troop)
        troopPositions[troop] = tile
    }

    override fun isReachableInCurrentTurn(troop: Troop, targetTile: INavigableTile): Boolean {
        if (useRealMovement) {
            val startTile = troopPositions[troop] as? FakeBattleTile ?: return false
            val reachable = MovementRangeUseCase.execute(
                startTile = startTile,
                unitMovement = troop.speed.toFloat(),
                context = BattleMovementContext()
            )
            return reachable.containsKey(targetTile)
        }
        return allTilesReachable
    }
}
