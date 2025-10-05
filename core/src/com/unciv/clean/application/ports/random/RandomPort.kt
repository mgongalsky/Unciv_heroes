package com.unciv.clean.application.ports.random

/** Абстракция источника случайности для детерминизма и тестируемости. */
interface RandomPort {
    fun nextDouble(): Double
    fun nextInt(bound: Int): Int
}
