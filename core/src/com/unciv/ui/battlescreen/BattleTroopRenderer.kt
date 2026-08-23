package com.unciv.ui.battlescreen

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen

fun populateBattleTroopGroup(
    target: Group,
    troop: Troop,
    attacker: Boolean,
    parentWidth: Float,
    parentHeight: Float,
    originX: Float,
    originY: Float
) {
    val amountLabel = Label(troop.currentAmount.toString(), BaseScreen.skin).apply {
        name = "amountLabel"
        setPosition(parentWidth * 0.5f, 0f)
    }

    ImageGetter.getLayeredImageColored(
        "TileSets/AbsoluteUnits/Units/${troop.unitName}",
        null,
        null,
        null
    ).forEach { troopImage ->
        troopImage.setScale(if (attacker) -0.25f else 0.25f, 0.25f)
        troopImage.setPosition(
            if (attacker) parentWidth * 1.3f else parentWidth * -0.3f,
            parentHeight * 0.15f
        )
        troopImage.setOrigin(originX, originY)
        troopImage.name = "troopImage"
        target.addActor(troopImage)
    }
    target.addActor(amountLabel)
}
