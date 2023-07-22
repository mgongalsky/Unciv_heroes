package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.JsonValue
import kotlin.math.sign

enum class Tilability { PLANE, X, Y}
enum class Anchor {CENTER, LEFT_TOP, TOP, RIGHT_TOP, RIGHT, RIGHT_BOTTOM, BOTTOM, LEFT_BOTTOM, LEFT}

class Tiled2DDrawable : Drawable {
    //private lateinit var texture: Texture
    private lateinit var textureRegion: TextureRegion
    var anchor = Anchor.LEFT_TOP
    private var anchorString = ""
    private var tilability = Tilability.PLANE
    private var tilabilityString = ""
    private var mirroredTiling = true //false

    constructor() : super() {
        // no-arg constructor for JSON instantiation
    }

    constructor(skin: Skin, jsonData: JsonValue) : this() {
        textureRegion = skin.getRegion(jsonData.getString("textureRegion"))
        //tilability = Tilability.valueOf(jsonData.getString("tilability"))
        tilabilityString = jsonData.getString("tilability")
        anchorString = jsonData.getString("anchorString")
        mirroredTiling = jsonData.getString("mirroredTiling").toBoolean()


        setFromStrings()


    }

    constructor(region: TextureRegion) {
        textureRegion = TextureRegion(region)
    }


    init {
        setFromStrings()
    }

    private fun setFromStrings() {

        if (tilabilityString != "")
            tilability = when (tilabilityString) {
                "PLANE" -> Tilability.PLANE
                "X" -> Tilability.X
                "Y" -> Tilability.Y
                else -> throw IllegalArgumentException("Invalid tilability value in Skin.json")
            }

        if (anchorString != "")
            anchor = when (anchorString) {
                "CENTER" -> Anchor.CENTER
                "LeftTop" -> Anchor.LEFT_TOP
                "RightTop" -> Anchor.RIGHT_TOP
                "Top" -> Anchor.TOP
                "LeftBottom" -> Anchor.LEFT_BOTTOM
                "Bottom" -> Anchor.BOTTOM
                "RightBottom" -> Anchor.RIGHT_BOTTOM
                "Left" -> Anchor.LEFT
                "Right" -> Anchor.RIGHT
                else -> throw IllegalArgumentException("Invalid anchorString value in Skin.json")
            }

    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        setFromStrings()
        var tileWidth = textureRegion.regionWidth.toFloat()
        var tileHeight = textureRegion.regionHeight.toFloat()

        var startX: Int = 0
        var startY: Int = 0
        var finishX: Int = 0
        var finishY: Int = 0
        var stepX: Int = 0
        var stepY: Int = 0
        var drawX: Int = 0
        var drawY: Int = 0

        when(anchor){
            Anchor.LEFT_TOP, Anchor.LEFT_BOTTOM, Anchor.LEFT, Anchor.CENTER, Anchor.TOP, Anchor.BOTTOM -> {
                stepX = tileWidth.toInt()
                startX = 0
                finishX = width.toInt()
            }
            Anchor.RIGHT, Anchor.RIGHT_TOP, Anchor.RIGHT_BOTTOM -> {
                stepX = -tileWidth.toInt()
                startX = width.toInt()
                finishX = 0
                tileWidth *= -1
            }
        }
        drawX = startX


        when(anchor){
            Anchor.LEFT_TOP, Anchor.TOP, Anchor.RIGHT_TOP, Anchor.CENTER, Anchor.LEFT, Anchor.RIGHT -> {
                stepY = -tileHeight.toInt()
                startY = height.toInt()
                finishY = 0
                tileHeight *= -1

            }
            Anchor.LEFT_BOTTOM, Anchor.BOTTOM, Anchor.RIGHT_BOTTOM -> {
                stepY = tileHeight.toInt()
                startY = 0
                finishY = height.toInt()

            }
        }
        drawY = startY

        var i = 0
        var j = 0

        when (tilability) {
            Tilability.PLANE ->
                while (drawX * sign(stepX.toFloat()) < finishX * sign(stepX.toFloat())) {
                    drawY = startY
                    j = 0
                    while (drawY * sign(stepY.toFloat()) < finishY * sign(stepY.toFloat())) {
                        if(mirroredTiling) {
                            if(i % 2 == 1 && j % 2 == 0)
                                batch.draw(textureRegion, x + drawX + tileWidth, y + drawY, -tileWidth, tileHeight)
                            if(i % 2 == 0 && j % 2 == 1)
                                batch.draw(textureRegion, x + drawX, y + drawY + tileHeight, tileWidth, -tileHeight)
                            if(i % 2 == 1 && j % 2 == 1)
                                batch.draw(textureRegion, x + drawX + tileWidth, y + drawY + tileHeight, -tileWidth, -tileHeight)
                            if(i % 2 == 0 && j % 2 == 0)
                                batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)


                        }
                        else
                            batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)

                        j++
                        drawY += stepY
                    }
                    drawX += stepX
                    i++
                }
            Tilability.X ->
                while (drawX * sign(stepX.toFloat()) < finishX * sign(stepX.toFloat())) {
                    if(mirroredTiling)
                        if(i % 2 == 0)
                            batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
                        else
                            batch.draw(textureRegion, x + drawX + tileWidth, y + drawY, -tileWidth, tileHeight)

                    drawX += stepX
                    i++
                }
            Tilability.Y ->
                while (drawY * sign(stepY.toFloat()) < finishY * sign(stepY.toFloat())) {
                    if(mirroredTiling)
                        if(j % 2 == 0)
                            batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
                        else
                            batch.draw(textureRegion, x + drawX, y + drawY + tileHeight, tileWidth, -tileHeight)

                    drawY += stepY
                    j++
                }
        }
    }

    override fun getLeftWidth() = 0f
    override fun getRightWidth() = 0f
    override fun getTopHeight() = 0f
    override fun getBottomHeight() = 0f
    override fun getMinWidth() = textureRegion.regionWidth.toFloat()
    override fun getMinHeight() = textureRegion.regionHeight.toFloat()

    override fun setLeftWidth(leftWidth: Float) {}
    override fun setRightWidth(rightWidth: Float) {}
    override fun setTopHeight(topHeight: Float) {}
    override fun setBottomHeight(bottomHeight: Float) {}
    override fun setMinWidth(minWidth: Float) {}
    override fun setMinHeight(minHeight: Float) {}

}
