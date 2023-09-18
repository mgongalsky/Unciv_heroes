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

    var leftTopSpacer: TextureRegion? = null
    var rightBottomSpacer: TextureRegion? = null

    var rightTopTextureRegion: TextureRegion? = null
    var rightTopTilableSprite: TilableSprite? = null

    var leftBottomTextureRegion: TextureRegion? = null
    var leftBottomTilableSprite: TilableSprite? = null

    var rightBottomTextureRegion: TextureRegion? = null
    var rightBottomTilableSprite: TilableSprite? = null


    var leftTopHorizontalTextureRegion: TextureRegion? = null
    var leftTopHorizontalSprite: Sprite? = null
    var leftTopHorizontalTilableSprite: TilableSprite? = null

    var leftTopVerticalTextureRegion: TextureRegion? = null
    var leftTopVerticalSprite: Sprite? = null
    var leftTopVerticalTilableSprite: TilableSprite? = null



    var rightTopHorizontalTextureRegion: TextureRegion? = null
    var rightTopHorizontalTilableSprite: TilableSprite? = null

    var rightTopVerticalTextureRegion: TextureRegion? = null
    var rightTopVerticalTilableSprite: TilableSprite? = null

    var leftBottomVerticalTextureRegion: TextureRegion? = null
    var leftBottomVerticalTilableSprite: TilableSprite? = null

    var rightBottomVerticalTextureRegion: TextureRegion? = null
    var rightBottomVerticalTilableSprite: TilableSprite? = null

    var topTextureRegion: TextureRegion? = null
    var topTiledDrawable: TiledDrawable? = null
    var topTilableSprite: TilableSprite? = null

    var bottomTextureRegion: TextureRegion? = null
    var bottomTilableSprite: TilableSprite? = null

    var leftTextureRegion: TextureRegion? = null
    var leftTilableSprite: TilableSprite? = null

    var rightTextureRegion: TextureRegion? = null
    var rightTilableSprite: TilableSprite? = null

    var leftBottomHorizontalTextureRegion: TextureRegion? = null
    var leftBottomHorizontalTilableSprite: TilableSprite? = null

    var rightBottomHorizontalTextureRegion: TextureRegion? = null
    var rightBottomHorizontalTilableSprite: TilableSprite? = null


    var leftTopHorizontalDrawable: Drawable? = null
    var leftTopVerticalDrawable: Drawable? = null
    var rightTopHorizontalDrawable: Drawable? = null
   // var rightTopVerticalDrawable: Drawable? = null
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
        leftTopVerticalTextureRegion = skin.get(jsonData.getString("leftTopVerticalTextureRegion"), TextureRegion::class.java)
      //  leftTopTextureRegion = skin.get(jsonData.getString("leftTopTextureRegion"), TextureRegion::class.java)
        rightTopHorizontalTextureRegion = skin.get(jsonData.getString("rightTopHorizontalTextureRegion"), TextureRegion::class.java)
        rightTopTextureRegion = skin.get(jsonData.getString("rightTopTextureRegion"), TextureRegion::class.java)
        rightTopVerticalTextureRegion = skin.get(jsonData.getString("rightTopVerticalTextureRegion"), TextureRegion::class.java)
       // rightTopTextureRegion = skin.get(jsonData.getString("rightTopTextureRegion"), TextureRegion::class.java)

        leftTopSpacer = skin.get(jsonData.getString("leftTopSpacer"), TextureRegion::class.java)


        leftBottomTextureRegion = skin.get(jsonData.getString("leftBottomTextureRegion"), TextureRegion::class.java)
        rightBottomTextureRegion = skin.get(jsonData.getString("rightBottomTextureRegion"), TextureRegion::class.java)


        leftBottomVerticalTextureRegion = skin.get(jsonData.getString("leftBottomHorizontalTextureRegion"), TextureRegion::class.java)
        rightBottomVerticalTextureRegion = skin.get(jsonData.getString("rightBottomHorizontalTextureRegion"), TextureRegion::class.java)

        leftBottomHorizontalTextureRegion = skin.get(jsonData.getString("leftBottomHorizontalTextureRegion"), TextureRegion::class.java)
        rightBottomHorizontalTextureRegion = skin.get(jsonData.getString("rightBottomHorizontalTextureRegion"), TextureRegion::class.java)


        //   topTextureRegion = skin.get(jsonData.getString("topHorizontalTextureRegion"), TextureRegion::class.java)
        topTextureRegion = skin.get(jsonData.getString("topTextureRegion"), TextureRegion::class.java)
        bottomTextureRegion = skin.get(jsonData.getString("bottomTextureRegion"), TextureRegion::class.java)
        leftTextureRegion = skin.get(jsonData.getString("leftTextureRegion"), TextureRegion::class.java)
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

        // Left top corner
        if(leftTopTilableSprite == null && leftTopTextureRegion != null) {
            leftTopTilableSprite = TilableSprite(leftTopTextureRegion!!)
            leftTopTilableSprite?.apply {
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }

        // Left top horizontal border
        if(leftTopHorizontalTilableSprite == null && leftTopHorizontalTextureRegion != null) {
            leftTopHorizontalTilableSprite = TilableSprite(leftTopHorizontalTextureRegion!!)
            leftTopHorizontalTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Right top corner
        if(rightTopTilableSprite == null && (rightTopTextureRegion != null || leftTopTextureRegion != null)) {
            rightTopTilableSprite = TilableSprite(rightTopTextureRegion ?: leftTopTextureRegion!!)
            rightTopTilableSprite?.apply {
                // TODO: flip only if rightTexture is absent
                flip(true, false)
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }

        // Right top horizontal border
        if(rightTopHorizontalTilableSprite == null && (rightTopHorizontalTextureRegion != null || leftTopHorizontalTextureRegion != null)) {
            rightTopHorizontalTilableSprite = TilableSprite(rightTopHorizontalTextureRegion ?: leftTopHorizontalTextureRegion!!)
            rightTopHorizontalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(true, false)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Left bottom corner
        if(leftBottomTilableSprite == null && (leftBottomTextureRegion != null || leftTopTextureRegion != null)) {
            leftBottomTilableSprite = TilableSprite(leftBottomTextureRegion ?: leftTopTextureRegion!!)
            leftBottomTilableSprite?.apply {
                // TODO: flip only if rightTexture is absent
                flip(false, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }

        // Right bottom corner
        if(rightBottomTilableSprite == null && (rightBottomTextureRegion != null || leftTopTextureRegion != null)) {
            rightBottomTilableSprite = TilableSprite(rightBottomTextureRegion ?: leftTopTextureRegion!!)
            rightBottomTilableSprite?.apply {
                // TODO: flip only if rightTexture is absent
                flip(true, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)
            }

        }
/*
        // Left bottom horizontal border
        if(leftBottomHorizontalTilableSprite == null && leftBottomHorizontalTextureRegion != null) {
            leftBottomHorizontalTilableSprite = TilableSprite(leftBottomHorizontalTextureRegion!!)
            leftBottomHorizontalTilableSprite?.apply{
                flip(false, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }


 */
        // Left bottom horizontal border
        if(leftBottomHorizontalTilableSprite == null && (leftBottomHorizontalTextureRegion != null || leftTopHorizontalTextureRegion != null)) {
            leftBottomHorizontalTilableSprite = TilableSprite(leftBottomHorizontalTextureRegion ?: leftTopHorizontalTextureRegion!!)
            leftBottomHorizontalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(false, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Right bottom horizontal border
        if(rightBottomHorizontalTilableSprite == null && (rightBottomHorizontalTextureRegion != null || leftTopHorizontalTextureRegion != null)) {
            rightBottomHorizontalTilableSprite = TilableSprite(rightBottomHorizontalTextureRegion ?: leftTopHorizontalTextureRegion!!)
            rightBottomHorizontalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(true, true)
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

        // Bottom border
        if(bottomTilableSprite == null && (bottomTextureRegion != null || topTextureRegion != null)) {
            bottomTilableSprite = TilableSprite(bottomTextureRegion ?: topTextureRegion!!)
            bottomTilableSprite?.apply{
                flip(false, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)
                isTiledX = true

            }
        }


        // Vertical parts:

        // Left top vertical border (non-tilable)
        if(leftTopVerticalTilableSprite == null && leftTopVerticalTextureRegion != null) {
            leftTopVerticalTilableSprite = TilableSprite(leftTopVerticalTextureRegion!!)
            leftTopVerticalTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Right vertical border
        if(rightTopVerticalTilableSprite == null && (rightTopVerticalTextureRegion != null || leftTopVerticalTextureRegion != null)) {
            rightTopVerticalTilableSprite = TilableSprite(rightTopVerticalTextureRegion ?: leftTopVerticalTextureRegion!!)
            rightTopVerticalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(true, false)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Left border
        if(leftTilableSprite == null && leftTextureRegion != null) {
            leftTilableSprite = TilableSprite(leftTextureRegion!!)
            leftTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)
                isTiledY = true

            }
        }

        // Right border
        if(rightTilableSprite == null && (rightTextureRegion != null || leftTextureRegion != null)) {
            rightTilableSprite = TilableSprite(rightTextureRegion ?: leftTextureRegion!!)
            rightTilableSprite?.apply{
                setOrigin(0f, 0f)
                setScale(scaleFrame)
                isTiledY = true

            }
        }

        // Left bottom vertical border
        if(leftBottomVerticalTilableSprite == null && (leftBottomVerticalTextureRegion != null || leftTopVerticalTextureRegion != null)) {
            leftBottomVerticalTilableSprite = TilableSprite(leftBottomVerticalTextureRegion ?: leftTopVerticalTextureRegion!!)
            leftBottomVerticalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(false, true)
                setOrigin(0f, 0f)
                setScale(scaleFrame)

            }
        }

        // Right bottom vertical border
        if(rightBottomVerticalTilableSprite == null && (rightBottomVerticalTextureRegion != null || leftTopVerticalTextureRegion != null)) {
            rightBottomVerticalTilableSprite = TilableSprite(rightBottomVerticalTextureRegion ?: leftTopVerticalTextureRegion!!)
            rightBottomVerticalTilableSprite?.apply{
                // TODO: flip only if rightTexture is absent

                flip(true, true)
                setOrigin(0f, 0f)
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
        if(leftTopVerticalTextureRegion != null && leftTopVerticalSprite == null){
            leftTopVerticalSprite = Sprite(leftTopVerticalTextureRegion)


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
        //val xPixelRatio = Gdx.graphics.width.toFloat() / width
       // val yPixelRatio = Gdx.graphics.height.toFloat() / height
       // val realWidth = Gdx.graphics.width.toFloat()
       // val realHeight = Gdx.graphics.height.toFloat()

        // TODO: here we need instead of dividing
        var xLeftSpacer = 0
        var yTopSpacer = 0
        if(leftTopSpacer != null) {
            xLeftSpacer = leftTopSpacer?.regionWidth!! / 2
            yTopSpacer = leftTopSpacer?.regionHeight!! / 2
            //leftTopSpacer?.texture.
        }

        var xRightSpacer = 0
        var yBottomSpacer = 0
        if(leftTopSpacer != null) {
            if(rightBottomSpacer != null){
                xRightSpacer = rightBottomSpacer?.regionWidth!! / 2
                yBottomSpacer = rightBottomSpacer?.regionHeight!! / 2
            }
            else {
                xRightSpacer = leftTopSpacer?.regionWidth!! / 2
                yBottomSpacer = leftTopSpacer?.regionHeight!! / 2
            }
        }




/*
        tiledBackground?.draw(
            batch,
            x + leftTopDrawable?.minWidth!! * scaleFrame * scaleFrame / 2,
            y + x + leftTopDrawable?.minHeight!! * scaleFrame * scaleFrame  / 2,
            width - leftTopDrawable?.minWidth!! * scaleFrame * scaleFrame ,
            height - leftTopDrawable?.minHeight!! * scaleFrame * scaleFrame
        )


 */
        tiledBackground?.draw(
            batch,
            x + xLeftSpacer,
            y + yBottomSpacer,
            width - xLeftSpacer - xRightSpacer ,
            height - yBottomSpacer - yTopSpacer
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
                    width / 2f, //min(this.width, width / 2) + width/2,
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

        // Left Bottom Corner
        if (leftBottomTilableSprite != null) {
            leftBottomTilableSprite?.apply {
                isAnchoredLeft = false
                draw(
                    batch,
                    x,
                    y,
                    this.width, //min(this.width, width / 2) + width/2,
                    this.height
                )
            }
        }

        // Right Bottom Corner
        if (rightBottomTilableSprite != null) {
            rightBottomTilableSprite?.apply {
                isAnchoredLeft = true
                draw(
                    batch,
                    x + width - this.width * scaleFrame * scaleFrame,
                    y,
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

                draw(
                    batch,
                    x + spaceX,
                    y + height - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
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
                val dbx1 = x + spaceX
                val dbx2 = width - spaceX * 2
                val dxy1 = y + height - this.height * scaleFrame * scaleFrame
                val dby2 = this.height
/*
                draw(
                    batch,
                    x + spaceX,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width - spaceX * 2, //min(this.width, width / 2) + width/2,
                    this.height
                )


 */
                draw(
                    batch,
                    x + spaceX,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    this.height
                )

                isAnchoredLeft = false

                draw(
                    batch,
                    x + width / 2f,
                    y + height - this.height * scaleFrame * scaleFrame,
                    width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    this.height
                )

            }
        }

        // Bottom Border
        if (bottomTilableSprite != null) {
            bottomTilableSprite?.apply {
                val spaceX = (leftTopTilableSprite?.width!! + leftTopHorizontalTilableSprite?.width!!) * scaleFrame * scaleFrame
                isTiledX = true
                isAnchoredBottom = true
                isAnchoredLeft = true

                draw(
                    batch,
                    x + spaceX,
                    y,
                    width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    this.height
                )

                isAnchoredLeft = false

                draw(
                    batch,
                    x + width / 2f,
                    y,
                    width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    this.height
                )

            }
        }

        // Vertical elements:

        // Left Top Horizontal
        if (leftTopVerticalTilableSprite != null) {
            leftTopVerticalTilableSprite?.apply {
                // TODO: Boundaries is right, but the sprite is stretched if it is limited.
                val spaceY = leftTopTilableSprite?.height!! * scaleFrame * scaleFrame

                draw(
                    batch,
                    x,
                    y + height - spaceY - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
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

        if (rightTopVerticalTilableSprite != null) {
            rightTopVerticalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                val spaceY = leftTopTilableSprite?.height!! * scaleFrame * scaleFrame

                draw(
                    batch,
                    x + width - this.width * scaleFrame * scaleFrame,
                    y + height - spaceY - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
                    this.height
                )

            }
        }

        // Right bottom vertical border
        if (rightBottomVerticalTilableSprite != null) {
            rightBottomVerticalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                //isAnchoredBottom = true
                //isAnchoredLeft = false
       //         val spaceX = abs(leftTopTilableSprite?.width!! * scaleFrame * scaleFrame)

                val spaceY = leftTopTilableSprite?.height!! * scaleFrame * scaleFrame
/*
                draw(
                    batch,
                    x, // + width - this.width * scaleFrame * scaleFrame,
                    y + spaceY, // + height - spaceY - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
                    this.height
                )


 */
                draw(
                    batch,
                    x + width - this.width * scaleFrame * scaleFrame,
                    y + spaceY,// - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
                    this.height
                )

            }
        }

        if (leftBottomVerticalTilableSprite != null) {
            leftBottomVerticalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                isAnchoredBottom = true
                isAnchoredLeft = false
                val spaceY = leftTopTilableSprite?.height!! * scaleFrame * scaleFrame
/*
                draw(
                    batch,
                    x, // + width - this.width * scaleFrame * scaleFrame,
                    y + spaceY, // + height - spaceY - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
                    this.height
                )


 */
                draw(
                    batch,
                    x,
                    y + spaceY,// - this.height * scaleFrame * scaleFrame,
                    this.width,//min(this.width, (width / 2 - spaceX )) ,
                    this.height
                )

            }
        }


        // Left Border
        if (leftTilableSprite != null) {
            leftTilableSprite?.apply {
                val spaceY = (leftTopTilableSprite?.height!! + leftTopVerticalTilableSprite?.height!!) * scaleFrame * scaleFrame
                isTiledY = true
                isAnchoredBottom = false
                isAnchoredLeft = true

                draw(
                    batch,
                    x,
                    y + height / 2f, // - this.height * scaleFrame * scaleFrame - spaceY,
                    this.width,//width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    height / 2f - spaceY
                )

                isAnchoredBottom = true

                draw(
                    batch,
                    x,
                    y + spaceY, // - this.height * scaleFrame * scaleFrame - spaceY,
                    this.width,//width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    height / 2f - spaceY
                )


            }
        }


        // Right Border
        if (rightTilableSprite != null) {
            rightTilableSprite?.apply {
                val spaceY = (leftTopTilableSprite?.height!! + leftTopVerticalTilableSprite?.height!!) * scaleFrame * scaleFrame
                isTiledY = true
                isAnchoredBottom = false
                isAnchoredLeft = false

                draw(
                    batch,
                    x + width - this.width,
                    y + height / 2f, // - this.height * scaleFrame * scaleFrame - spaceY,
                    this.width,//width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    height / 2f - spaceY
                )

                isAnchoredBottom = true

                draw(
                    batch,
                    x + width - this.width,
                    y + spaceY, // - this.height * scaleFrame * scaleFrame - spaceY,
                    this.width,//width / 2f - spaceX, //min(this.width, width / 2) + width/2,
                    height / 2f - spaceY
                )


            }
        }

        // Left Bottom Horizontal
        if (leftBottomHorizontalTilableSprite != null) {
            leftBottomHorizontalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                val spaceX = abs(leftTopTilableSprite?.width!! * scaleFrame * scaleFrame)
                draw(
                    batch,
                    x + spaceX,
                    y,
                    this.width, //min(this.width, width / 2) + width/2,
                    this.height
                )

            }
        }

        // Right Bottom Horizontal
        if (rightBottomHorizontalTilableSprite != null) {
            rightBottomHorizontalTilableSprite?.apply {
                // TODO: Here we need to crop the sprite if it goes out of right half of the screen
                val spaceX = abs(leftTopTilableSprite?.width!! * scaleFrame * scaleFrame)
                draw(
                    batch,
                    x + width - spaceX - this.width * scaleFrame * scaleFrame,
                    y,
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
