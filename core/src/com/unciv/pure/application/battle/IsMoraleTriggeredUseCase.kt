package com.unciv.pure.application.battle

import com.unciv.models.GameConstants
import com.unciv.pure.domain.battle.IBattleRandom

object IsMoraleTriggeredUseCase {
    fun execute(moraleValue: Int, random: IBattleRandom, moraleProbability: Double): Boolean {
        val effectiveProbability = if (moraleValue <= 3) {
            (moraleProbability / 3.0) * moraleValue
        } else {
            moraleProbability
        }
        return random.nextDouble() < effectiveProbability
    }
}
