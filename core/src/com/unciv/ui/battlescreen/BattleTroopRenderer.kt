package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen

private const val FORMATION_BAR_WIDTH = 42f
private const val FORMATION_BAR_HEIGHT = 6f
private const val FORMATION_BORDER_WIDTH = 1f

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
    target.addActor(createFormationBar(troop, parentWidth))
}

fun updateFormationBar(target: Group, troop: Troop) {
    val bar = target.findActor<Group>("formationBar") ?: return
    val fill = bar.findActor<Image>("formationFill") ?: return
    fill.width = (FORMATION_BAR_WIDTH - FORMATION_BORDER_WIDTH * 2) * troop.formation.fraction
    val borderColor = if (troop.formation.isBroken) Color.RED else Color(0.15f, 0.3f, 0.5f, 1f)
    listOf(
        "formationBorderTop",
        "formationBorderBottom",
        "formationBorderLeft",
        "formationBorderRight"
    )
        .mapNotNull { bar.findActor<Image>(it) }
        .forEach { it.color = borderColor }
}

private fun createFormationBar(troop: Troop, parentWidth: Float): Group {
    val bar = Group().apply {
        name = "formationBar"
        setSize(FORMATION_BAR_WIDTH, FORMATION_BAR_HEIGHT)
        setPosition(parentWidth * 0.5f - FORMATION_BAR_WIDTH / 2f, 18f)
        isTransform = false
    }
    bar.addActor(
        dot(
            "formationBackground",
            Color(0.04f, 0.07f, 0.12f, 0.9f),
            0f,
            0f,
            FORMATION_BAR_WIDTH,
            FORMATION_BAR_HEIGHT
        )
    )
    bar.addActor(
        dot(
            "formationFill",
            Color(0.15f, 0.65f, 1f, 1f),
            FORMATION_BORDER_WIDTH,
            FORMATION_BORDER_WIDTH,
            (FORMATION_BAR_WIDTH - FORMATION_BORDER_WIDTH * 2) * troop.formation.fraction,
            FORMATION_BAR_HEIGHT - FORMATION_BORDER_WIDTH * 2
        )
    )
    val borderColor = if (troop.formation.isBroken) Color.RED else Color(0.15f, 0.3f, 0.5f, 1f)
    bar.addActor(
        dot(
            "formationBorderTop",
            borderColor,
            0f,
            FORMATION_BAR_HEIGHT - FORMATION_BORDER_WIDTH,
            FORMATION_BAR_WIDTH,
            FORMATION_BORDER_WIDTH
        )
    )
    bar.addActor(
        dot(
            "formationBorderBottom",
            borderColor,
            0f,
            0f,
            FORMATION_BAR_WIDTH,
            FORMATION_BORDER_WIDTH
        )
    )
    bar.addActor(
        dot(
            "formationBorderLeft",
            borderColor,
            0f,
            0f,
            FORMATION_BORDER_WIDTH,
            FORMATION_BAR_HEIGHT
        )
    )
    bar.addActor(
        dot(
            "formationBorderRight",
            borderColor,
            FORMATION_BAR_WIDTH - FORMATION_BORDER_WIDTH,
            0f,
            FORMATION_BORDER_WIDTH,
            FORMATION_BAR_HEIGHT
        )
    )
    return bar
}

private fun dot(name: String, color: Color, x: Float, y: Float, width: Float, height: Float) =
        ImageGetter.getWhiteDot().apply {
            this.name = name
            this.color = color
            setPosition(x, y)
            setSize(width, height)
        }
