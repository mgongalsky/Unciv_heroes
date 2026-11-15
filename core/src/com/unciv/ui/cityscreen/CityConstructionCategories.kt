package com.unciv.ui.cityscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel

/** Младший свиток из handoff: фиксированные торцы, повторяемая бумага и выделение внутри её контура. */
internal class CityConstructionCategories(initial: String, onSelect: (String) -> Unit) : Table() {
    private val captions = listOf("Units", "Buildings", "Events")
    private var selected = captions.indexOf(initial).coerceAtLeast(0)
    private val labels = captions.map { it.toLabel(fontSize = 16, fontColor = Color.valueOf("365C85")) }

    init {
        val left = region("frames/junior-left.png")
        val right = region("frames/junior-right.png")
        val centre = region("frames/junior-centre-tile.png")
        val selectedPaper = selectionRegion()
        background = object : BaseDrawable() {
            override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                if (width <= 0f || height <= 0f) return
                val cap = minOf(height * 190f / 330f, width / 2f)
                val end = width - cap
                val tileWidth = height * 1690f / 330f
                val tabWidth = (width - 54f).coerceAtLeast(0f) / 3f
                val selectedStart = 27f + selected * tabWidth
                val selectedEnd = selectedStart + tabWidth
                fun slice(source: TextureRegion, tileX: Float, start: Float, finish: Float) {
                    if (finish <= start) return
                    val u1 = source.u + (source.u2 - source.u) * (start - tileX) / tileWidth
                    val u2 = source.u + (source.u2 - source.u) * (finish - tileX) / tileWidth
                    batch.draw(source.texture, x + start, y, finish - start, height,
                        u1, source.v2, u2, source.v)
                }
                var tileX = cap
                while (tileX < end) {
                    val tileEnd = minOf(tileX + tileWidth, end)
                    slice(centre, tileX, tileX, tileEnd)
                    slice(selectedPaper, tileX, maxOf(tileX, selectedStart), minOf(tileEnd, selectedEnd))
                    tileX += tileWidth
                }
                batch.draw(left, x, y, cap, height)
                batch.draw(right, x + width - cap, y, cap, height)
            }
        }
        padLeft(27f).padRight(27f)
        captions.forEachIndexed { index, caption ->
            val target = Table()
            target.add(labels[index]).minWidth(0f).growX()
            labels[index].setAlignment(com.badlogic.gdx.utils.Align.center)
            labels[index].setEllipsis(true)
            target.onClick {
                selected = index
                updateColors()
                onSelect(caption)
            }
            add(target).uniformX().growX().height(40f)
        }
        updateColors()
    }

    private fun updateColors() {
        labels.forEachIndexed { index, label ->
            label.color = if (index == selected) Color.valueOf("FFF0CB") else Color.valueOf("365C85")
            // Цвет Actor умножается на цвет стиля; базовый цвет остаётся белым.
            label.style = com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle(label.style).apply {
                fontColor = Color.WHITE
            }
        }
    }

    companion object {
        private const val root = "UI_bundles/Heroic-Civs-LibGDX-Handoff/heroic-civs-libgdx-handoff/libgdx-ready/"
        private fun region(path: String): TextureRegion {
            val skin = BaseScreen.skin
            val key = "heroic-city-category:$path"
            val texture = if (skin.has(key, Texture::class.java)) skin.get(key, Texture::class.java)
            else Texture(Gdx.files.internal(root + path)).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                skin.add(key, it, Texture::class.java)
            }
            return TextureRegion(texture)
        }

        /** Маска создаётся один раз по альфа-каналу бумаги, оставляя видимой её рисованную кромку. */
        private fun selectionRegion(): TextureRegion {
            val skin = BaseScreen.skin
            val key = "heroic-city-category:selection"
            if (skin.has(key, Texture::class.java)) return TextureRegion(skin.get(key, Texture::class.java))
            val paper = Pixmap(Gdx.files.internal(root + "frames/junior-centre-tile.png"))
            val cloth = Pixmap(Gdx.files.internal(root + "materials/azure/cloth-tab.png"))
            val mask = Pixmap(paper.width, paper.height, Pixmap.Format.RGBA8888)
            try {
                val inset = maxOf(1, (paper.height * 1.35f / 48f).toInt())
                mask.setBlending(Pixmap.Blending.None)
                for (x in 0 until paper.width) {
                    var first = -1
                    var last = -1
                    for (y in 0 until paper.height) {
                        if ((paper.getPixel(x, y) and 255) >= 192) {
                            if (first < 0) first = y
                            last = y
                        }
                    }
                    if (first < 0 || last - first <= 2 * inset) continue
                    for (y in first + inset..last - inset) {
                        if ((paper.getPixel(x, y) and 255) >= 192)
                            mask.drawPixel(x, y, cloth.getPixel(x % cloth.width, y % cloth.height))
                    }
                }
                val texture = Texture(mask).apply {
                    setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                }
                skin.add(key, texture, Texture::class.java)
                return TextureRegion(texture)
            } finally {
                mask.dispose()
                cloth.dispose()
                paper.dispose()
            }
        }
    }
}
