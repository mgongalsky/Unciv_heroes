package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import com.badlogic.gdx.utils.JsonValue
import kotlin.math.min

class TilableFrame : Drawable{


    //  leftTopDrawable - leftTopHorizontalDrawable ------ topDrawable ------ rightTopHorizontalDrawable - rightTopDrawable
    //  leftTopVerticalDrawable ------ background ------ rightTopVerticalDrawable
    //  ----
    //  ----
    //  leftDrawable ------ background ------ rightDrawable
    //  ----
    //  ----
    //  leftBottomVerticalDrawable ------ background ------ rightBottomVerticalDrawable
    //  leftBottomDrawable - leftBottomHorizontalDrawable ------ bottomDrawable ------ rightBottomHorizontalDrawable - rightBottomDrawable


    var tiledBackground: TiledDrawable? = null

    var leftTopDrawable: Drawable? = null
    var leftTopHorizontalDrawable: Drawable? = null
    var leftTopVerticalDrawable: Drawable? = null
    var rightTopHorizontalDrawable: Drawable? = null
    var rightTopVerticalDrawable: Drawable? = null

    var rightTopDrawable: Drawable? = null

    var tiledTopDrawable: TiledDrawable? = null

    var leftTiledDrawable: TiledDrawable? = null
    var rightTiledDrawable: TiledDrawable? = null
    // TODO: add here untilable parts or corners (leftTopBorder, etc.)

    constructor() : super() {
        // no-arg constructor for JSON instantiation
    }

    constructor(skin: Skin, jsonData: JsonValue) : this() {
        //background = Tiled2DDrawable(skin)
        //textureRegion = skin.getRegion(jsonData.getString("textureRegion"))

        tiledBackground = skin.get(jsonData.getString("background"), TiledDrawable::class.java)
       // tiledBackground = skin.get(jsonData.getString("background"), TiledDrawable::class.java)
        leftTopDrawable = skin.get(jsonData.getString("leftTopDrawable"), Drawable::class.java)
        leftTopHorizontalDrawable = skin.get(jsonData.getString("leftTopHorizontalDrawable"), Drawable::class.java)
        leftTopVerticalDrawable = skin.get(jsonData.getString("leftTopVerticalDrawable"), Drawable::class.java)
        tiledTopDrawable = skin.get(jsonData.getString("tiledTopDrawable"), TiledDrawable::class.java)
      //  tiledTopDrawable = skin.get(jsonData.getString("topDrawable"), TiledDrawable::class.java)
        leftTiledDrawable = skin.get(jsonData.getString("tiledLeftDrawable"), TiledDrawable::class.java)
        //initTiledDrawables()

    }

    constructor(region: TextureRegion) {
    }

    fun initTiledDrawables(){
        //if(background != null && tiledBackground == null)
       //     tiledBackground = background as TiledDrawable
       // if(topDrawable != null && tiledTopDrawable == null)
       //     tiledTopDrawable = topDrawable as TiledDrawable


    }


    init {
    }



    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        //var textureRegion = background?.textureRegion
       // initTiledDrawables()
       // val tiledDrawable = TiledDrawable(textureRegion)
      //  tiledBackground = background as TiledDrawable
        //Drawable

// Now you can use the TiledDrawable to draw
// Assume batch is your SpriteBatch, and you want to fill an area at (50,50) with size (200,200)
        ////batch.begin()
        //tiledBackground?.draw(batch, x,y,width,height)
        tiledBackground?.draw(
            batch,
            x + leftTopDrawable?.minWidth!! / 2,
            y + x + leftTopDrawable?.minHeight!! / 2,
            width - leftTopDrawable?.minWidth!!,
            height - leftTopDrawable?.minHeight!!
        )
        //batch.end()

       // background?.draw(batch,x,y,width,height)

        if(leftTiledDrawable != null) {
            var spacerUp = 0f
            if(leftTopDrawable != null)
                spacerUp += leftTopDrawable?.minHeight!!
            if(leftTopVerticalDrawable != null)
                spacerUp += leftTopVerticalDrawable?.minHeight!!

            var spacerDown = spacerUp

            /*
            tiledTopDrawable?.draw(
                batch,
                x + spacerLeft,
                y + height - tiledTopDrawable?.region?.regionHeight!!.toFloat(),
                (width - spacerLeft - spacerRight),
                tiledTopDrawable?.region?.regionHeight!!.toFloat()
            )


             */

            leftTiledDrawable?.draw(
                batch,
                x,
                y + spacerDown,
                leftTiledDrawable?.region?.regionWidth?.toFloat()!!,
                height - spacerDown - spacerUp
            )
            if (rightTiledDrawable == null){
                //leftTiledDrawable?.scale = -1f
                //leftTiledDrawable.draw()
                //TransformDrawable
                leftTiledDrawable?.draw(
                    batch,
                    x + width - leftTiledDrawable?.region?.regionWidth?.toFloat()!!,
                    y + spacerDown,
                    leftTiledDrawable?.region?.regionWidth?.toFloat()!!,
                    height - spacerDown - spacerUp,
                )
            }
        }

