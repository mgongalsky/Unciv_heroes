package com.unciv.logic.battle

import java.util.WeakHashMap

internal data class BattleEffectProbabilities(
    val luck: Double?,
    val morale: Double?
)

private val effectProbabilityOverrides = WeakHashMap<BattleManager, BattleEffectProbabilities>()

internal fun BattleManager.configureEffectProbabilities(
    luckProbability: Double?,
    moraleProbability: Double?
) {
    require(luckProbability == null || luckProbability in 0.0..1.0)
    require(moraleProbability == null || moraleProbability in 0.0..1.0)
    synchronized(effectProbabilityOverrides) {
        effectProbabilityOverrides[this] = BattleEffectProbabilities(
            luck = luckProbability,
            morale = moraleProbability
        )
    }
}

internal fun BattleManager.configuredLuckProbability(default: Double): Double =
    synchronized(effectProbabilityOverrides) {
        effectProbabilityOverrides[this]?.luck ?: default
    }

internal fun BattleManager.configuredMoraleProbability(default: Double): Double =
    synchronized(effectProbabilityOverrides) {
        effectProbabilityOverrides[this]?.morale ?: default
    }
