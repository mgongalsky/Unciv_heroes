package com.unciv.logic.battle.session

import com.unciv.logic.battle.BattleManager
import com.unciv.pure.application.battle.session.OrderedBattleSession
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.army.IArmy
import com.unciv.pure.domain.battle.IBattleField
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.troop.ITroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory

/** Creates isolated combat state. The caller supplies a fresh field and random source per battle. */
object BattleSessionSetup {
    data class Stack(val unitName: String, val count: Int)
    data class Side(val stacks: List<Stack>, val luck: Int = 1, val morale: Int = 0)

    fun create(
        attacker: Side,
        defender: Side,
        definitions: ITroopDefinitionSource,
        field: IBattleField,
        random: IBattleRandom,
        luckProbability: Double,
        moraleProbability: Double
    ): OrderedBattleSession {
        require(luckProbability in 0.0..1.0 && moraleProbability in 0.0..1.0)
        var nextId = 1
        fun army(side: Side): IArmy {
            require(side.stacks.size in 1..5)
            val army = Army(side.stacks.size)
            side.stacks.forEachIndexed { index, stack ->
                require(stack.unitName.isNotBlank() && stack.count > 0)
                val troop = TroopFactory.createWithId(nextId++, stack.unitName, stack.count, definitions)
                require(troop.speed > 0 && troop.maxHealth > 0 && troop.damage >= 0) {
                    "Invalid battle unit: ${stack.unitName}"
                }
                army.setTroopAt(index, troop)
            }
            return object : IArmy by army {
                override fun getBattleLuck(): Int = side.luck
                override fun getBattleMorale(): Int = side.morale
            }
        }
        val manager = BattleManager(
            army(attacker), army(defender), field, random,
            moraleProbability = moraleProbability, luckProbability = luckProbability
        )
        manager.initializeBattle()
        (manager.getAttackerArmy().getAllTroops() + manager.getDefenderArmy().getAllTroops())
            .filterNotNull().forEach { troop ->
                require(manager.getTroopTile(troop) != null) { "Missing starting tile for troop ${troop.id}" }
            }
        return OrderedBattleSession(BattleManagerSessionEngine(manager))
    }
}
