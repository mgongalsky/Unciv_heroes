package com.unciv.ui.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable

/** Общие ресурсы Royal Parchment; текстура принадлежит переданному Skin. */
object RoyalUi {
    const val assetRoot = "UI_bundles/Heroic-Civs-Royal-UI-Bundle/Heroic-Civs-Royal-UI-Bundle/assets/"

    /** Сохраняет имена стилей legacy, заменяя оформление стандартных кнопок. */
    fun installButtons(skin: Skin) {
        val textureName = "royal-button-texture"
        val texture = if (skin.has(textureName, Texture::class.java)) {
            skin.get(textureName, Texture::class.java)
        } else {
            Texture(Gdx.files.internal(assetRoot + "ui/buttons/button.png")).also {
                it.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                skin.add(textureName, it, Texture::class.java)
            }
        }
        // Исходные торцы занимают 16%; их экранная ширина — 25, высота плашки — 43.
        // Вертикальных срезов нет: орнамент сохраняет пропорции при обычной высоте кнопки.
        val end = (texture.width * 0.16f).toInt()
        val patch = NinePatch(texture, end, end, 0, 0).apply {
            scale(25f / end, 43f / texture.height)
            setPadding(25f, 25f, 9f, 9f)
        }
        val normal = NinePatchDrawable(patch).tint(Color(0.86f, 0.86f, 0.86f, 1f))
        val hover = NinePatchDrawable(patch)
        val pressed = NinePatchDrawable(patch).tint(Color(0.74f, 0.74f, 0.74f, 1f))
        val disabled = NinePatchDrawable(patch).tint(Color(0.65f, 0.65f, 0.65f, 0.42f))
        val cream = Color.valueOf("fff0cb")

        skin.get(Button.ButtonStyle::class.java).apply {
            up = normal
            over = hover
            down = pressed
            this.disabled = disabled
            checked = pressed
            checkedOver = hover
        }
        for (name in listOf("default", "positive", "negative", "disabled", "fantasy")) {
            if (!skin.has(name, TextButton.TextButtonStyle::class.java)) continue
            skin.get(name, TextButton.TextButtonStyle::class.java).apply {
                // Смысловые действия сохраняют цветовой акцент поверх общей плашки.
                up = when (name) {
                    "positive" -> NinePatchDrawable(patch).tint(Color(0.72f, 0.86f, 0.62f, 1f))
                    "negative" -> NinePatchDrawable(patch).tint(Color(1f, 0.62f, 0.52f, 1f))
                    "disabled" -> disabled
                    else -> normal
                }
                over = if (name == "disabled") disabled else hover
                down = if (name == "disabled") disabled else pressed
                this.disabled = disabled
                checked = pressed
                checkedOver = hover
                fontColor = cream.cpy()
                overFontColor = cream.cpy()
                downFontColor = cream.cpy()
                checkedFontColor = cream.cpy()
                disabledFontColor = Color(cream).apply { a = 0.55f }
            }
        }
    }

