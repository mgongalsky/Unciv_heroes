package com.unciv.logic.battle

import com.unciv.Constants
import com.unciv.ai.AIBattlePolicy
import com.unciv.infrastructure.battle.SeededBattleRandom
import com.unciv.logic.city.CityInfo
import com.unciv.logic.city.NeutralCityEntry
import com.unciv.logic.map.ClimateParameters
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileMap
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.pure.application.battle.BattleSimulationResult
import com.unciv.pure.domain.city.NeutralCityEntryRules.Decision

/** Shared tactical setup; neither side requires a substitute civilization. */
class NeutralCityBattleSession private constructor(
    val battleField: TileMap,
    val manager: BattleManager,
    val seed: Long,
    private val hero: MapUnit,
    private val city: CityInfo
) {
    private var attackConsumed = false

    fun consumeWorldAttack() {
        if (attackConsumed) return
        attackConsumed = true
        hero.attacksThisTurn += 1
        hero.currentMovement = 0f
        hero.action = null
        hero.attacksSinceTurnStart.add(com.badlogic.gdx.math.Vector2(city.getCenterTile().position))
    }

    fun runAutomated(maxTurns: Int = 1_000): BattleSimulationResult {
        require(maxTurns > 0) { "maxTurns must be positive" }
        manager.initializeBattle()
        val policy = AIBattlePolicy(manager)
        return BattleSimulationRunner(
            manager, policy, policy, maxTurns = maxTurns,
            maxTurnsWithoutProgress = 100, seed = seed
        ).run()
    }

    companion object {
        fun canStart(hero: MapUnit, city: CityInfo): Boolean =
            NeutralCityEntry.decision(city, hero) == Decision.FIGHT_GARRISON &&
                hero.currentMovement > Constants.minimumMovementEpsilon && hero.canAttack() &&
                hero.currentTile.aerialDistanceTo(city.getCenterTile()) == 1 &&
                city.getCenterTile().militaryUnit == null &&
                hero.army.getAllTroops().any { it != null && it.currentAmount > 0 }

        fun create(hero: MapUnit, city: CityInfo, seed: Long = 42L): NeutralCityBattleSession {
            require(canStart(hero, city)) { "Hero cannot attack this neutral garrison" }
            val field = MapGenerator(city.getRuleset()).generateBattlefield(
                14, 8,
                hero.currentTile.getClimateParameters() ?: ClimateParameters(0.3, 0.5, 0.6),
                city.getCenterTile().getClimateParameters() ?: ClimateParameters(0.3, 0.5, 0.6)
            )
            val manager = BattleManager(hero.army, city.garrisonInfo, field, SeededBattleRandom(seed))
            return NeutralCityBattleSession(field, manager, seed, hero, city)
        }
    }
}
