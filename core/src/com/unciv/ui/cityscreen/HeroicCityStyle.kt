package com.unciv.ui.cityscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Scaling
import com.unciv.ui.utils.BaseScreen

/** Локальное оформление города. Текстурами владеет Skin; общие стили не изменяются. */
internal object HeroicCityStyle {
    private const val root = "UI_bundles/Heroic-Civs-LibGDX-Handoff/heroic-civs-libgdx-handoff/libgdx-ready/"

    private fun region(path: String): TextureRegion {
        val skin = BaseScreen.skin
        val key = "heroic-city:$path"
        val texture = if (skin.has(key, Texture::class.java)) skin.get(key, Texture::class.java)
        else Texture(Gdx.files.internal(root + path)).also {
            it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            skin.add(key, it, Texture::class.java)
        }
        return TextureRegion(texture)
    }

    /** Повторяет фрагмент без растяжения; неполный последний тайл обрезается через UV. */
    private fun tile(batch: Batch, source: TextureRegion, x: Float, y: Float,
                     width: Float, height: Float, tileWidth: Float, tileHeight: Float) {
        if (width <= 0f || height <= 0f || tileWidth <= 0f || tileHeight <= 0f) return
        var dy = 0f
        while (dy < height) {
            val h = minOf(tileHeight, height - dy)
            var dx = 0f
            while (dx < width) {
                val w = minOf(tileWidth, width - dx)
                batch.draw(source.texture, x + dx, y + dy, w, h,
                    source.u, source.v2,
                    source.u + (source.u2 - source.u) * w / tileWidth,
                    source.v2 + (source.v - source.v2) * h / tileHeight)
                dx += tileWidth
            }
            dy += tileHeight
        }
    }

