package com.unciv.ui.army


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
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


    /** Draw the troop in an army context (e.g., garrison, hero screen). */
    fun drawInArmy() {
        val amountText = Label(troopInfo.currentAmount.toString(), BaseScreen.skin)
        amountText.scaleBy(0.5f)
        amountText.moveBy(50f, 0.5f)

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