    /** Подключает две гарнитуры, сохраняя пользовательский шрифт и значки NativeBitmapFontData. */
    fun installTypography(skin: Skin) {
        val game = com.unciv.UncivGame.Current
        if (game.settings.fontFamily != Fonts.DEFAULT_FONT_FAMILY) return
        val platform = game.fontImplementation ?: return
        fun loadFont(name: String, family: String): com.badlogic.gdx.graphics.g2d.BitmapFont {
            if (skin.has(name, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java))
                return skin.get(name, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
            val data =
                NativeBitmapFontData(platform.withBundledFont(assetRoot + "fonts/$family/font.ttf"))
            return NativeBitmapFont(data).also {
                it.data.setScale(com.unciv.Constants.defaultFontSize / Fonts.ORIGINAL_FONT_SIZE)
                skin.add(name, it, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
            }
        }

        val body = loadFont("royal-body", "ebgaramond")
        val display = loadFont("royal-display", "berkshireswash")
        // Расширения Label.setFontSize используют этот же основной шрифт.
        Fonts.font = body
        skin.get(com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle::class.java).font = body
        skin.get(com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle::class.java).font = body
        skin.get(com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle::class.java).font =
            body
        skin.get(com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle::class.java).font = body
        skin.get(com.badlogic.gdx.scenes.scene2d.ui.SelectBox.SelectBoxStyle::class.java).apply {
            font = body
            listStyle.font = body
        }
        for (name in listOf("default", "positive", "negative", "disabled", "fantasy")) {
            if (skin.has(name, TextButton.TextButtonStyle::class.java))
                skin.get(name, TextButton.TextButtonStyle::class.java).font = display
        }
        if (skin.has("default", com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java))
            skin.get(com.badlogic.gdx.scenes.scene2d.ui.Window.WindowStyle::class.java).titleFont =
                display
        if (skin.has(
                "fantasyLabel",
                com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle::class.java
            )
        )
            skin.get(
                "fantasyLabel",
                com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle::class.java
            ).font = display
    }

    /** Меняет только индикаторы: подписи и локальные смысловые стили сохраняются. */
    fun installCheckBoxes(skin: Skin) {
        val style = skin.get(com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle::class.java)
        val size = maxOf(
            16f, style.checkboxOff.minWidth, style.checkboxOff.minHeight,
            style.checkboxOn.minWidth, style.checkboxOn.minHeight
        )
        style.checkboxOff = RoyalCheckBoxDrawable(skin, false, false, false, size)
        style.checkboxOn = RoyalCheckBoxDrawable(skin, true, false, false, size)
        style.checkboxOver = RoyalCheckBoxDrawable(skin, false, true, false, size)
        style.checkboxOnOver = RoyalCheckBoxDrawable(skin, true, true, false, size)
        style.checkboxOffDisabled = RoyalCheckBoxDrawable(skin, false, false, true, size)
        style.checkboxOnDisabled = RoyalCheckBoxDrawable(skin, true, false, true, size)
    }

    /** Вызывается после installCheckBoxes, создавшего общий белый пиксель в Skin. */
    fun installSliders(skin: Skin) {
        val pixel = skin.get("royal-control-pixel", Texture::class.java)
        for (vertical in listOf(false, true)) {
            val name = if (vertical) "default-vertical" else "default-horizontal"
            val style = skin.get(
                name,
                com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle::class.java
            )
            val width = maxOf(20f, style.knob?.minWidth ?: 0f)
            val height = maxOf(20f, style.knob?.minHeight ?: 0f)
            fun knob(emphasized: Boolean = false, disabled: Boolean = false) =
                    RoyalSliderDrawable(pixel, true, vertical, emphasized, disabled, width, height)

            fun track(disabled: Boolean = false) =
                    RoyalSliderDrawable(
                        pixel, false, vertical, false, disabled,
                        if (vertical) 5f else 0f, if (vertical) 0f else 5f
                    )
            style.background = track()
            style.disabledBackground = track(true)
            style.knob = knob()
            style.knobOver = knob(emphasized = true)
            style.knobDown = knob(emphasized = true)
            style.disabledKnob = knob(disabled = true)
            style.knobBefore = null
            style.knobAfter = null
            style.disabledKnobBefore = null
            style.disabledKnobAfter = null
        }
    }

    /** Оформляет поля после installCheckBoxes; общий пиксель остаётся собственностью Skin. */
    fun installTextFields(skin: Skin) {
        val pixel = skin.get("royal-control-pixel", Texture::class.java)
        val style =
                skin.get(com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle::class.java)
        val previous = style.background
        fun background(focused: Boolean = false, disabled: Boolean = false) =
                object : com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable() {
                    private val border = Color.valueOf(if (focused) "8a632e" else "baa06a")
                    private val paper = Color.valueOf(if (disabled) "d3c7a7" else "eadfbd")

                    init {
                        leftWidth = maxOf(7f, previous?.leftWidth ?: 0f)
                        rightWidth = maxOf(7f, previous?.rightWidth ?: 0f)
                        topHeight = maxOf(7f, previous?.topHeight ?: 0f)
                        bottomHeight = maxOf(7f, previous?.bottomHeight ?: 0f)
                        minWidth = previous?.minWidth ?: 0f
                        minHeight = previous?.minHeight ?: 0f
                    }

                    override fun draw(
                        batch: com.badlogic.gdx.graphics.g2d.Batch,
                        x: Float,
                        y: Float,
                        width: Float,
                        height: Float
                    ) {
                        if (width <= 0f || height <= 0f) return
                        val packed = batch.packedColor
                        val r = batch.color.r
                        val g = batch.color.g
                        val b = batch.color.b
                        val a = batch.color.a
                        try {
                            batch.setColor(r * border.r, g * border.g, b * border.b, a)
                            batch.draw(pixel, x, y, width, height)
                            val edge = minOf(if (focused) 2f else 1f, width / 2f, height / 2f)
                            batch.setColor(r * paper.r, g * paper.g, b * paper.b, a)
                            batch.draw(
                                pixel,
                                x + edge,
                                y + edge,
                                width - 2f * edge,
                                height - 2f * edge
                            )
                        } finally {
                            batch.packedColor = packed
                        }
                    }
                }

        fun solid(hex: String) = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
            com.badlogic.gdx.graphics.g2d.TextureRegion(pixel)
        ).tint(Color.valueOf(hex))

        style.background = background()
        style.focusedBackground = background(focused = true)
        style.disabledBackground = background(disabled = true)
        style.fontColor = Color.valueOf("453925")
        style.focusedFontColor = Color.valueOf("453925")
        style.disabledFontColor = Color.valueOf("70634c")
        style.messageFontColor = Color.valueOf("7f6d50")
        style.cursor = solid("453925").apply { minWidth = maxOf(1f, style.cursor?.minWidth ?: 1f) }
        style.selection = solid("d3bd8c")
    }
}