    /** Геометрия brocade-frame.mjs: угол 82, оси x=16/y=21, полосы 11.2 и 8.65. */
    fun panel(): Drawable {
        val paper = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
        )
        val corner = region("frames/azure/panel-corner-tl.png")
        val topRight = TextureRegion(corner).apply { flip(true, false) }
        val bottomLeft = TextureRegion(corner).apply { flip(false, true) }
        val bottomRight = TextureRegion(corner).apply { flip(true, true) }
        val horizontal = region("frames/azure/panel-rail-horizontal.png")
        val vertical = region("frames/azure/panel-rail-vertical.png")
        return object : BaseDrawable() {
            init {
                leftWidth = 24f
                rightWidth = 24f
                topHeight = 30f
                bottomHeight = 30f
            }
            override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                if (width <= 0f || height <= 0f) return
                val scale = minOf(1f, width / 164f, height / 164f)
                val c = 82f * scale
                val axisX = 16f * scale
                val axisY = 21f * scale
                val horizontalHeight = 11.2f * scale
                val verticalWidth = 8.65f * scale
                paper.draw(batch, x + axisX, y + axisY,
                    width - 2f * axisX, height - 2f * axisY)
                // Сначала прямые участки, затем исходные углы поверх стыков, как в SVG.
                for (railY in floatArrayOf(y + axisY - horizontalHeight / 2f,
                    y + height - axisY - horizontalHeight / 2f)) {
                    tile(batch, horizontal, x + axisX, railY, width - 2f * axisX,
                        horizontalHeight, 32f * scale, horizontalHeight)
                }
                for (railX in floatArrayOf(x + axisX - verticalWidth / 2f,
                    x + width - axisX - verticalWidth / 2f)) {
                    tile(batch, vertical, railX, y + axisY, verticalWidth,
                        height - 2f * axisY, verticalWidth, verticalWidth * 200f / 54f)
                }
                batch.draw(corner, x, y + height - c, c, c)
                batch.draw(topRight, x + width - c, y + height - c, c, c)
                batch.draw(bottomLeft, x, y, c, c)
                batch.draw(bottomRight, x + width - c, y, c, c)
            }
        }
    }

    /** Пики сохраняют геометрию dense; размер герба следует footerDimensions из бандла. */
    fun mapOverlay(): Drawable {
        val head = region("frames/azure/pike-head-left.png")
        val shaft = region("frames/azure/pike-shaft-tile-left.png")
        val foot = region("frames/azure/pike-foot-left.png")
        val rightHead = TextureRegion(head).apply { flip(true, false) }
        val rightShaft = TextureRegion(shaft).apply { flip(true, false) }
        val rightFoot = TextureRegion(foot).apply { flip(true, false) }
        val shield = region("frames/azure/footer-shield-banner.png")
        return object : BaseDrawable() {
            override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                if (width <= 0f || height <= 0f) return
                val s = width / 3072f * minOf(1f, height / width * 3072f / 1496f) * 0.58f
                val headHeight = 1400f * s
                val headWidth = 1536f * s
                val footHeight = 96f * s
                val shaftWidth = 400f * s
                val bodyHeight = (height - headHeight - footHeight).coerceAtLeast(0f)
                tile(batch, shaft, x, y + footHeight, shaftWidth, bodyHeight, shaftWidth, 24f * s)
                tile(
                    batch, rightShaft, x + width - shaftWidth, y + footHeight,
                    shaftWidth, bodyHeight, shaftWidth, 24f * s
                )
                batch.draw(foot, x, y, shaftWidth, footHeight)
                batch.draw(rightFoot, x + width - shaftWidth, y, shaftWidth, footHeight)
                batch.draw(head, x, y + height - headHeight, headWidth, headHeight)
                batch.draw(
                    rightHead,
                    x + width - headWidth,
                    y + height - headHeight,
                    headWidth,
                    headHeight
                )
                // 65% — множитель базового размера, а не доля всей ширины карты.
                val baseWidth = (width * 0.58f).coerceIn(220f, 380f)
                val shieldWidth = minOf(width * 0.78f, baseWidth * 0.65f)
                val shieldHeight = shieldWidth * shield.regionHeight / shield.regionWidth
                batch.draw(shield, x + (width - shieldWidth) / 2f, y, shieldWidth, shieldHeight)
            }
        }
    }

    /** Отдельный декоративный Actor при необходимости не перехватывает ввод. */
    fun footer(): Image = Image(region("frames/azure/footer-shield-banner.png")).apply {
        setScaling(Scaling.fit)
        touchable = Touchable.disabled
    }

    /** Синяя ткань только городских кнопок; доступность и обработчики сохраняются. */
    fun applyActionStyle(button: TextButton) {
        val inactive = button.isDisabled || button.touchable == Touchable.disabled
        val previous = button.style
        if (previous is CityButtonStyle && previous.inactive == inactive) return
        fun background(state: String): Drawable {
            val skin = BaseScreen.skin
            val key = "heroic-city-button-azure:$state"
            if (skin.has(key, Drawable::class.java)) return skin.get(key, Drawable::class.java)
            val left = region("png/skin/lancet-left.png")
            val right = region("png/skin/lancet-right.png")
            val source = region("png/skin/azure/button-base-$state.9.png")
            val content = TextureRegion(
                source.texture,
                1,
                1,
                source.regionWidth - 2,
                source.regionHeight - 2
            )
            val patch = NinePatch(content, 8, 8, 8, 8).apply {
                scale(0.5f, 0.5f)
                setPadding(26f, 26f, 8f, 8f)
            }
            val base = NinePatchDrawable(patch)
            return object : BaseDrawable(base) {
                init {
                    minWidth = 104f; minHeight = 44f
                }

                override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
                    base.draw(batch, x, y, width, height)
                    batch.draw(left, x + 8f, y + (height - 32f) / 2f, 18f, 32f)
                    batch.draw(right, x + width - 26f, y + (height - 32f) / 2f, 18f, 32f)
                }
            }.also { skin.add(key, it, Drawable::class.java) }
        }
        button.style = CityButtonStyle(previous, inactive).apply {
            up = background(if (inactive) "disabled" else "up")
            over = background(if (inactive) "disabled" else "over")
            down = background(if (inactive) "disabled" else "down")
            checked = down
            checkedOver = over
            disabled = background("disabled")
            fontColor = Color.valueOf("FFF0CB")
            overFontColor = fontColor.cpy()
            downFontColor = fontColor.cpy()
            checkedFontColor = fontColor.cpy()
            disabledFontColor = fontColor.cpy()
        }
    }

    /** Маркер локальной копии позволяет не создавать стили повторно при обходе видимых контролов. */
    private class CityButtonStyle(source: TextButton.TextButtonStyle, val inactive: Boolean) :
        TextButton.TextButtonStyle(source)

    /** Подхватывает новые кнопки, сохраняя выбранный размер локального оформления. */
    fun applyButtons(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
        if (actor is com.badlogic.gdx.scenes.scene2d.ui.CheckBox) return
        if (actor is TextButton) {
            if (actor.style is CompactButtonStyle) applyCompactStyle(actor)
            else applyActionStyle(actor)
            return
        }
        if (actor is com.badlogic.gdx.scenes.scene2d.Group)
            for (child in actor.children) applyButtons(child)
    }

    /** Маркер компактных действий: общий обход не заменяет их крупным стилем с ланцетами. */
    private class CompactButtonStyle(source: TextButton.TextButtonStyle, val inactive: Boolean) :
        TextButton.TextButtonStyle(source)

    /** Небольшая тканевая кнопка без боковых росчерков; общий BitmapFont не изменяется. */
    fun applyCompactStyle(button: TextButton) {
        val inactive = button.isDisabled || button.touchable == Touchable.disabled
        val previous = button.style
        if (previous is CompactButtonStyle && previous.inactive == inactive) return
        fun background(state: String): Drawable {
            val skin = BaseScreen.skin
            val key = "heroic-city-compact-azure:$state"
            if (skin.has(key, Drawable::class.java)) return skin.get(key, Drawable::class.java)
            val source = region("png/skin/azure/button-base-$state.9.png")
            val content = TextureRegion(
                source.texture,
                1,
                1,
                source.regionWidth - 2,
                source.regionHeight - 2
            )
            val patch = NinePatch(content, 8, 8, 8, 8).apply {
                scale(0.5f, 0.5f)
                setPadding(8f, 8f, 4f, 4f)
            }
            return NinePatchDrawable(patch).apply {
                minWidth = 32f
                minHeight = 32f
            }.also { skin.add(key, it, Drawable::class.java) }
        }
        button.style = CompactButtonStyle(previous, inactive).apply {
            font = com.unciv.ui.utils.Fonts.font
            up = background(if (inactive) "disabled" else "up")
            over = background(if (inactive) "disabled" else "over")
            down = background(if (inactive) "disabled" else "down")
            checked = down
            checkedOver = over
            disabled = background("disabled")
            fontColor = Color.valueOf("FFF0CB")
            overFontColor = fontColor.cpy()
            downFontColor = fontColor.cpy()
            checkedFontColor = fontColor.cpy()
            disabledFontColor = fontColor.cpy()
        }
        button.label.setFontScale(16f / com.unciv.ui.utils.Fonts.ORIGINAL_FONT_SIZE)
        button.label.setWrap(false)
        button.label.setEllipsis(true)
        button.labelCell.pad(0f)
        button.pad(4f, 8f, 4f, 8f)
    }
}
