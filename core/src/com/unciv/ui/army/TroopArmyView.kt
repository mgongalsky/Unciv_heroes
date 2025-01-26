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
import com.unciv.ui.utils.TextureUtils

/**
 * Represents the visual representation of a single troop in an army.
 * Includes functionality for selection/deselection and updating the display accordingly.
 */
class TroopArmyView(
    internal val troopInfo: TroopInfo?, // Null indicates an empty slot
    private val armyView: ArmyView,
    private val avatarOnly: Boolean = false, // avatar-only mode
    private val slotSize: Float = 64f // Размер слота, по умолчанию 64x64
) : Group() {
    private val troopGroup = Group() // A group to contain all troop-related visuals
    private lateinit var troopImages: ArrayList<Image> // Images representing the troop's layers
    private var selected: Boolean = false // Indicates whether the troop is selected

    // Preload textures for selection states to avoid creating them repeatedly
    private val defaultTexture = TextureUtils.createMonochromaticTexture(slotSize.toInt(), slotSize.toInt(), Color.BROWN)
    private val selectedTexture = TextureUtils.createMonochromaticTexture(slotSize.toInt(), slotSize.toInt(), Color.GOLD)


    companion object {
        var isVerbose = false // Flag to enable or disable verbose output
    }

    /**
     * Copy constructor to create a new TroopArmyView based on an existing one.
     * Copies `troopInfo` and initializes `armyView` for standalone usage.
     */
    constructor(original: TroopArmyView, isAvatarOnly: Boolean) : this(
        troopInfo = original.troopInfo?.copy(), // Copy troopInfo to ensure it's not the same reference
        armyView = original.armyView, // ArmyView reference remains the same, modify if needed for detached usage
        avatarOnly = isAvatarOnly
            ) {


    }

    init {
        // Load troop images based on the troop's unit name
        if(troopInfo != null) {
            val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
            troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)
        }
        // Initial drawing of the troop view
        draw()
        setupClickListener()
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

    /**
     * Updates the view to reflect the current selection state by changing the texture within the drawable.
     */
    private fun updateViewForSelection() {
        val newTexture = if (selected) selectedTexture else defaultTexture

        // Get the background image by its name
        val backgroundImage = troopGroup.findActor<Image>("backgroundImage")

        if (backgroundImage != null) {
            logVerbose("Updating background texture ")

            // Ensure the drawable is of type TextureRegionDrawable
            val drawable = backgroundImage.drawable
            if (drawable is TextureRegionDrawable) {
                drawable.region.texture = newTexture
                logVerbose("Texture updated successfully")
            } else {
                logVerbose("Drawable is not of type TextureRegionDrawable")
            }

            // Notify the parent that the visuals need a refresh
            backgroundImage.invalidateHierarchy()
        } else {
            logVerbose("Background image not found")
        }
    }

    /**
     * Draws the troop's visual components, including the interactive background.
     */
    fun draw() {
        // Create a background image and assign it a name for easy reference
        val backgroundImage = Image(defaultTexture).apply {
            name = "backgroundImage"
            touchable = Touchable.enabled // Enable interactivity for the background
            align = Align.center
        }
        troopGroup.addActor(backgroundImage)

        drawAvatar()

        if(!avatarOnly) {
            if (troopInfo != null) {
                // Create and add a label showing the troop's current amount
                val amountText = Label(troopInfo.amount.toString(), BaseScreen.skin).apply {
                    setAlignment(Align.right) // Align text to the right
                    scaleBy(0.5f*slotSize/64f)
                    moveBy(backgroundImage.width * 0.95f, 0.5f)
                    setPosition(x, y, Align.bottomRight)

                    name = "amountLabel"
                    touchable = Touchable.disabled // Label should not be interactive
                }

                troopGroup.findActor<Label>("amountLabel")
                    ?.remove() // Remove old label if it exists
                troopGroup.addActor(amountText)
            }
        }
        // Add the troop group to this view
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
    /**
     * Sets up a click listener to toggle the selection state when the troop is clicked.
     */
    private fun setupClickListener() {
        troopGroup.touchable = Touchable.enabled // Allow interactions with the group
        troopGroup.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (troopInfo != null)
                    logVerbose("Troop clicked: ${troopInfo.unitName}")
                else
                    logVerbose("Empty slot clicked")

                // Check if Shift key is pressed
                val isShiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                // Pass the shift state to the ArmyView
                armyView.onTroopClicked(this@TroopArmyView, isShiftPressed)
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
