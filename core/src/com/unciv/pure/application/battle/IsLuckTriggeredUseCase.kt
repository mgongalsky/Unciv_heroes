package com.unciv.pure.application.battle

import com.unciv.pure.domain.battle.IBattleRandom

object IsLuckTriggeredUseCase {
    fun execute(luckValue: Int, random: IBattleRandom, luckProbability: Double): Boolean {
        val effectiveProbability = if (luckValue <= 3) {
            (luckProbability / 3.0) * luckValue
        } else {
            luckProbability
        }
        return random.nextDouble() < effectiveProbability
    }
}
