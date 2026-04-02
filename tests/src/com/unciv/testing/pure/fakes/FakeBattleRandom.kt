package com.unciv.testing.pure.fakes

import com.unciv.pure.domain.battle.IBattleRandom

class FakeBattleRandom(private val values: List<Double>) : IBattleRandom {
    private var index = 0
    override fun nextDouble() = values[index++]
}
