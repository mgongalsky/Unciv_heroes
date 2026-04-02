package com.unciv.infrastructure.battle

import com.unciv.pure.domain.battle.IBattleRandom
import kotlin.random.Random

class RealBattleRandom : IBattleRandom {
    override fun nextDouble() = Random.nextDouble()
}
