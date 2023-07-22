package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.JsonValue

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


    var background: Tiled2DDrawable? = null
    var leftTopDrawable: Drawable? = null
    var leftTopHorizontalDrawable: Drawable? = null
    var rightTopHorizontalDrawable: Drawable? = null
    var rightTopDrawable: Drawable? = null
    var topDrawable: Tiled2DDrawable? = null
    var leftDrawable: Tiled2DDrawable? = null
    var rightDrawable: Tiled2DDrawable? = null
    // TODO: add here untilable parts or corners (leftTopBorder, etc.)

    constructor() : super() {
        // no-arg constructor for JSON instantiation
    }

    constructor(skin: Skin, jsonData: JsonValue) : this() {
        //background = Tiled2DDrawable(skin)
        //textureRegion = skin.getRegion(jsonData.getString("textureRegion"))

        background = skin.get(jsonData.getString("background"), Tiled2DDrawable::class.java)
        leftTopDrawable = skin.get(jsonData.getString("leftTopDrawable"), Drawable::class.java)
        leftTopHorizontalDrawable = skin.get(jsonData.getString("leftTopHorizontalDrawable"), Drawable::class.java)
        topDrawable = skin.get(jsonData.getString("topDrawable"), Tiled2DDrawable::class.java)
        leftDrawable = skin.get(jsonData.getString("leftDrawable"), Tiled2DDrawable::class.java)


    }

    constructor(region: TextureRegion) {
    }


    init {
    }



    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        background?.draw(batch,x,y,width,height)

        if(leftDrawable != null) {
            leftDrawable?.draw(
                batch,
                x,
                y,
                leftDrawable?.minWidth!!,
                height
            )
            if (rightDrawable == null){
                leftDrawable?.anchor = Anchor.RIGHT
                leftDrawable?.draw(
                    batch,
                    x+width-leftDrawable?.minWidth!!,
                    y,
                    leftDrawable?.minWidth!!,
                    height
                )
                leftDrawable?.anchor = Anchor.LEFT
            }
        }

        if(leftTopDrawable != null) {
           // leftTopDrawable.leftWidth
            leftTopDrawable?.draw(batch, x, y + height - leftTopDrawable?.minHeight!!, leftTopDrawable?.minWidth!!, leftTopDrawable?.minHeight!!)
            if(rightTopDrawable != null){
                rightTopDrawable?.draw(batch, x + width - rightTopDrawable?.minWidth!!, y + height - rightTopDrawable?.minHeight!!, rightTopDrawable?.minWidth!!, rightTopDrawable?.minHeight!!)

            }
            else {
                leftTopDrawable?.draw(batch, x + width, y + height - leftTopDrawable?.minHeight!!, -leftTopDrawable?.minWidth!!, leftTopDrawable?.minHeight!!)
            }


            if(leftTopHorizontalDrawable != null){
                leftTopHorizontalDrawable?.draw(
                    batch,
                    x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopHorizontalDrawable?.minHeight!!,
                    leftTopHorizontalDrawable?.minWidth!!,
                    leftTopHorizontalDrawable?.minHeight!!
                )
                //leftTopHorizontalDrawable?.anchor = Anchor.RIGHT

                if(rightTopHorizontalDrawable == null){


                    leftTopHorizontalDrawable?.draw(
                        batch,
                        x + width - leftTopDrawable?.minWidth!!,
                        y + height - leftTopHorizontalDrawable?.minHeight!!,
                        -leftTopHorizontalDrawable?.minWidth!!,
                        leftTopHorizontalDrawable?.minHeight!!
                    )


                }
            }

        }

        if(topDrawable != null) {
            var spacerLeft = 0f
            if(leftTopDrawable != null)
                spacerLeft += leftTopDrawable?.minWidth!!
            if(leftTopHorizontalDrawable != null)
                spacerLeft += leftTopHorizontalDrawable?.minWidth!!

            var spacerRight = spacerLeft

            topDrawable?.draw(
                batch,
                x + spacerLeft,
                y + height - topDrawable?.minHeight!!,
                width - spacerLeft - spacerRight,
                topDrawable?.minHeight!!
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
