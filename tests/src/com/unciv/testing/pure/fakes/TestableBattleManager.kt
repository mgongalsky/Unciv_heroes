package com.unciv.testing.pure.fakes

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.application.pathfinding.BattleMovementContext
import com.unciv.pure.application.pathfinding.MovementRangeUseCase
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.pathfinding.INavigableTile

class TestableBattleManager(
    attackerArmy: ArmyInfo,
    defenderArmy: ArmyInfo,
    battleField: IBattleField,
    random: IBattleRandom,
    private val allTilesReachable: Boolean = true,
    private val useRealMovement: Boolean = false
) : BattleManager(attackerArmy, defenderArmy, battleField, random) {

    private val troopPositions = mutableMapOf<TroopInfo, FakeBattleTile>()

    fun placeTroop(troop: TroopInfo, tile: FakeBattleTile) {
        tile.setTroop(troop)
        troopPositions[troop] = tile
    }

    override fun isReachableInCurrentTurn(troop: TroopInfo, targetTile: INavigableTile): Boolean {
        if (useRealMovement) {
            val startTile = getTroopCurrentTile(troop) ?: return false
            val reachable = MovementRangeUseCase.execute(
                startTile = startTile,
                unitMovement = troop.baseUnit.speed.toFloat(),
                context = BattleMovementContext()
            )
            return reachable.containsKey(targetTile)
        }
        return allTilesReachable
    }

    public override fun getTroopCurrentTile(troop: TroopInfo): IBattleTile? = troopPositions[troop]

    override fun moveTroop(troop: TroopInfo, targetTile: IBattleTile) {
        troopPositions[troop]?.setTroop(null)
        (targetTile as? FakeBattleTile)?.let {
            it.setTroop(troop)
            troopPositions[troop] = it
        }
    }
}
