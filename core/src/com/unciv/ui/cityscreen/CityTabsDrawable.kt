package com.unciv.ui.cityscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.unciv.ui.utils.BaseScreen

/** Городской scroll-azure-v15: бумага, выбранная ткань, непрерывные рейки и исходные торцы. */
internal class CityTabsDrawable(private val selection: () -> Pair<Float, Float>?) : BaseDrawable() {
    private val root = "UI_bundles/Heroic-Civs-LibGDX-Handoff/heroic-civs-libgdx-handoff/libgdx-ready/"
    private fun region(path: String): TextureRegion {
        val skin = BaseScreen.skin
        val key = "city-tabs:$path"
        val texture = if (skin.has(key, Texture::class.java)) skin.get(key, Texture::class.java)
        else Texture(Gdx.files.internal(root + path)).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            skin.add(key, it, Texture::class.java)
        }
        return TextureRegion(texture)
    }
    private val left = region("frames/azure/scroll-left.png")
    private val right = region("frames/azure/scroll-right.png")
    private val paper = region("frames/azure/scroll-centre-tile.png")
    private val cloth = region("materials/azure/cloth-tab.png")
    // В полном trim PNG середина содержит только золотые рейки, бумага прозрачна.
    private val rails = region("png/frames/azure/tabs-trim-420x64.png").let {
        TextureRegion(it, 80, 0, it.regionWidth - 160, it.regionHeight)
    }

    /** Повторение сохраняет пропорции материала и обрезает последний фрагмент по UV. */
    private fun repeat(batch: Batch, source: TextureRegion, x: Float, y: Float,
                       width: Float, height: Float, tileWidth: Float) {
        if (width <= 0f || height <= 0f || tileWidth <= 0f) return
        var offset = 0f
        while (offset < width) {
            val part = minOf(tileWidth, width - offset)
            batch.draw(source.texture, x + offset, y, part, height,
                source.u, source.v2, source.u + (source.u2 - source.u) * part / tileWidth, source.v)
            offset += tileWidth
        }
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val cap = minOf(height * 240f / 550f, width / 2f)
        val middleWidth = (width - 2f * cap).coerceAtLeast(0f)
        repeat(batch, paper, x + cap, y, middleWidth, height, height * 2000f / 550f)
        selection()?.let { (start, selectedWidth) ->
            val from = maxOf(cap, start)
            val to = minOf(width - cap, start + selectedWidth)
            // Координаты верхней и нижней границы бумаги взяты из scrollGround.azure.
            val bottom = y + height * (1f - 0.844f)
            val fillHeight = height * (0.844f - 0.206f)
            repeat(batch, cloth, x + from, bottom, to - from, fillHeight,
                fillHeight * cloth.regionWidth / cloth.regionHeight)
        }
        repeat(batch, rails, x + cap, y, middleWidth, height,
            height * rails.regionWidth / rails.regionHeight)
        batch.draw(left, x, y, cap, height)
        batch.draw(right, x + width - cap, y, cap, height)
    }
}
