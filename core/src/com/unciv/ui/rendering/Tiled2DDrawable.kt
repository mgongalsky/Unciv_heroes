package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.utils.JsonValue

class Tiled2DDrawable : Drawable {
    //private lateinit var texture: Texture
    private lateinit var textureRegion: TextureRegion

    constructor() : super() {
        // no-arg constructor for JSON instantiation
    }

    constructor(skin: Skin, jsonData: JsonValue) : this() {
        textureRegion = skin.getRegion(jsonData.getString("textureRegion"))
        textureRegion.texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
    }

    constructor(region: TextureRegion) {
        textureRegion = TextureRegion(region)
        textureRegion.texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)

    }


    init {
    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        textureRegion.texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)

        val tileWidth = textureRegion.regionWidth.toFloat()
        val tileHeight = textureRegion.regionHeight.toFloat()

        for (drawX in 0 until width.toInt() step tileWidth.toInt()) {
            for (drawY in 0 until height.toInt() step tileHeight.toInt()) {
                batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
            }
        }
    }

    override fun getLeftWidth() = 0f
    override fun getRightWidth() = 0f
    override fun getTopHeight() = 0f
    override fun getBottomHeight() = 0f
    override fun getMinWidth() = 0f
    override fun getMinHeight() = 0f

    override fun setLeftWidth(leftWidth: Float) {}
    override fun setRightWidth(rightWidth: Float) {}
    override fun setTopHeight(topHeight: Float) {}
    override fun setBottomHeight(bottomHeight: Float) {}
    override fun setMinWidth(minWidth: Float) {}
    override fun setMinHeight(minHeight: Float) {}
}
