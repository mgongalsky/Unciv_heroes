package com.unciv.ui


import com.badlogic.gdx.graphics.Color
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
) {
    private val troopGroup = Group()
    private lateinit var troopImages: ArrayList<Image>

    /** Initialize the troop's army appearance. */
    fun initialize(civColor: Color) {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, civColor, civColor)
    }

    /** Draw the troop in an army context (e.g., garrison, hero screen). */
    fun drawInArmy(group: Group) {
        val amountText = Label(troopInfo.currentAmount.toString(), BaseScreen.skin)
        amountText.scaleBy(0.5f)
        amountText.moveBy(group.width * 0.6f, 0.5f)

        for (troopImage in troopImages) {
            troopImage.setScale(-0.125f, 0.125f)
            troopImage.moveBy(group.width * 0.8f, 0f)
            troopImage.touchable = Touchable.disabled
            troopGroup.addActor(troopImage)
        }

        troopGroup.addActor(amountText)
        group.addActor(troopGroup)
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
