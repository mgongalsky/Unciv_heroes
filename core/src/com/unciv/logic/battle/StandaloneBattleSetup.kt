package com.unciv.logic.battle

import com.unciv.logic.map.ClimateParameters
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.battle.BattlefieldTerrain
import com.unciv.pure.domain.army.Army
import com.unciv.pure.domain.battle.IBattleRandom
import com.unciv.pure.domain.battle.TacticalBattleField
import com.unciv.pure.domain.troop.RulesetTroopDefinitionSource
import com.unciv.pure.domain.troop.TroopFactory
import kotlin.random.Random

/** Input preparation only: all actions are executed by the game's BattleManager. */
object StandaloneBattleSetup {
    data class Stack(val unitName: String, val count: Int)

    fun army(stacks: List<Stack>, ruleset: Ruleset): Army {
        require(stacks.isNotEmpty() && stacks.size <= 5)
        val source = RulesetTroopDefinitionSource(ruleset)
        return Army(stacks.size).apply {
            stacks.forEachIndexed { index, stack ->
                require(stack.count > 0)
                val unit =
                    requireNotNull(ruleset.units[stack.unitName]) { "Unknown battle unit: ${stack.unitName}" }
                require(unit.speed > 0 && unit.health > 0 && unit.damage >= 0)
                setTroopAt(index, TroopFactory.create(stack.unitName, stack.count, source))
            }
        }
    }

    fun field(
        ruleset: Ruleset,
        attackerClimate: ClimateParameters,
        defenderClimate: ClimateParameters,
        width: Int = 14,
        height: Int = 8,
        terrainSeed: Long = 42L
    ): TacticalBattleField {
        val temperatureSeed = Random(terrainSeed).nextDouble()
        return TacticalBattleField.rectangular(width, height) { position ->
            val terrain = BattlefieldTerrain.at(
                position,
                width,
                attackerClimate,
                defenderClimate,
                temperatureSeed
            )
            // TileInfo.isImpassible uses the last terrain, including features.
            val effectiveTerrain = terrain.features.lastOrNull() ?: terrain.baseTerrain
            requireNotNull(ruleset.terrains[effectiveTerrain]) {
                "Missing battlefield terrain definition: $effectiveTerrain"
            }.impassable
        }
    }

    fun create(
        attacker: List<Stack>,
        defender: List<Stack>,
        ruleset: Ruleset,
        random: IBattleRandom,
        luckProbability: Double,
        moraleProbability: Double,
        attackerClimate: ClimateParameters = ClimateParameters(0.3, 0.5, 0.6),
        defenderClimate: ClimateParameters = ClimateParameters(0.3, 0.5, 0.6)
    ): BattleManager = BattleManager(
        army(attacker, ruleset), army(defender, ruleset),
        field(ruleset, attackerClimate, defenderClimate), random,
        moraleProbability = moraleProbability, luckProbability = luckProbability
    ).also { it.initializeBattle() }
}
