package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.army.ArmyView

/**
 * Represents the view of a troop in battles.
 */
class TroopBattleView(
    private val troopInfo: TroopInfo,
    private val battleScreen: NewBattleScreen
    ) {
    private val troopGroup = Group()
    private var troopImages: ArrayList<Image>

    /** Initialize the troop's battle appearance. */
    // Deprecated. To be removed
    fun initialize(civColor: Color) {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, civColor, civColor)
    }

    init {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)


    }

    fun getBattlefieldPosition(): Vector2 {
        return troopInfo.position
    }

    fun getTroopInfo(): TroopInfo{
        return troopInfo
    }

    /** Draw the troop on the battle field. */
    fun draw(tileGroup: TileGroup, attacker: Boolean) {
        val amountText = Label(troopInfo.currentAmount.toString(), BaseScreen.skin)
        amountText.moveBy(tileGroup.width * 0.5f, 0f)

        for (troopImage in troopImages) {
            troopImage.setScale(if (attacker) -0.25f else 0.25f, 0.25f)
            troopImage.moveBy(
                if (attacker) tileGroup.width * 1.3f else tileGroup.width * -0.3f,
                tileGroup.height * 0.15f
            )
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            troopGroup.addActor(troopImage)
        }

        troopGroup.addActor(amountText)
        tileGroup.addActor(troopGroup)
    }

    /** Show morale animation (e.g., after gaining morale). */
    fun showMoraleBird() {
        val moraleImage = ImageGetter.getExternalImage("MoraleBird.png").apply {
            setScale(0.075f)
            moveBy(troopGroup.parent.width * -0.035f, troopGroup.parent.height * 1.85f)
            touchable = Touchable.disabled
            color = Color.WHITE.cpy().apply { a = 0f }
        }

        moraleImage.addAction(
            Actions.sequence(
                Actions.alpha(1f, 0.5f),
                Actions.delay(0.3f),
                Actions.alpha(0f, 0.5f)
            )
        )

        troopGroup.addActor(moraleImage)
    }

    /** Remove the troop's group from the stage when it perishes. */
    fun perish() {
        troopGroup.remove()
    }
}
