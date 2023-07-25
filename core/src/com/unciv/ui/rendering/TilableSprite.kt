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
    var isAnchoredBottom: Boolean = true

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
            val realWidth: Float
            val realHealth: Float
            val stepX: Float
            var currX: Float
            val realHeight = abs(this.height * scaleY)

            if(isAnchoredLeft) {
                startX = x
                finishX = width
                realWidth = abs(this.width * scaleX)
                stepX = abs(realWidth)
                currX = startX
            }
            else{
                startX = width
                finishX = x
                realWidth = abs(this.width * scaleX)
                stepX = -abs(realWidth)
                currX = startX
            }

            val brush: Sprite = Sprite(this)

            while(currX * sign(stepX.toFloat()) < finishX * sign(stepX.toFloat())){
                brush.setRegion(
                    this.u,
                    this.v,
                    min(
                        abs(stepX * scaleX),
                        abs(finishX - currX)
                    ) / (abs(stepX) * scaleX) * (this.u2 - this.u) + this.u,
                    this.v2
                )
                brush.setBounds(
                    currX,
                    y,
                    min(abs(stepX), abs(finishX - currX) / scaleX) * sign(stepX),
                    realHeight
                )
                brush.draw(batch)

                //batch.

                currX += stepX * scaleX
            }

        }




    }


}
