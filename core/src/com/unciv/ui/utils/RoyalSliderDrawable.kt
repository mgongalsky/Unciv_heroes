package com.unciv.ui.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable

/** Геометрия дорожки и ромбовидного бегунка из CSS-образца Royal. */
class RoyalSliderDrawable(
    private val pixel: Texture,
    private val knob: Boolean,
    private val vertical: Boolean,
    private val emphasized: Boolean,
    private val disabled: Boolean,
    width: Float,
    height: Float
) : BaseDrawable() {
    private val line = Color.valueOf("baa06a")
    private val paper = Color.valueOf("eadfbd")
    private val accent = Color.valueOf(if (emphasized) "6f4c20" else "8a632e")
    private val track = Color.valueOf("bbb297")

    init {
        minWidth = width
        minHeight = height
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val saved = batch.packedColor
        val red = batch.color.r
        val green = batch.color.g
        val blue = batch.color.b
        val alpha = batch.color.a * if (disabled) 0.42f else 1f
        fun tint(color: Color) {
            batch.setColor(red * color.r, green * color.g, blue * color.b, alpha)
        }
        try {
            if (knob) {
                // Диагональ остаётся внутри размеров Drawable и области ввода Slider.
                val side = minOf(width, height) / 1.4142136f
                fun diamond(size: Float, color: Color) {
                    if (size <= 0f) return
                    tint(color)
                    batch.draw(pixel, x + (width - size) / 2f, y + (height - size) / 2f,
                        size / 2f, size / 2f, size, size, 1f, 1f, 45f,
                        0, 0, 1, 1, false, false)
                }
                diamond(side, line)
                diamond(side - 2f, paper)
                diamond(side - 6f, accent)
            } else {
                val w = if (vertical) minOf(5f, width) else width
                val h = if (vertical) height else minOf(5f, height)
                val left = x + (width - w) / 2f
                val bottom = y + (height - h) / 2f
                tint(line)
                batch.draw(pixel, left, bottom, w, h)
                val edge = minOf(1f, w / 2f, h / 2f)
                tint(track)
                batch.draw(pixel, left + edge, bottom + edge, w - 2f * edge, h - 2f * edge)
            }
        } finally {
            batch.packedColor = saved
        }
    }
}
