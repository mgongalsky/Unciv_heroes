package com.unciv.pure.application.battle

import kotlin.math.cos

/** Presentation only: levels come from the combat core. Phase is measured in cycles. */
object BattleFlankingAppearance {
    const val PULSE_SECONDS = 1.6f

    data class Tint(val r: Float, val g: Float, val b: Float, val a: Float)

    fun intensity(level: Int): Float =
        if (level <= 0) 0f else (0.18f + 0.12f * level.coerceAtMost(5)).coerceAtMost(0.78f)

    private fun wave(phase: Float): Float =
        ((1.0 - cos(2.0 * Math.PI * phase)) / 2.0).toFloat()

    fun alpha(currentLevel: Int, previewLevel: Int?, phase: Float): Float {
        val current = intensity(currentLevel)
        val preview = intensity(previewLevel ?: currentLevel)
        if (current == preview) return current
        return current + (preview - current) * wave(phase)
    }

    /** Flanking takes visual priority; Support is the fallback at a non-flanked endpoint. */
    fun tint(currentLevel: Int, previewLevel: Int?, supported: Boolean, phase: Float): Tint {
        fun endpoint(level: Int): Tint = when {
            level > 0 -> Tint(1f, 0.12f, 0.08f, intensity(level))
            supported -> Tint(0.25f, 0.7f, 1f, 0.6f)
            else -> Tint(1f, 0.12f, 0.08f, 0f)
        }
        val current = endpoint(currentLevel)
        val preview = endpoint(previewLevel ?: currentLevel)
        if (current == preview) return current
        val blend = wave(phase)
        fun mix(from: Float, to: Float) = from + (to - from) * blend
        return Tint(mix(current.r, preview.r), mix(current.g, preview.g),
            mix(current.b, preview.b), mix(current.a, preview.a))
    }
}
