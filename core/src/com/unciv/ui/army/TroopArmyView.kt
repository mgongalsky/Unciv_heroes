package com.unciv.ui.army


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.logic.army.TroopInfo

/**
 * Represents the view of a troop in an army context (e.g., garrison, hero screen).
 */
class TroopArmyView(
    private val troopInfo: TroopInfo
) : Group() {
    private val troopGroup = Group()
    private lateinit var troopImages: ArrayList<Image>

    init {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)

        drawInArmy()
    }

    fun createMonochromaticTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()

        val texture = Texture(pixmap)
        pixmap.dispose()

        return texture
    }

    fun createBorderDrawable(texture: Texture, borderWidth: Float, borderColor: Color): Drawable {
        val sprite = Sprite(texture)
        val borderSprite = Sprite(texture)

        borderSprite.setSize(sprite.width + borderWidth * 2f, sprite.height + borderWidth * 2f)
        borderSprite.setColor(borderColor)

        val borderBatch = SpriteBatch()
        borderBatch.begin()
        borderSprite.draw(borderBatch)
        borderBatch.end()

        val drawable = SpriteDrawable(sprite)
        drawable.leftWidth = borderWidth
        drawable.rightWidth = borderWidth
        drawable.topHeight = borderWidth
        drawable.bottomHeight = borderWidth

        return drawable
    }


    /** Draw the troop in an army context (e.g., garrison, hero screen). */
    fun drawInArmy() {

        val bgTexture = createMonochromaticTexture(64, 64, Color.BROWN) // Adjust the dimensions and color as needed
        val bgTextureActive = createMonochromaticTexture(64, 64, Color.OLIVE) // Adjust the dimensions and color as needed

        // Create a drawable with a border using the texture
        val drawable = createBorderDrawable(bgTexture, 5f, Color.WHITE) // Adjust the border width and color as needed


        //val cell: Cell<*> = garrisonWidget.add()


        val backgroundImage = Image(bgTexture)

        // Create a group to hold the troopGroup and the background actor
        //val bgGroup = Group()
        troopGroup.addActor(backgroundImage)


        val amountText = Label(troopInfo.currentAmount.toString(), BaseScreen.skin)
        amountText.scaleBy(0.5f)
        amountText.moveBy(50f, 0.5f)

        amountText.name = "amountLabel"
        amountText.touchable = Touchable.disabled
        troopGroup.findActor<Label>("amountLabel")?.remove()
        troopGroup.addActor(amountText)

        for (troopImage in troopImages) {
            troopImage.setScale(-0.125f, 0.125f)
            troopImage.moveBy(60f, 0f)
            //troopImage.setOrigin(troopGroup.originX, troopGroup.originY)
            troopImage.touchable = Touchable.disabled

            troopImage.name = "troopImage"
            troopGroup.name = "troopGroup"
            troopGroup.findActor<Image>("troopImage")?.remove()
            troopGroup.addActor(troopImage)
        }


        // Here is the problem:
        addActor(troopGroup)
    }

    /** Handle click interaction for troop selection. */
    fun onClick(action: () -> Unit) {
        troopGroup.touchable = Touchable.enabled // Разрешить взаимодействие с группой
        troopGroup.addListener(object : ClickListener() { // Добавляем слушатель клика
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                action() // Выполняем переданное действие
            }
        })
    }


    /** Remove the troop from the view (e.g., when dismissed). */
    fun dismiss() {
        troopGroup.remove()
    }
}
