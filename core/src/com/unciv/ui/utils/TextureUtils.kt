package com.unciv.ui.utils


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable

object TextureUtils {
    fun createMonochromaticTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()

        return texture
    }


    fun createBorderDrawable(texture: Texture, borderWidth: Float, borderColor: Color): Drawable {
        val sprite = Sprite(texture)
        val borderSprite = Sprite(texture)

        borderSprite.setSize(sprite.width + borderWidth * 2f, sprite.height + borderWidth * 2f)
        borderSprite.setColor(borderColor)

        val borderBatch = SpriteBatch()
        borderBatch.begin()
        borderSprite.draw(borderBatch)
        borderBatch.end()

        val drawable = SpriteDrawable(sprite)
        drawable.leftWidth = borderWidth
        drawable.rightWidth = borderWidth
        drawable.topHeight = borderWidth
        drawable.bottomHeight = borderWidth

        return drawable
    }
}

