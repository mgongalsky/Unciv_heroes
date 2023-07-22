package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureWrap
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.utils.JsonValue
import com.unciv.UncivGame
import com.unciv.ui.UncivStage
import kotlin.math.sign

enum class Tilability { PLANE, X, Y}
enum class Anchor {CENTER, LEFT_TOP, TOP, RIGHT_TOP, RIGHT, RIGHT_BOTTOM, BOTTOM, LEFT_BOTTOM, LEFT}

/** DEPRECATED! Use standart TiledDrawable class instead */
class Tiled2DDrawable : Drawable {
    //private lateinit var texture: Texture
    lateinit var textureRegion: TextureRegion
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



        val tiledDrawable = TiledDrawable(textureRegion)

// Now you can use the TiledDrawable to draw
// Assume batch is your SpriteBatch, and you want to fill an area at (50,50) with size (200,200)
        ////batch.begin()
        tiledDrawable.draw(batch, x,y,width,height)
        return

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
                    var currWidth = tileWidth
                    var currHeight = tileHeight
                    if((drawX + stepX) * sign(stepX.toFloat()) > finishX * sign(stepX.toFloat()))
                        currWidth = (drawX + stepX) * sign(stepX.toFloat()) - finishX * sign(stepX.toFloat())

               //     var textureRegion = textureRegion

                    //val tiledDrawable = TiledDrawable(textureRegion)

// Now you can use the TiledDrawable to draw
// Assume batch is your SpriteBatch, and you want to fill an area at (50,50) with size (200,200)
                    ////batch.begin()
                    //tiledDrawable.draw(batch, x,y,width/2,height/2)
                    //batch.end()

                    // background?.draw(batch,x,y,width,height)

                    // TODO: We need to fix overlapping of Tiled and non-tiled elements by cropping
                    /*
                    if(mirroredTiling)
                        if(i % 2 == 0)
                            drawWithCropping(batch,x + drawX, y + drawY, tileWidth, tileHeight)
                        else
                            drawWithCropping(batch,x + drawX + tileWidth, y + drawY, -tileWidth, tileHeight)


                     */
                    /*
                    if(mirroredTiling)
                        if(i % 2 == 0)
                            batch.draw(textureRegion, x + drawX, y + drawY, tileWidth, tileHeight)
                        else
                            batch.draw(textureRegion, x + drawX + tileWidth, y + drawY, -tileWidth, tileHeight)


                     */
                    /*
                   if(mirroredTiling)
                       if(i % 2 == 0)
                           batch.draw(textureRegion, x + drawX, y + drawY, currWidth, tileHeight)
                       else
                           batch.draw(textureRegion, x + drawX + currWidth, y + drawY, -currWidth, tileHeight)


                     */
/*
                    if (mirroredTiling)
                        if (i % 2 == 0)
                            tiledDrawable.draw(batch, x + drawX, y + drawY, currWidth, tileHeight)
                        else
                            tiledDrawable.draw(
                                batch,
                                x + drawX + currWidth,
                                y + drawY,
                                -currWidth,
                                tileHeight
                            )



 */


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


    fun drawWithCropping(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        // Define your area where rendering will be allowed.
        val scissor = Rectangle()
        val clipBounds = Rectangle(x, y, width, height)

        scissor.set(x, y, width, height)
        val camera = UncivGame.Current.screen?.stage?.camera
        ScissorStack.calculateScissors(camera, batch.transformMatrix, clipBounds, scissor)

        batch.flush() // Ensure batch is flushed before changing scissors.

        // Now push the scissors to the ScissorStack which modifies the OpenGL scissor test.
        if (ScissorStack.pushScissors(scissor)) {
            //draw(batch,x, y, width, height)
            batch.draw(textureRegion, x, y, width, height)
            batch.flush() // Remember to flush the batch before popping scissors.
            ScissorStack.popScissors()
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
