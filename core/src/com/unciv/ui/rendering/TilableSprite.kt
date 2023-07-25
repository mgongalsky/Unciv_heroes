package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class TilableSprite : Sprite {

    constructor(): super()
    constructor(textureRegion: TextureRegion) : super(textureRegion)

    var isAnchoredLeft: Boolean = false
    var isAnchoredBottom: Boolean = false

    // TODO: write function setBoundaries, which will make boundaries for automatic cropping
    var isTiledX: Boolean = false
    var isTiledY: Boolean = false

    fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {

        val brush: Sprite = Sprite(this)

        if(!isTiledX && !isTiledY) {
            // Those values have not been tested, but should work
/*
            brush.setRegion(
                this.u,
                (1 - min(
                    abs(this.height * scaleY * scaleY),
                    height - y)
                 / (this.height * scaleY * scaleY)) * (this.v2 - this.v) + this.v,
                min(
                    abs(this.width * scaleX * scaleX),
                    width - x)
                 / (this.width * scaleX * scaleX) * (this.u2 - this.u) + this.u,
                this.v2
            )


 */
            /*
            brush.setBounds(
                x,
                y,
                min(abs(this.width * scaleX) * scaleX, width),
                min(abs(this.height * scaleY) * scaleY, height)
            )

             */
            brush.setBounds(
                x,
                y,
                min(abs(this.width) * scaleX, width),
                min(abs(this.height) * scaleY, height)
            )


            //brush.setBounds(x,y,width,height)
            brush.draw(batch)
        }
        else
        {
            val startX: Float
            val finishX: Float
            val startY: Float
            val finishY: Float
            val realWidth: Float
            val realHeight: Float
            val stepX: Float
            var currX: Float
            val stepY: Float
            var currY: Float

            if(isAnchoredLeft) {
                startX = x
                finishX = width
                realWidth = abs(this.width * scaleX)
                stepX = abs(realWidth)
            }
            else{
                startX = width
                finishX = x
                realWidth = abs(this.width * scaleX)
                stepX = -abs(realWidth)
            }

            if(isAnchoredBottom) {
                startY = y
                finishY = height
                realHeight = abs(this.height * scaleY)
                stepY = abs(realHeight)
            }
            else{
                startY = height
                finishY = y
                realHeight = abs(this.height * scaleY)
                stepY = -abs(realHeight)
            }



            currX = startX

            while (currX * sign(stepX.toFloat()) < finishX * sign(stepX.toFloat())) {
                currY = startY
                while (currY * sign(stepY.toFloat()) < finishY * sign(stepY.toFloat())) {

                    brush.setRegion(
                        this.u,
                        (1 - min(
                            abs(stepY * scaleY),
                            abs(finishY - currY)
                        ) / (abs(stepY) * scaleY)) * (this.v2 - this.v) + this.v
                        ,
                        min(
                            abs(stepX * scaleX),
                            abs(finishX - currX)
                        ) / (abs(stepX) * scaleX) * (this.u2 - this.u) + this.u,
                        this.v2
                    )
                    brush.setBounds(
                        currX,
                        currY,
                        min(abs(stepX), abs(finishX - currX) / scaleX) * sign(stepX),
                        min(abs(stepY), abs(finishY - currY) / scaleY) * sign(stepY)
                        //realHeight
                    )
                    brush.draw(batch)
                    if(!isTiledY)
                        break
                    //batch.

                    currY += stepY * scaleX
                }
                if(!isTiledX)
                    break
                currX += stepX * scaleX

            }
        }




    }


}
