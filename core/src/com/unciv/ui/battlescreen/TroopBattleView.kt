package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup

/** Represents the view of a troop in battles. */
class TroopBattleView(
    private val troop: Troop,
    private val battleScreen: BattleScreen
) {
    private val troopGroup = Group()
    private var troopImages: ArrayList<Image>
    private var activeOutline: Group? = null

    /** Initialize the troop's battle appearance. */
    @Deprecated("To be removed")
    fun initialize(civColor: Color) {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troop.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, civColor, civColor)
    }

    init {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troop.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)
        troopGroup.name = "troopGroup"
        troopGroup.addListener(object : ClickListener() {
            override fun enter(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                fromActor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                battleScreen.setHoveredTroop(troop)
                super.enter(event, x, y, pointer, fromActor)
            }

            override fun exit(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                battleScreen.setHoveredTroop(null)
                super.exit(event, x, y, pointer, toActor)
            }
        })
    }

    fun getTroopInfo(): Troop = troop

    /** Returns the current group of the troop for rendering. */
    fun getCurrentGroup(): Group = troopGroup

    private val DEBUG_LOGS_ENABLED = true

    fun updatePosition(targetTileGroup: TileGroup?) {
        if (targetTileGroup == null) {
            if (DEBUG_LOGS_ENABLED) println("Error: Target tile group is null!")
            return
        }
        troopGroup.remove()
        if (DEBUG_LOGS_ENABLED) println("Troop removed from previous tile.")
        if (DEBUG_LOGS_ENABLED) println("Troop moved to new tile at position: (${targetTileGroup.x}, ${targetTileGroup.y})")
        targetTileGroup.addActor(troopGroup)
        targetTileGroup.update()
        if (DEBUG_LOGS_ENABLED) println("Troop added to target tile group.")
    }

    fun draw(tileGroup: TileGroup, attacker: Boolean) {
        populateBattleTroopGroup(
            target = troopGroup,
            troop = troop,
            attacker = attacker,
            parentWidth = tileGroup.width,
            parentHeight = tileGroup.height,
            originX = tileGroup.originX,
            originY = tileGroup.originY
        )
        activeOutline = createActiveTroopOutline(
            troop = troop,
            attacker = attacker,
            parentWidth = tileGroup.width,
            parentHeight = tileGroup.height,
            originX = tileGroup.originX,
            originY = tileGroup.originY
        ).also { outline ->
            outline.isVisible = battleScreen.getCurrentTroopView()?.getTroopInfo() === troop
            troopGroup.addActorAt(0, outline)
        }
        tileGroup.addActor(troopGroup)
    }

    fun setActive(active: Boolean) {
        activeOutline?.isVisible = active
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

    fun updateStats() {
        Gdx.app.postRunnable {
            val amountLabel = troopGroup.findActor<Label>("amountLabel")
            if (amountLabel != null) {
                amountLabel.setText(troop.currentAmount.toString())
            } else {
                println("Amount Label not found")
            }
            updateFormationBar(troopGroup, troop)
            println(
                "Troop stats updated: ${troop.unitName}, Amount: ${troop.currentAmount}, " +
                        "Formation: ${troop.formation.current}/${troop.formation.maximum}"
            )
        }
    }

    /** Remove the troop's group from the stage when it perishes. */
    fun perish() {
        troopGroup.remove()
    }
}
