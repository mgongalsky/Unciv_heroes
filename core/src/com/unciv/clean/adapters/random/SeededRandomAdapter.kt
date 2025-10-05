package com.unciv.clean.adapters.random

import com.unciv.clean.application.ports.random.RandomPort
import kotlin.random.Random

class SeededRandomAdapter(seed: Long) : RandomPort {
    private val rnd = Random(seed)
    override fun nextDouble(): Double = rnd.nextDouble()
    override fun nextInt(bound: Int): Int = rnd.nextInt(bound)
}
