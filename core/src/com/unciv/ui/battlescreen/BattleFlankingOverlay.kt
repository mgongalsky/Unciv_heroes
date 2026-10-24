package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.pure.application.battle.BattleFlankingAppearance

/** Owns its animation clock so repeated pointer updates do not restart the wave. */
internal class BattleFlankingOverlay(width: Float, height: Float) :
    Image(BattleSupportFillDrawable(width, height)) {
    private var currentLevel = 0
    private var previewLevel: Int? = null
    private var supported = false
    private var phase = 0f
    private var fixedPhase: Float? = null

    val hasFlanking: Boolean
        get() = currentLevel > 0 || (previewLevel ?: 0) > 0

    init {
        name = "battleFlankingOverlay"
        touchable = Touchable.disabled
        color.set(1f, 0.12f, 0.08f, 0f)
        isVisible = false
    }

    fun setSupported(value: Boolean) {
        if (supported == value) return
        supported = value
        refreshColor()
        Gdx.graphics?.requestRendering()
    }

    fun configure(current: Int, preview: Int?, fixed: Float?) {
        if (currentLevel != current || previewLevel != preview) phase = 0f
        currentLevel = current
        previewLevel = preview
        fixedPhase = fixed
        refreshColor()
        if (isPulsing()) Gdx.graphics?.requestRendering()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (!isPulsing()) return
        phase = (phase + delta.coerceAtLeast(0f) / BattleFlankingAppearance.PULSE_SECONDS) % 1f
        refreshColor()
        Gdx.graphics?.requestRendering()
    }

    private fun isPulsing(): Boolean = fixedPhase == null && previewLevel != null &&
        BattleFlankingAppearance.intensity(currentLevel) !=
        BattleFlankingAppearance.intensity(previewLevel!!)

    private fun refreshColor() {
        val tint = BattleFlankingAppearance.tint(currentLevel, previewLevel, supported, fixedPhase ?: phase)
        color.set(tint.r, tint.g, tint.b, tint.a)
        // Keep a transparent endpoint active so the next frame can animate it.
        isVisible = hasFlanking
    }
}
