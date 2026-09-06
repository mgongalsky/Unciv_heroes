package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import com.unciv.ui.images.ImageGetter

/** A tintable, hollow hexagon using the atlas white pixel, without allocating a texture. */
internal class BattleControlOutlineDrawable(width: Float, height: Float) : BaseDrawable(),
    TransformDrawable {
    private val vertices = FloatArray(20)
    private val points = floatArrayOf(
        0.5f, 1f, 1f, 0.75f, 1f, 0.25f,
        0.5f, 0f, 0f, 0.25f, 0f, 0.75f
    )

    init {
        minWidth = width
        minHeight = height
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        draw(batch, x, y, 0f, 0f, width, height, 1f, 1f, 0f)
    }

    override fun draw(
        batch: Batch, x: Float, y: Float, originX: Float, originY: Float,
        width: Float, height: Float, scaleX: Float, scaleY: Float, rotation: Float
    ) {
        val region = ImageGetter.getDrawable("OtherIcons/whiteDot").region
        val u = (region.u + region.u2) / 2f
        val v = (region.v + region.v2) / 2f
        val cos = MathUtils.cosDeg(rotation)
        val sin = MathUtils.sinDeg(rotation)
        val tint = batch.packedColor

        fun vertex(slot: Int, point: Int, radius: Float) {
            val px = ((0.5f + (points[point * 2] - 0.5f) * radius) * width - originX) * scaleX
            val py = ((0.5f + (points[point * 2 + 1] - 0.5f) * radius) * height - originY) * scaleY
            val offset = slot * 5
            vertices[offset] = x + originX + px * cos - py * sin
            vertices[offset + 1] = y + originY + px * sin + py * cos
            vertices[offset + 2] = tint
            vertices[offset + 3] = u
            vertices[offset + 4] = v
        }

        for (point in 0 until 6) {
            val next = (point + 1) % 6
            vertex(0, point, 0.96f)
            vertex(1, next, 0.96f)
            vertex(2, next, 0.90f)
            vertex(3, point, 0.90f)
            batch.draw(region.texture, vertices, 0, vertices.size)
        }
    }
}
