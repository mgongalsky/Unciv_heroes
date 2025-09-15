package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.toLabel

/**
 * Popup window that displays detailed information about a unit in Heroes-style format.
 * Shows unit image, amount, stats, and unique abilities.
 *
 * @param screen The screen from which the popup is opened.
 * @param sourceTroopView The TroopArmyView to copy visuals from (avatar) and read data.
 */
class UnitInfoPopup(
    screen: BaseScreen,
    private val sourceTroopView: TroopArmyView
) : Popup(screen) {

    init {
        // Set the size of the popup window (make it a bit larger for bigger avatar)
        setSize(650f, 380f)

        createUnitDisplay()
    }

    private fun createUnitDisplay() {
        val troopInfo = sourceTroopView.troopInfo ?: return close()

        // Get base unit information
        val baseUnit = troopInfo.baseUnit
        val unitName = troopInfo.unitName
        val amount = troopInfo.amount

        // Create main horizontal layout table
        val mainTable = Table()
        mainTable.defaults().pad(10f)

        // Left side: Avatar and basic info
        val leftSide = Table()
        leftSide.defaults().pad(5f)
        
        // Unit avatar (larger and more prominent, non-interactive)
        val unitAvatarView = TroopArmyView(sourceTroopView, true, 140f, false)
        unitAvatarView.deselect()
        leftSide.add(unitAvatarView).size(140f).padBottom(10f).row()
        
        // Unit name (larger and more prominent)
        val nameLabel = unitName.toLabel(fontSize = 22)
        nameLabel.setAlignment(Align.center)
        leftSide.add(nameLabel).padBottom(8f).row()
        
        // Amount (more prominent)
        val amountLabel = "Amount: $amount".toLabel(fontSize = 18)
        amountLabel.setAlignment(Align.center)
        leftSide.add(amountLabel).padBottom(5f).row()
        
        // Unit type
        val typeLabel = "Type: ${baseUnit.unitType}".toLabel(fontSize = 15)
        typeLabel.setAlignment(Align.center)
        leftSide.add(typeLabel).row()
        
        // Right side: Stats and abilities
        val rightSide = Table()
        rightSide.defaults().pad(3f)
        
        // Stats header
        val statsHeader = "Unit Statistics".toLabel(fontSize = 18)
        statsHeader.setAlignment(Align.center)
        rightSide.add(statsHeader).colspan(2).padBottom(10f).row()
        
        // Combat strength
        if (baseUnit.strength > 0) {
            rightSide.add("Combat Strength:".toLabel()).left().padRight(10f)
            rightSide.add("${baseUnit.strength}${Fonts.strength}".toLabel()).left().row()
        }
        
        // Ranged strength and range
        if (baseUnit.rangedStrength > 0) {
            rightSide.add("Ranged Strength:".toLabel()).left().padRight(10f)
            rightSide.add("${baseUnit.rangedStrength}${Fonts.rangedStrength}".toLabel()).left().row()
            
            rightSide.add("Range:".toLabel()).left().padRight(10f)
            rightSide.add("${baseUnit.range}${Fonts.range}".toLabel()).left().row()
        }
        
        // Movement
        rightSide.add("Movement:".toLabel()).left().padRight(10f)
        rightSide.add("${baseUnit.movement}${Fonts.movement}".toLabel()).left().row()
        
        // Health
        rightSide.add("Health:".toLabel()).left().padRight(10f)
        rightSide.add("${baseUnit.health}".toLabel()).left().row()
        
        // Production cost
        if (baseUnit.cost > 0) {
            rightSide.add("Cost:".toLabel()).left().padRight(10f)
            rightSide.add("${baseUnit.cost}${Fonts.production}".toLabel()).left().row()
        }
        
        // Special abilities (compact version)
        if (baseUnit.uniques.isNotEmpty()) {
            rightSide.add().row() // Small spacer
            rightSide.add("Special Abilities:".toLabel(fontSize = 16)).colspan(2).left().padTop(10f).row()
            
            var abilityCount = 0
            for (unique in baseUnit.uniqueObjects) {
                if (!unique.hasFlag(com.unciv.models.ruleset.unique.UniqueFlag.HiddenToUsers) && abilityCount < 3) {
                    val abilityLabel = "• ${unique.text}".toLabel(fontSize = 12)
                    abilityLabel.wrap = true
                    rightSide.add(abilityLabel).colspan(2).left().fillX().row()
                    abilityCount++
                }
            }
        }
        
        // Add left and right sides to main table with better spacing
        mainTable.add(leftSide).top().padRight(30f).minWidth(200f)
        mainTable.add(rightSide).top().expandX().fillX().minWidth(350f)
        
        // Add main table to popup with padding
        add(mainTable).expand().fill().pad(20f).row()
        
        // Close button
        addCloseButton(
            text = "OK",
            style = BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        ).apply {
            actor.label.setFontScale(0.6f)
            actor.label.setAlignment(Align.center)
            actor.pad(8f, 20f, 8f, 20f)
        }
    }
}