        if(leftTopDrawable != null) {
           // leftTopDrawable.leftWidth
            leftTopDrawable?.draw(batch, x, y + height - leftTopDrawable?.minHeight!!, min( leftTopDrawable?.minWidth!!, width/2), leftTopDrawable?.minHeight!!)
            if(rightTopDrawable != null){
                rightTopDrawable?.draw(batch, x + width - rightTopDrawable?.minWidth!!, y + height - rightTopDrawable?.minHeight!!, min(rightTopDrawable?.minWidth!!, width/2), rightTopDrawable?.minHeight!!)

            }
            else {
                leftTopDrawable?.draw(batch, x + width, y + height - leftTopDrawable?.minHeight!!, -min(leftTopDrawable?.minWidth!!, width/2), leftTopDrawable?.minHeight!!)
            }


            if(leftTopHorizontalDrawable != null){
                leftTopHorizontalDrawable?.draw(
                    batch,
                    x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopHorizontalDrawable?.minHeight!!,
                    min(leftTopHorizontalDrawable?.minWidth!!, width/2 - leftTopDrawable?.minWidth!!),
                    leftTopHorizontalDrawable?.minHeight!!
                )
                //leftTopHorizontalDrawable?.anchor = Anchor.RIGHT

                if(rightTopHorizontalDrawable == null){


                    // !! Note: negative width or height values works for mirroring only for DRAWABLE.
                    //      For Tiled2DDrawables use positive values, but opposite anchoring!!
                    leftTopHorizontalDrawable?.draw(
                        batch,
                        x + width - leftTopDrawable?.minWidth!!,
                        y + height - leftTopHorizontalDrawable?.minHeight!!,
                        -min(leftTopHorizontalDrawable?.minWidth!!, width/2 - leftTopDrawable?.minWidth!!),
                        leftTopHorizontalDrawable?.minHeight!!
                    )


                }
            }

            if(leftTopVerticalDrawable != null){
                /*
                leftTopVerticalDrawable?.draw(
                    batch,
                    x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopVerticalDrawable?.minHeight!!,
                    min(leftTopVerticalDrawable?.minWidth!!, width/2 - leftTopDrawable?.minWidth!!),
                    leftTopVerticalDrawable?.minHeight!!
                )

                 */
                leftTopVerticalDrawable?.draw(
                    batch,
                    x,
                    y + height - leftTopDrawable?.minHeight!! -min(leftTopVerticalDrawable?.minHeight!!, height/2 - leftTopDrawable?.minHeight!!),
                    leftTopVerticalDrawable?.minWidth!!,
                    min(leftTopVerticalDrawable?.minHeight!!, height/2 - leftTopDrawable?.minHeight!!)
                )
                //leftTopHorizontalDrawable?.anchor = Anchor.RIGHT

                if(rightTopVerticalDrawable == null){


                    // !! Note: negative width or height values works for mirroring only for DRAWABLE.
                    //      For Tiled2DDrawables use positive values, but opposite anchoring!!
                    /*
                    leftTopVerticalDrawable?.draw(
                        batch,
                        x + width - leftTopDrawable?.minWidth!!,
                        y + height - leftTopVerticalDrawable?.minHeight!!,
                        -min(leftTopVerticalDrawable?.minWidth!!, width/2 - leftTopDrawable?.minWidth!!),
                        leftTopVerticalDrawable?.minHeight!!
                    )

                     */
                    leftTopVerticalDrawable?.draw(
                        batch,
                        x + width,// - leftTopVerticalDrawable?.minWidth!!,
                        y + height - leftTopDrawable?.minHeight!! - min(leftTopVerticalDrawable?.minHeight!!, height/2 - leftTopDrawable?.minHeight!!),
                        -leftTopVerticalDrawable?.minWidth!!,
                        min(leftTopVerticalDrawable?.minHeight!!, height/2 - leftTopDrawable?.minHeight!!)
                    )


                }
            }


        }

        if(tiledTopDrawable != null) {
            var spacerLeft = 0f
            if(leftTopDrawable != null)
                spacerLeft += leftTopDrawable?.minWidth!!
            if(leftTopHorizontalDrawable != null)
                spacerLeft += leftTopHorizontalDrawable?.minWidth!!

            var spacerRight = spacerLeft

            tiledTopDrawable?.draw(
                batch,
                x + spacerLeft,
                y + height - tiledTopDrawable?.region?.regionHeight!!.toFloat(),
                (width - spacerLeft - spacerRight),
                tiledTopDrawable?.region?.regionHeight!!.toFloat()
            )
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
