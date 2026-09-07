package com.unciv.testing.pure.fakes

import com.unciv.logic.battle.BattleManager
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.troop.Troop

/** Fixture placement only. All movement and combat use unmodified game rules. */
class TestableBattleManager(
    attackerArmy: IArmy,
    defenderArmy: IArmy,
    battleField: IBattleField,
    random: IBattleRandom,
    moraleProbability: Double = 0.0,
    luckProbability: Double = 0.0
) : BattleManager(
    attackerArmy,
    defenderArmy,
    battleField,
    random,
    moraleProbability,
    luckProbability
) {
    fun placeTroop(troop: Troop, tile: FakeBattleTile) {
        tile.receiveTroop(troop)
        setTroopPosition(troop, tile)
    }
}
