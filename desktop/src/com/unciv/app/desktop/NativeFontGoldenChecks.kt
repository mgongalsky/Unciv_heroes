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
                val layout =
                        GlyphLayout(font, "Royal font lifecycle: Latin and Кириллица 0123456789")
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
        verifySharedAtlas(screen)
        screen.stage.addActor(
            com.badlogic.gdx.scenes.scene2d.ui.Label(
                "Native font and shared Skin disposal checks passed", BaseScreen.skin
            ).apply { setPosition(24f, 24f) })
    }
    private fun verifySharedAtlas(screen: BaseScreen) {
        val atlas = com.badlogic.gdx.graphics.g2d.TextureAtlas()
        val pixels = com.badlogic.gdx.graphics.Pixmap(
            2,
            2,
            com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888
        )
        val sharedTexture = try {
            com.badlogic.gdx.graphics.Texture(pixels)
        } finally {
            pixels.dispose()
        }
        atlas.addRegion("shared", com.badlogic.gdx.graphics.g2d.TextureRegion(sharedTexture))
        val first = com.unciv.ui.utils.SharedAtlasSkin(atlas)
        val second = com.unciv.ui.utils.SharedAtlasSkin(atlas)
        try {
            val font =
                    NativeBitmapFont(NativeBitmapFontData(checkNotNull(screen.game.fontImplementation)))
            first.add("Nativefont", font, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
            first.add("button", font, com.badlogic.gdx.graphics.g2d.BitmapFont::class.java)
            GlyphLayout(font, "Shared Skin: Кириллица")
            val fontTextures = font.regions.map { it.texture }.distinct()
            val fontPixmaps = fontTextures.map { it.textureData.consumePixmap() }.distinct()
            com.unciv.ui.utils.RoyalPanelDrawable(first)
            com.unciv.ui.utils.RoyalRibbonDrawable(first) { null }
            com.unciv.ui.utils.RoyalPanelDrawable(second)
            val firstTextures =
                    first.getAll(com.badlogic.gdx.graphics.Texture::class.java).values().toList()
            val secondTextures =
                    second.getAll(com.badlogic.gdx.graphics.Texture::class.java).values().toList()
            check(firstTextures.isNotEmpty() && secondTextures.isNotEmpty())
            check((firstTextures + secondTextures + fontTextures).all { it.textureObjectHandle != 0 })
            val sharedHandle = sharedTexture.textureObjectHandle
            check(sharedHandle != 0)
            first.dispose()
            first.dispose()
            check(firstTextures.all { it.textureObjectHandle == 0 }) { "Skin оставил Royal-текстуры" }
            check(fontTextures.all { it.textureObjectHandle == 0 } && fontPixmaps.all { it.isDisposed })
            check(sharedTexture.textureObjectHandle == sharedHandle) { "Skin освободил общий атлас" }
            check(secondTextures.all { it.textureObjectHandle != 0 }) { "Повреждены ресурсы другого Skin" }
            check(second.getRegion("shared").texture === sharedTexture)
            second.dispose()
            second.dispose()
            check(secondTextures.all { it.textureObjectHandle == 0 })
            check(sharedTexture.textureObjectHandle == sharedHandle)
            atlas.dispose()
            check(sharedTexture.textureObjectHandle == 0)
            println("Shared Skin disposal checks passed: fonts, Royal textures and borrowed atlas")
        } finally {
            first.dispose()
            second.dispose()
            if (sharedTexture.textureObjectHandle != 0) atlas.dispose()
        }
    }
}
