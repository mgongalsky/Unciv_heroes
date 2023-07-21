package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.JsonValue

enum class Tilability { PLANE, X, Y}

class Tiled2DDrawable : Drawable {
    //private lateinit var texture: Texture
    private lateinit var textureRegion: TextureRegion
    private var tilability = Tilability.PLANE
    private var tilabilityString = ""

    constructor() : super() {
        // no-arg constructor for JSON instantiation
    }

    constructor(skin: Skin, jsonData: JsonValue) : this() {
        textureRegion = skin.getRegion(jsonData.getString("textureRegion"))

        //tilability = Tilability.valueOf(jsonData.getString("tilability"))
        tilabilityString = jsonData.getString("tilability")


        setTilability()


    }

    constructor(region: TextureRegion) {
        textureRegion = TextureRegion(region)
    }


    init {
        setTilability()
    }

    private fun setTilability() {

        if (tilabilityString != "")
            tilability = when (tilabilityString) {
                "PLANE" -> Tilability.PLANE
                "X" -> Tilability.X
                "Y" -> Tilability.Y
                else -> throw IllegalArgumentException("Invalid tilability value in Skin.json")
            }

    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {

        setTilability()
        val tileWidth = textureRegion.regionWidth.toFloat()
        val tileHeight = textureRegion.regionHeight.toFloat()

        when (tilability) {
            Tilability.PLANE ->
                for (drawX in 0 until width.toInt() step tileWidth.toInt())
                    for (drawY in 0 until height.toInt() step tileHeight.toInt()) {
                        batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
                    }
            Tilability.X ->
                for (drawX in 0 until width.toInt() step tileWidth.toInt())
                    batch.draw(textureRegion, x + drawX, y, tileWidth, tileHeight)
            Tilability.Y ->
                for (drawY in 0 until height.toInt() step tileHeight.toInt()) {
                    batch.draw(textureRegion, x, y + drawY, tileWidth, tileHeight)
                }
        }
/*

                } == Tilability.PLANE || tilability == Tilability.X)
            for (drawX in 0 until width.toInt() step tileWidth.toInt()) {
                if (tilability == Tilability.PLANE || tilability == Tilability.Y)
                    for (drawY in 0 until height.toInt() step tileHeight.toInt()) {
                        batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
                    }
            }


 */
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
