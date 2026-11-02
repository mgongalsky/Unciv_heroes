package com.unciv.app.desktop

import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.NativeBitmapFont
import com.unciv.ui.utils.NativeBitmapFontData

/** Проверяет освобождение собственных страниц шрифта в настоящем GL-контексте. */
internal object NativeFontGoldenChecks {
    fun open(screen: BaseScreen) {
        val implementation = checkNotNull(screen.game.fontImplementation)
        repeat(2) {
            val data = NativeBitmapFontData(implementation)
            val font = NativeBitmapFont(data)
            try {
                val layout = GlyphLayout(font, "Royal font lifecycle: Latin and Кириллица 0123456789")
                check(layout.width > 0f && layout.height > 0f)
                val textures = data.regions.map { it.texture }.distinct()
                check(textures.isNotEmpty() && textures.all { it.textureObjectHandle != 0 })
                // PixmapPacker сохраняет Pixmap для восстановления текстуры после потери контекста.
                val pixmaps = textures.map { it.textureData.consumePixmap() }.distinct()
                check(pixmaps.none { it.isDisposed })
                font.dispose()
                check(textures.all { it.textureObjectHandle == 0 }) {
                    "Освобождение NativeBitmapFont оставило активные текстуры"
                }
                check(pixmaps.all { it.isDisposed }) {
                    "Освобождение NativeBitmapFont оставило Pixmap страниц"
                }
                font.dispose()
                check(textures.all { it.textureObjectHandle == 0 })
            } finally {
                font.dispose()
            }
        }
        screen.stage.addActor(com.badlogic.gdx.scenes.scene2d.ui.Label(
            "Native font disposal checks passed", BaseScreen.skin
        ).apply { setPosition(24f, 24f) })
    }
}
