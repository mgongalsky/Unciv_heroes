package com.unciv.ui.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable

/** Квадрат и галочка из образца Royal; ресурсы принадлежат Skin. */
class RoyalCheckBoxDrawable(
    skin: Skin,
    private val checked: Boolean,
    private val hovered: Boolean,
    private val disabled: Boolean,
    size: Float
) : BaseDrawable() {
    private val pixel = if (skin.has("royal-control-pixel", Texture::class.java)) {
        skin.get("royal-control-pixel", Texture::class.java)
    } else {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        val texture = try {
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap)
        } finally {
            pixmap.dispose()
        }
        skin.add("royal-control-pixel", texture, Texture::class.java)
        texture
    }
    private val mark = if (skin.has("royal-checkbox-mark", Texture::class.java)) {
        skin.get("royal-checkbox-mark", Texture::class.java)
    } else {
        Texture(Gdx.files.internal(RoyalUi.assetRoot + "icons/check.png")).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            skin.add("royal-checkbox-mark", it, Texture::class.java)
        }
    }
    private val border = Color.valueOf(if (hovered) "8a632e" else "baa06a")
    private val fill = Color.valueOf(if (hovered) "eadfbd" else "dad0af")
    private val ink = Color.valueOf("8a632e")

    init {
        minWidth = size
        minHeight = size
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        val side = minOf(width, height)
        if (side <= 0f) return
        val left = x + (width - side) / 2f
        val bottom = y + (height - side) / 2f
        val oldColor = batch.packedColor
        val red = batch.color.r
        val green = batch.color.g
        val blue = batch.color.b
        val alpha = batch.color.a * if (disabled) 0.42f else 1f
        fun tint(color: Color) {
            batch.setColor(red * color.r, green * color.g, blue * color.b, alpha)
        }
        try {
            tint(border)
            batch.draw(pixel, left, bottom, side, side)
            val edge = minOf(side / 2f, if (hovered) 2f else 1f)
            tint(fill)
            batch.draw(pixel, left + edge, bottom + edge, side - 2f * edge, side - 2f * edge)
            if (checked) {
                val inset = side * 3f / 32f
                tint(ink)
                batch.draw(mark, left + inset, bottom + inset, side - 2f * inset, side - 2f * inset)
            }
        } finally {
            batch.packedColor = oldColor
        }
    }
}
