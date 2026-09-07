package com.unciv.logic.battle

internal data class BattleEffectProbabilities(
    val luck: Double?,
    val morale: Double?
)

/** Overrides this battle's effect probabilities; null restores the constructor default. */
fun BattleManager.configureEffectProbabilities(
    luckProbability: Double?,
    moraleProbability: Double?
) {
    require(luckProbability == null || luckProbability in 0.0..1.0)
    require(moraleProbability == null || moraleProbability in 0.0..1.0)
    effectProbabilities = BattleEffectProbabilities(luckProbability, moraleProbability)
}

internal fun BattleManager.configuredLuckProbability(default: Double): Double =
        effectProbabilities?.luck ?: default

internal fun BattleManager.configuredMoraleProbability(default: Double): Double =
        effectProbabilities?.morale ?: default
