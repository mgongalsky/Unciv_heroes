package com.unciv.infrastructure.battle

import com.unciv.pure.domain.battle.IBattleRandom
import kotlin.random.Random

class SeededBattleRandom(seed: Long) : IBattleRandom {
    private val random = Random(seed)

    override fun nextDouble(): Double = random.nextDouble()
}
