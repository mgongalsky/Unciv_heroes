package com.unciv.ui.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable

/**
 * Пергамент и рамка Royal Parchment. Все текстуры принадлежат Skin.
 * Границы Drawable включают внешний орнамент: прокрутка не отсекает выступы.
 * Срезы масштабируются отдельно от центральной области, сохраняя углы.
 */
class RoyalPanelDrawable(skin: Skin) : BaseDrawable() {
    private class Frame(
        val border: NinePatch,
        val crest: Texture?,
        val rail: Float,
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float,
        val crestWidth: Float,
        val crestHeight: Float
    )

    private fun texture(skin: Skin, path: String): Texture {
        val key = "royal-panel-$path"
        if (skin.has(key, Texture::class.java)) return skin.get(key, Texture::class.java)
        return Texture(Gdx.files.internal(RoyalUi.assetRoot + path)).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            skin.add(key, it, Texture::class.java)
        }
    }

    private fun frame(
        skin: Skin, name: String, rail: Float, slice: Int?,
        left: Float, right: Float, top: Float, bottom: Float,
        withCrest: Boolean
    ): Frame {
        val border = texture(skin, "ui/frames/$name/border.png")
        val ratio = if (name == "grand") 0.29f else 0.22f
        val horizontal = slice ?: (border.width * ratio).toInt()
        val vertical = slice ?: (border.height * ratio).toInt()
        val patch = NinePatch(border, horizontal, horizontal, vertical, vertical).apply {
            scale(rail / horizontal, rail / vertical)
        }
        val crest = if (withCrest) texture(skin, "ui/frames/$name/crest.png") else null
        return Frame(patch, crest, rail, left, right, top, bottom,
            (crest?.width ?: 0) * rail / horizontal,
            (crest?.height ?: 0) * rail / vertical)
    }

    private val grand = frame(skin, "grand", 65f, null,
        0.21016f, 0.20742f, 0.22940f, 0.26923f, true)
    private val compact = frame(skin, "compact", 32f, 108,
        0.23148f, 0.23611f, 0.43519f, 0.25463f, true)
    private val minimal = frame(skin, "minimal", 12f, null,
        0.22727f, 0.23864f, 0.35965f, 0.37719f, false)
    private val paperColor = Color.valueOf("eadfbd")
    private val paper: Texture = run {
        val key = "royal-panel-paper"
        if (skin.has(key, Texture::class.java)) skin.get(key, Texture::class.java)
        else {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            val result = try {
                pixmap.setColor(Color.WHITE)
                pixmap.fill()
                Texture(pixmap)
            } finally {
                pixmap.dispose()
            }
            skin.add(key, result, Texture::class.java)
            result
        }
    }

    init {
        // Постоянные отступы не вызывают циклов перевёрстки при смене размера рамки.
        leftWidth = 52f
        rightWidth = 52f
        topHeight = 58f
        bottomHeight = 52f
        minWidth = 128f
        minHeight = 128f
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val frame = when {
            width >= 600f && height >= 240f -> grand
            width >= 200f && height >= 140f -> compact
            else -> minimal
        }
        val scale = minOf(1f, width / (2f * frame.rail), height / (2f * frame.rail))
        val rail = frame.rail * scale
        val left = rail * frame.left
        val right = rail * frame.right
        val top = rail * frame.top
        val bottom = rail * frame.bottom
        val oldColor = batch.packedColor
        val color = batch.color
        val red = color.r
        val green = color.g
        val blue = color.b
        val alpha = color.a
        try {
            batch.setColor(red * paperColor.r, green * paperColor.g, blue * paperColor.b, alpha)
            batch.draw(paper, x + left, y + bottom,
                width - left - right, height - top - bottom)
            batch.packedColor = oldColor
            if (scale == 1f) frame.border.draw(batch, x, y, width, height)
            else {
                // Только для размеров меньше двух углов; общий ресурс не изменяется.
                NinePatch(frame.border).apply { scale(scale, scale) }
                    .draw(batch, x, y, width, height)
            }
            frame.crest?.let {
                val crestWidth = frame.crestWidth * scale
                val crestHeight = frame.crestHeight * scale
                batch.draw(it, x + (width - crestWidth) / 2f,
                    y + height - crestHeight, crestWidth, crestHeight)
            }
        } finally {
            batch.packedColor = oldColor
        }
    }
}
