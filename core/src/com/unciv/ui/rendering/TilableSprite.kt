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
    var isTiled: Boolean = true
    fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {

        if(!isTiled) {
            setBounds(x, y, width, height)
            super.draw(batch)
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
                realHeight = abs(this.height * scaleX)
                stepY = -abs(realHeight)
            }


            val brush: Sprite = Sprite(this)

            currX = startX

            while (currX * sign(stepX.toFloat()) < finishX * sign(stepX.toFloat())) {
                currY = startY
                while (currY * sign(stepY.toFloat()) < finishY * sign(stepY.toFloat())) {

                    /*
                    brush.setRegion(
                        this.u,
                        this.v,
                        min(
                            abs(stepX * scaleX),
                            abs(finishX - currX)
                        ) / (abs(stepX) * scaleX) * (this.u2 - this.u) + this.u,
                        min(
                            abs(stepY * scaleY),
                            abs(finishY - currY)
                        ) / (abs(stepY) * scaleY) * (this.v2 - this.v) + this.v
                    )


                     */

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

                    //batch.

                    currY += stepY * scaleX
                }
                currX += stepX * scaleX

            }
        }




    }


}
