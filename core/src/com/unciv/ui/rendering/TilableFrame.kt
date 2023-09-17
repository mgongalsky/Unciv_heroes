package com.unciv.ui.rendering

import com.badlogic.gdx.Gdx
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
import kotlin.math.sqrt

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
    var rightTopTilableSprite: TilableSprite? = null

    var leftTopHorizontalTextureRegion: TextureRegion? = null
    var leftTopHorizontalSprite: Sprite? = null
    var leftTopHorizontalTilableSprite: TilableSprite? = null


    var rightTopHorizontalTextureRegion: TextureRegion? = null
    var rightTopHorizontalTilableSprite: TilableSprite? = null


    var topTextureRegion: TextureRegion? = null
    var topTiledDrawable: TiledDrawable? = null
    var topTilableSprite: TilableSprite? = null



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
        scaleFrame = sqrt( skin.get(jsonData.getString("scaleFrame"), Float::class.java))
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
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }

        // Left horizontal border
        if(leftTopHorizontalTilableSprite == null && leftTopHorizontalTextureRegion != null) {
            leftTopHorizontalTilableSprite = TilableSprite(leftTopHorizontalTextureRegion!!)
            leftTopHorizontalTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Right corner
        if(rightTopTilableSprite == null && (rightTopTextureRegion != null || leftTopTextureRegion != null)) {
            rightTopTilableSprite = TilableSprite(rightTopTextureRegion ?: leftTopTextureRegion!!)
            rightTopTilableSprite?.apply {
                // TODO: flip only if rightTexture is absent
                flip(true, false)
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }

        // Right horizontal border
        if(rightTopHorizontalTilableSprite == null && (rightTopHorizontalTextureRegion != null || leftTopHorizontalTextureRegion != null)) {
            rightTopHorizontalTilableSprite = TilableSprite(rightTopHorizontalTextureRegion ?: leftTopHorizontalTextureRegion!!)
            rightTopHorizontalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(true, false)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }


/*
        if(rightTopTilableSprite == null && (rightTopTextureRegion != null || leftTopTextureRegion != null)) {
            rightTopTilableSprite = Sprite(rightTopTextureRegion ?: leftTopTextureRegion!!)
            rightTopTilableSprite?.apply {
                setOrigin(width, height)
                flip(true, false)
                setScale(scaleFrame)
            }
        }

 */


        // Top border
        if(topTilableSprite == null && topTextureRegion != null) {
            topTilableSprite = TilableSprite(topTextureRegion!!)
            topTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)
                isTiledX = true

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

        // Here we introduce Pixel ratios. Real size in physical pixels and virtual pixels
        // We have our sprites in physical pixels, but all logic of the screen in virtual pixels
        val xPixelRatio = Gdx.graphics.width / width
        val yPixelRatio = Gdx.graphics.height / height

        // TODO: here we need instead of dividing
        tiledBackground?.draw(
            batch,
            x + leftTopDrawable?.minWidth!! * scaleFrame * scaleFrame / 2,
            y + x + leftTopDrawable?.minHeight!! * scaleFrame * scaleFrame  / 2,
            width - leftTopDrawable?.minWidth!! * scaleFrame * scaleFrame ,
            height - leftTopDrawable?.minHeight!! * scaleFrame * scaleFrame
        )
        //batch.end()

       // background?.draw(batch,x,y,width,height)

        // Left Top Corner
        if (leftTopTilableSprite != null) {
            leftTopTilableSprite?.apply {
                draw(
                    batch,
                    x,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width / 2, //min(this.width, width / 2) + width/2,
                    this.height
                )
            }
        }

        // Right Top Corner
        if (rightTopTilableSprite != null) {
            rightTopTilableSprite?.apply {
                isAnchoredLeft = false
                draw(
                    batch,
                    x + width - this.width * scaleFrame * scaleFrame,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width, //min(this.width, width / 2) + width/2,
                    this.height
                )
            }
        }

        // TODO: correct
        // Left Top Horizontal
        if (leftTopHorizontalTilableSprite != null) {
            leftTopHorizontalTilableSprite?.apply {
                // TODO: Boundaries is right, but the sprite is stretched if it is limited.
                val spaceX = leftTopTilableSprite?.width!! * scaleFrame * scaleFrame
                val dbx1 = x + spaceX
                val dbx2 = this.width
                val dbx3 = this.width * width / Gdx.graphics.width
                val dby1 = y + height - this.height * scaleFrame * scaleFrame
                val dby2 = this.height
                val dbright1 = width / 2f
                val dbright2 = dbx1 + min(this.width, width / 2 - spaceX)

                draw(
                    batch,
                    x + spaceX,
                    y + height - this.height * scaleFrame * scaleFrame,
                    min(this.width, (width / 2 - spaceX) * xPixelRatio) ,
                    this.height
                )
            }
        }

        // Top Border
        if (topTilableSprite != null) {
            topTilableSprite?.apply {
                val spaceX = (leftTopTilableSprite?.width!! + leftTopHorizontalTilableSprite?.width!!) * scaleFrame * scaleFrame
                isTiledX = true
                isAnchoredBottom = true
                isAnchoredLeft = true
                draw(
                    batch,
                    x + spaceX,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width - spaceX * 2, //min(this.width, width / 2) + width/2,
                    this.height
                )
            }
        }

        // Right

        if (rightTopHorizontalTilableSprite != null) {
            rightTopHorizontalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                val spaceX = abs(rightTopTilableSprite?.width!! * scaleFrame * scaleFrame)
                draw(
                    batch,
                    x + width - spaceX - this.width * scaleFrame * scaleFrame,
                    y + height - this.height * scaleFrame * scaleFrame,
                    this.width, //min(this.width, width / 2) + width/2,
                    this.height
                )
            }
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
