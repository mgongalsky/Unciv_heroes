// testing/pure/fakes/TestableBattleManager.kt
package com.unciv.testing.pure.fakes

import com.unciv.logic.army.ArmyInfo
import com.unciv.logic.army.TroopInfo
import com.unciv.logic.battle.BattleManager
import com.unciv.logic.battle.IBattleTile
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.pathfinding.INavigableTile
import com.unciv.pure.domain.battle.IBattleRandom

class TestableBattleManager(
    attackerArmy: ArmyInfo,
    defenderArmy: ArmyInfo,
    battleField: IBattleField,
    random: IBattleRandom,
    private val allTilesReachable: Boolean = true
) : BattleManager(attackerArmy, defenderArmy, battleField, random) {

    // Subclass and Override Method (Feathers)
    override fun isReachableInCurrentTurn(troop: TroopInfo, targetTile: INavigableTile): Boolean =
            allTilesReachable

    private val troopPositions = mutableMapOf<TroopInfo, FakeBattleTile>()

    fun placeTroop(troop: TroopInfo, tile: FakeBattleTile) {
        tile.setTroop(troop)
        troopPositions[troop] = tile
    }

    override fun getTroopCurrentTile(troop: TroopInfo): IBattleTile? = troopPositions[troop]

    override fun moveTroop(troop: TroopInfo, targetTile: INavigableTile) {
        // убираем с текущего тайла
        (troopPositions[troop])?.setTroop(null)
        // ставим на новый
        (targetTile as? FakeBattleTile)?.let {
            it.setTroop(troop)
            troopPositions[troop] = it
        }
    }
}
