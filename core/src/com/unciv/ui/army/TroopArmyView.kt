package com.unciv.ui.army

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.logic.army.TroopInfo
import com.unciv.pure.domain.troop.Troop

/**
 * Represents the visual representation of a single troop in an army.
 * Includes functionality for selection/deselection and updating the display accordingly.
 */
class TroopArmyView(
    internal val troopInfo: Troop?, // Null indicates an empty slot
    private val armyView: ArmyView,
    private val avatarOnly: Boolean = false, // avatar-only mode
    private val slotSize: Float = 64f, // Размер слота, по умолчанию 64x64
    private val isInteractive: Boolean = true // Controls whether the view responds to clicks
) : Group() {
    private val troopGroup = Group() // A group to contain all troop-related visuals
    private lateinit var troopImages: ArrayList<Image> // Images representing the troop's layers
    private var selected: Boolean = false // Indicates whether the troop is selected


    companion object {
        var isVerbose = false // Flag to enable or disable verbose output
    }

    /**
     * Copy constructor to create a new TroopArmyView based on an existing one.
     * Copies `troopInfo` and initializes `armyView` for standalone usage.
     */
    constructor(original: TroopArmyView, isAvatarOnly: Boolean, newSlotSize: Float = 140f, interactive: Boolean = false) : this(
        troopInfo = original.troopInfo, // Copy troopInfo to ensure it's not the same reference
        armyView = original.armyView, // ArmyView reference remains the same, modify if needed for detached usage
        avatarOnly = isAvatarOnly,
        slotSize = newSlotSize,
        isInteractive = interactive
            ) {


    }

    // TODO: inject Ruleset here.
    init {
        // Load troop images based on the troop's unit name
        if(troopInfo != null) {
            val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
            troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)
        }
        // Initial drawing of the troop view
        draw()
        if (isInteractive) {
            setupClickListener()
        }
    }

    fun isEmptySlot(): Boolean {
        return troopInfo == null
    }

    /**
     * Marks the troop as selected and updates its appearance.
     */
    fun select() {
        if (!selected) {
            if(troopInfo != null)
                logVerbose("Troop selected: ${troopInfo.unitName}")
            else
                logVerbose("Empty slot selected")

            selected = true
            updateViewForSelection()
        }
    }

    /**
     * Marks the troop as deselected and updates its appearance.
     */
    fun deselect() {
        if (selected) {
            if(troopInfo != null)
                logVerbose("Troop deselected: ${troopInfo.unitName}")
            else
                logVerbose("Empty slot deselected")
            selected = false
            updateViewForSelection()
        }
    }

    /** Меняет только цвет фона слота, не изменяя общую текстуру и изображение отряда. */
    private fun updateViewForSelection() {
        val backgroundImage = troopGroup.findActor<Image>("backgroundImage") ?: return
        val royal = armyView.getScreen().game.settings.skin == "Minimal"
        backgroundImage.color = if (royal)
            Color.valueOf(if (selected) "C6C394" else "EADFBD")
        else if (selected) Color.GOLD else Color.BROWN
    }

    /** Собирает слот из общих ресурсов; повторная отрисовка не создаёт текстуры фона. */
    fun draw() {
        troopGroup.findActor<Image>("backgroundImage")?.remove()
        val backgroundImage = ImageGetter.getWhiteDot().apply {
            name = "backgroundImage"
            setSize(slotSize, slotSize)
            touchable = if (isInteractive) Touchable.enabled else Touchable.disabled
            align = Align.center
        }
        troopGroup.addActor(backgroundImage)
        backgroundImage.toBack()
        updateViewForSelection()
        drawAvatar()

        if (!avatarOnly && troopInfo != null) {
            val amountText = Label(troopInfo.amount.toString(), BaseScreen.skin).apply {
                if (armyView.getScreen().game.settings.skin == "Minimal") {
                    style = Label.LabelStyle(style).apply { fontColor = Color.valueOf("453925") }
                }
                setAlignment(Align.right)
                scaleBy(0.5f * slotSize / 64f)
                moveBy(backgroundImage.width * 0.95f, 0.5f)
                setPosition(x, y, Align.bottomRight)
                name = "amountLabel"
                touchable = Touchable.disabled
            }
            troopGroup.findActor<Label>("amountLabel")?.remove()
            troopGroup.addActor(amountText)
        }
        addActor(troopGroup)
    }

    /**
     * Draws only the avatar of the troop (without count or other details).
     */
    fun drawAvatar() {
        if (troopInfo != null) {
            // Add troop images to the group
            for (troopImage in troopImages) {
                val scaleFactor = 1/8f*slotSize/64f
                troopImage.setScale(-scaleFactor, scaleFactor) // Adjust scaling
                troopImage.align = Align.center

                // TODO: create Offset function and commit to libGDX for Image class. moveBy is very inconvenient, because shifts twice if function is called twice
                troopImage.moveBy(troopImage. width * scaleFactor, 0f)

                troopImage.touchable = Touchable.disabled // Images should not be interactive
                troopImage.name = "troopImage" // Assign a name for easy reference
                troopGroup.findActor<Image>("troopImage")?.remove() // Remove old image if it exists
                troopGroup.addActor(troopImage)
            }
        }
    }

    private fun setupClickListener() {
        troopGroup.touchable = Touchable.enabled
        troopGroup.addListener(object : ClickListener(Input.Buttons.LEFT) {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (tapCount == 2 && troopInfo != null) {
                    UnitInfoPopup(armyView.getScreen(), troopInfo).open(force = true)
                    return
                }
                val isShiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                        Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                armyView.onTroopClicked(this@TroopArmyView, isShiftPressed)
            }
        })
        troopGroup.addListener(object : com.badlogic.gdx.scenes.scene2d.InputListener() {
            private var panel: com.unciv.ui.battlescreen.BattleTroopInfoPanel? = null

            private fun hide() {
                panel?.remove()
                panel = null
            }

            override fun touchDown(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ): Boolean {
                if (button != Input.Buttons.RIGHT || troopInfo == null) return false
                hide()
                panel = com.unciv.ui.battlescreen.BattleTroopInfoPanel(troopInfo, null).also {
                    it.showNextTo(armyView.getScreen().stage, this@TroopArmyView)
                }
                event?.stop()
                return true
            }

            override fun touchUp(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                button: Int
            ) {
                if (button == Input.Buttons.RIGHT) hide()
            }

            override fun exit(
                event: InputEvent?,
                x: Float,
                y: Float,
                pointer: Int,
                toActor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                if (toActor == null || !toActor.isDescendantOf(troopGroup)) hide()
            }
        })
    }


    /**
     * Toggles the troop's selection state between selected and deselected.
     */
    private fun toggleSelection() {
        if (selected) {
            deselect()
        } else {
            select()
        }
    }

    /**
     * Removes the troop view, typically when the troop is dismissed or removed from the army.
     */
    fun dismiss() {
        // TODO: need to rewrite. do not remove, but substitute into empty slot
        //logVerbose("Troop dismissed: ${troopInfo.unitName}")
        //troopGroup.remove()
    }

    /**
     * Logs a message to the console if verbose logging is enabled.
     */
    private fun logVerbose(message: String) {
        if (isVerbose) {
            println(message)
        }
    }
}
