package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import kotlin.math.abs
import kotlin.math.min

class TilableSprite : Sprite {

    constructor(): super()
    constructor(textureRegion: TextureRegion) : super(textureRegion)

    // TODO: write function setBoundaries, which will make boundaries for automatic cropping
    var isTiled: Boolean = true
    fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {

        if(!isTiled) {
            setBounds(x, y, width, height)
            super.draw(batch)
        }
        else
        {
            val startX = x
            val finishX = width
            val realWidth = abs(this.width*scaleX)
            val realHeight = abs(this.height*scaleY)
            val stepX = abs(realWidth)
            var currX = startX
            //var brush: Sprite = this
            val brush: Sprite = Sprite(this)

            while(currX < finishX){
                //this.boundingRectangle = Rectangle()
               // brush.
                brush.setRegion(this.u,
                this.v,
                    (min(stepX * scaleX, finishX - currX) / (stepX * scaleX) * (this.u2 - this.u) + this.u),
                    (this.v2))


                brush.setBounds(currX,
                y,
                min(stepX, (finishX - currX) / scaleX),
                realHeight)
                brush.draw(batch)

                //batch.

                currX += stepX * scaleX
            }

        }




    }


}
