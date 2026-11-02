package com.unciv.ui.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable

/** Единая лента; выбранный участок задан в локальных координатах заголовка. */
class RoyalRibbonDrawable(
    skin: Skin,
    private val selection: () -> Pair<Float, Float>?
) : BaseDrawable() {
    private fun load(skin: Skin, file: String, variant: String = "original"): Texture {
        val key = "royal-ribbon-$file-$variant"
        if (skin.has(key, Texture::class.java)) return skin.get(key, Texture::class.java)
        val pixels = Pixmap(Gdx.files.internal(RoyalUi.assetRoot + "ui/navigation/$file.png"))
        val texture = try {
            pixels.setBlending(Pixmap.Blending.None)
            if (variant != "original") {
                for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                    val pixel = pixels.getPixel(x, y)
                    val r = (pixel ushr 24 and 255).toFloat()
                    val g = (pixel ushr 16 and 255).toFloat()
                    val b = (pixel ushr 8 and 255).toFloat()
                    val red: Float
                    val green: Float
                    val blue: Float
                    if (variant == "active") {
                        red = r * 0.058361f + g * 0.196329f + b * 0.019820f
                        green = r * 0.075035f + g * 0.252424f + b * 0.025482f
                        blue = r * 0.035433f + g * 0.119200f + b * 0.012033f
                    } else {
                        val gray = r * 0.2126f + g * 0.7152f + b * 0.0722f
                        red = gray + (r - gray) * 0.65f
                        green = gray + (g - gray) * 0.65f
                        blue = gray + (b - gray) * 0.65f
                    }
                    pixels.drawPixel(x, y,
                        (red.toInt().coerceIn(0, 255) shl 24) or
                        (green.toInt().coerceIn(0, 255) shl 16) or
                        (blue.toInt().coerceIn(0, 255) shl 8) or (pixel and 255))
                }
            }
            Texture(pixels)
        } finally {
            pixels.dispose()
        }
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        skin.add(key, texture, Texture::class.java)
        return texture
    }

    private val idle = load(skin, "ribbon", "idle")
    private val active = load(skin, "ribbon", "active")
    private val rails = load(skin, "shared-rails")
    private val corner = TextureRegion(load(skin, "quiet-corner"))
    private val oppositeCorner = TextureRegion(corner).apply { flip(true, true) }

    init {
        minHeight = 76f
        minWidth = 90f
        leftWidth = 45f
        rightWidth = 45f
    }

    /** Обрезает три участка по UV, не меняя ScissorStack родительского ScrollPane. */
    private fun strip(
        batch: Batch, texture: Texture, x: Float, y: Float, width: Float, height: Float,
        clipLeft: Float = 0f, clipRight: Float = width
    ) {
        val end = minOf(45f, width / 2f)
        fun segment(start: Float, finish: Float, uStart: Float, uFinish: Float) {
            val left = maxOf(start, clipLeft)
            val right = minOf(finish, clipRight)
            if (right <= left || finish <= start) return
            val uLeft = uStart + (uFinish - uStart) * (left - start) / (finish - start)
            val uRight = uStart + (uFinish - uStart) * (right - start) / (finish - start)
            batch.draw(texture, x + left, y, right - left, height, uLeft, 1f, uRight, 0f)
        }
        segment(0f, end, 0f, 0.08f)
        segment(end, width - end, 0.08f, 0.92f)
        segment(width - end, width, 0.92f, 1f)
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        strip(batch, idle, x, y, width, height)
        selection()?.let { (left, selectedWidth) ->
            val right = minOf(width, left + selectedWidth)
            val start = maxOf(0f, left)
            if (right > start) {
                strip(batch, active, x, y, width, height, start, right)
                strip(batch, rails, x, y, width, height)
                val size = minOf(17f, right - start, height)
                batch.draw(corner, x + start + 1f, y + height - 14f - size, size, size)
                batch.draw(oppositeCorner, x + right - 1f - size, y + 10f, size, size)
            }
        }
    }
}
