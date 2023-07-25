package com.unciv.ui.rendering

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Affine2
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable
import com.badlogic.gdx.utils.JsonValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("LiftReturnOrAssignment")
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

    var leftTopTextureRegion: TextureRegion? = null
    var leftTopSprite: Sprite? = null
    var leftTopTilableSprite: TilableSprite? = null

    var rightTopTextureRegion: TextureRegion? = null
    var rightTopSprite: Sprite? = null

    var leftTopHorizontalTextureRegion: TextureRegion? = null
    var leftTopHorizontalSprite: Sprite? = null

    var rightTopHorizontalTextureRegion: TextureRegion? = null
    var rightTopHorizontalSprite: Sprite? = null

    var topTextureRegion: TextureRegion? = null
    var topTiledDrawable: TiledDrawable? = null



    var leftTopHorizontalDrawable: Drawable? = null
    var leftTopVerticalDrawable: Drawable? = null
    var rightTopHorizontalDrawable: Drawable? = null
    var rightTopVerticalDrawable: Drawable? = null

    var rightTopDrawable: Drawable? = null

    var tiledTopDrawable: TiledDrawable? = null

    var leftTiledDrawable: TiledDrawable? = null
    var rightTiledDrawable: TiledDrawable? = null

    var testTexture: TextureRegion? = null


    var scaleFrame: Float = 1f
    var scaleBackground: Float = 1f

    @Transient
    var flagScaled = false
    @Transient
    var flagDrawablesCreated = false
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
        //tiledTopDrawable?.
        testTexture = skin.get(jsonData.getString("testTexture"), TextureRegion::class.java)


        leftTopHorizontalTextureRegion = skin.get(jsonData.getString("leftTopHorizontalTextureRegion"), TextureRegion::class.java)
        leftTopTextureRegion = skin.get(jsonData.getString("leftTopTextureRegion"), TextureRegion::class.java)
        rightTopHorizontalTextureRegion = skin.get(jsonData.getString("rightTopHorizontalTextureRegion"), TextureRegion::class.java)
        rightTopTextureRegion = skin.get(jsonData.getString("rightTopTextureRegion"), TextureRegion::class.java)

     //   topTextureRegion = skin.get(jsonData.getString("topHorizontalTextureRegion"), TextureRegion::class.java)
        topTextureRegion = skin.get(jsonData.getString("topTextureRegion"), TextureRegion::class.java)
        //leftTopTilableSprite = TilableSprite(skin.get(jsonData.getString("topTextureRegion"), TextureRegion::class.java))


        scaleBackground = skin.get(jsonData.getString("scaleBackground"), Float::class.java)
        scaleFrame = skin.get(jsonData.getString("scaleFrame"), Float::class.java)

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
        setSprites()
      //  createAllDrawables()
        //tiledBackground?.
    }

    fun createDrawable(region: TextureRegion, tiled: Boolean = false, scale: Float = 1f, isFlipX: Boolean = false, isFlipY: Boolean = false): Drawable {

      //  region.texture.textureData.
        //region.
        region.regionWidth = (region.regionWidth * scale).toInt()
        region.regionHeight = (region.regionHeight * scale).toInt()
        region.flip(isFlipX, isFlipY)

        //leftTopHorizontalSprite = Sprite(region)

        //return
        //region.texture
        if(tiled)
            return TiledDrawable(region)
        else
            return TextureRegionDrawable(region)

    }


    fun setSprites() {

        // Left corner
        if(leftTopTilableSprite == null && leftTopTextureRegion != null) {
            leftTopTilableSprite = TilableSprite(leftTopTextureRegion!!)
            leftTopTilableSprite?.apply {
                setOrigin(0f, height)
                setScale(scaleFrame)
            }

        }



        // Left horizontal border
        if(leftTopHorizontalSprite == null && leftTopHorizontalTextureRegion != null) {
            leftTopHorizontalSprite = Sprite(leftTopHorizontalTextureRegion)
            leftTopHorizontalSprite?.apply{
                setOrigin(0f, height)
                setScale(scaleFrame)

            }
        }

        // Right corner
        if(rightTopSprite == null && (rightTopTextureRegion != null || leftTopTextureRegion != null)) {
            rightTopSprite = Sprite(rightTopTextureRegion ?: leftTopTextureRegion)
            rightTopSprite?.apply {
                setOrigin(width, height)
                flip(true, false)
                setScale(scaleFrame)
            }
        }

        // Right horizontal border
        if(rightTopHorizontalSprite == null && (rightTopHorizontalTextureRegion != null || leftTopHorizontalTextureRegion != null)) {
            rightTopHorizontalSprite = Sprite(rightTopHorizontalTextureRegion ?: leftTopHorizontalTextureRegion)
            rightTopHorizontalSprite?.apply{
                //if(rightTopHorizontalTextureRegion == null)
               //     flip(true, false)
                // setOrigin(width, height)
                setOrigin(0f, height)
                setScale(scaleFrame)

            }
        }

        // Top border
        /*
        if(topTiledDrawable == null && topTextureRegion != null) {
            topTiledDrawable = TiledDrawable(topTextureRegion)
            topTiledDrawable.
            leftTopSprite?.apply {
                setOrigin(0f, height)
                setScale(scaleFrame)
            }
        }


         */




    }
    fun createAllDrawables(){
        //if(flagDrawablesCreated)
        //    return

        if(leftTopHorizontalTextureRegion != null && leftTopHorizontalSprite == null){
            leftTopHorizontalSprite = Sprite(leftTopHorizontalTextureRegion)


        }
            //leftTopHorizontalDrawableNew = createDrawable(leftTopHorizontalTextureRegion!!, tiled = false, 0.5f, isFlipX = true)


        // = true

    }

    fun scaling(){
        if(flagScaled)
            return

    //    tiledBackground?.scale = scaleBackground
     //   tiledBackground?.region?.u2 = 1f/scaleBackground
     //   tiledBackground?.region?.v2 = 1f/scaleBackground

        val region = TextureRegion(tiledBackground?.region)
    //    var texture = leftTopDrawable as Texture
        var region2 = TextureRegionDrawable(testTexture)
        //testTexture as TextureRegionDrawable
        //testTexture.textureData.
        //val region2 = TextureRegion(leftTopDrawable as TextureRegion)

// Scale the region
        region.regionWidth = (region.regionWidth * scaleBackground).toInt()
        region.regionHeight = (region.regionHeight * scaleBackground).toInt()
        //region.flip(true, false)

       // region2.r

        tiledBackground = TiledDrawable(region)

        region2.region.regionWidth= (region2.region.regionWidth / scaleBackground).toInt()
        region2.region.regionHeight = (region2.region.regionHeight / scaleBackground).toInt()
        //region.flip(true, false)

     //   leftTopDrawable = TiledDrawable(region2)


        flagScaled = true

    }

    override fun draw(batch: Batch, x: Float, y: Float, width: Float, height: Float) {
        //var textureRegion = background?.textureRegion
       // initTiledDrawables()
       // val tiledDrawable = TiledDrawable(textureRegion)
      //  tiledBackground = background as TiledDrawable
        //Drawable

        //scaling()
        //createAllDrawables()
        setSprites()
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

        leftTopHorizontalSprite?.apply {
            val spaceX = leftTopTilableSprite?.width!! * scaleFrame
            setBounds(
                x + spaceX,
                y + height - this.height,
                min(this.width, width / 2 - spaceX),
                this.height
            )
            draw(batch)

        }



        rightTopSprite?.apply {
            //val spaceX = rig
            setBounds(
                x + width - this.width ,
                y + height - this.height,
                this.width,
                this.height
            )
            draw(batch)
            //dra
        }

        rightTopHorizontalSprite?.apply {
            val spaceX = leftTopTilableSprite?.width!! * scaleFrame
            val dbMin = min(width/2 - spaceX, this.width)
            val oldWidth = this.width
            // Here we use abs of this.width, because it changes each time to opposite value. Abs fixes the problem, though it is indirected way (
            setBounds(
                x + width - spaceX ,
                y + height - this.height,
                -min(width/2 - spaceX, abs(this.width)),
                this.height
            )
            //flip(true, false)
           // setOriginBasedPosition(x + width - spaceX,
            //    y + height)
            draw(batch)
            //this.width = oldWidth

        }

       // x + spaceX,
      //  y + height - this.height,
      //  min(this.width, width / 2 - x - spaceX),
      //  this.height

        if (leftTopTilableSprite != null) {
            // leftTopDrawable.leftWidth
            leftTopTilableSprite?.apply {
                setOrigin(0f,0f)//this.height)
                draw(batch,
                    x,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width/2, //min(this.width, width / 2) + width/2,
                     this.height
                )
                //draw(batch)
                //dra
            }
        }

            if (leftTopSprite != null) {
            // leftTopDrawable.leftWidth
            /*
                leftTopSprite?.apply {
                setBounds(
                    x,
                    y + height - this.height,
                    min(this.width, width / 2),
                    this.height
                )
                draw(batch)
                //dra
            }

             */
/*
            if (rightTopDrawable != null) {
                rightTopDrawable?.draw(
                    batch,
                    x + width - rightTopDrawable?.minWidth!!,
                    y + height - rightTopDrawable?.minHeight!!,
                    min(rightTopDrawable?.minWidth!!, width / 2),
                    rightTopDrawable?.minHeight!!
                )

            } else {
                leftTopDrawable?.draw(
                    batch,
                    x + width,
                    y + height - leftTopDrawable?.minHeight!!,
                    -min(leftTopDrawable?.minWidth!!, width / 2),
                    leftTopDrawable?.minHeight!!
                )
            }


 */
            /*
            if(leftTopHorizontalSprite != null)
            {
                leftTopHorizontalSprite?.setOrigin(0f,leftTopHorizontalSprite?.height!!)
                leftTopHorizontalSprite?.setScale(0.5f)
                leftTopHorizontalSprite?.setPosition(x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopHorizontalSprite?.height!!)
                leftTopHorizontalSprite?.draw(batch)


             */
                /*
                leftTopHorizontalDrawableNew?.draw(
                    batch,
                    x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopHorizontalTextureRegion?.regionHeight!!,
                    min(leftTopHorizontalTextureRegion?.regionWidth!!.toFloat(), width/2 - leftTopDrawable?.minWidth!!),
                    leftTopHorizontalTextureRegion?.regionHeight!!.toFloat()
                )

                 */

                // }

/*
            if(leftTopHorizontalDrawableNew != null)
            {
                leftTopHorizontalDrawableNew?.draw(
                    batch,
                    x + leftTopDrawable?.minWidth!!,
                    y + height - leftTopHorizontalTextureRegion?.regionHeight!!,
                    min(leftTopHorizontalTextureRegion?.regionWidth!!.toFloat(), width/2 - leftTopDrawable?.minWidth!!),
                    leftTopHorizontalTextureRegion?.regionHeight!!.toFloat()
                )

            }


 */
                /*
                if(leftTopHorizontalDrawable != null){
                    leftTopHorizontalDrawable?.draw(
                        batch,
                        x + leftTopDrawable?.minWidth!!,
                        y + height - leftTopHorizontalDrawable?.minHeight!!,
                        min(leftTopHorizontalDrawable?.minWidth!!, width/2 - leftTopDrawable?.minWidth!!),
                        leftTopHorizontalDrawable?.minHeight!!
                    )
                    //leftTopHorizontalDrawable?.anchor = Anchor.RIGHT


                 */
                /*
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
                 */


                //}

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


     //   }

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
