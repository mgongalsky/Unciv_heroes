package com.unciv.ui.newgamescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.models.ruleset.Nation
import com.unciv.models.ruleset.Ruleset
import com.unciv.ui.civilopedia.FormattedLine.IconDisplay
import com.unciv.ui.civilopedia.MarkupRenderer
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.WrappableLabel
import com.unciv.ui.utils.extensions.pad

class NationTable(val nation: Nation, width: Float, minHeight: Float, ruleset: Ruleset? = null) :
    Table(BaseScreen.skin) {
    val innerTable = Table()

    init {
        val royal = com.unciv.UncivGame.Current.settings.skin == "Minimal"
        val innerColor = nation.getInnerColor()
        val outerColor = nation.getOuterColor()
        val textBackgroundColor = if (royal) Color.valueOf("eadfbd") else Color(0x002042ff)
        val borderWidth = 5f
        val totalPadding = 10f + 4 * borderWidth
        val internalWidth = if (royal) (width - 20f).coerceAtLeast(1f) else width - totalPadding

        val titleTable = Table()
        titleTable.background = BaseScreen.skinStrings.getUiBackground(
            "NewGameScreen/NationTable/Title", tintColor = outerColor
        )
        val nationIndicator: Actor =
            if (nation.name == Constants.random) ImageGetter.getRandomNationIndicator(50f)
            else ImageGetter.getNationIndicator(nation, 50f)
        val stackedTitle = royal && internalWidth < 260f
        titleTable.add(nationIndicator).pad(10f).padLeft(if (stackedTitle) 10f else 0f)
        if (stackedTitle) titleTable.row()

        val titleText =
            if (ruleset == null || nation.name == Constants.random || nation.name == Constants.spectator)
                nation.name else nation.getLeaderDisplayName()
        val leaderDisplayNameMaxWidth = if (royal)
            (internalWidth - if (stackedTitle) 20f else 70f).coerceAtLeast(1f)
        else internalWidth - 70f
        val leaderDisplayLabel = WrappableLabel(
            titleText,
            leaderDisplayNameMaxWidth,
            innerColor,
            Constants.headingFontSize
        )
        if (royal || leaderDisplayLabel.prefWidth > leaderDisplayNameMaxWidth - 2f) {
            leaderDisplayLabel.wrap = true
            titleTable.add(leaderDisplayLabel).width(leaderDisplayNameMaxWidth)
                .padBottom(if (stackedTitle) 10f else 0f)
        } else {
            titleTable.add(leaderDisplayLabel).align(Align.center).pad(10f, 0f)
        }

        innerTable.add(titleTable).growX().fillY().row()

        if (ruleset != null) {
            titleTable.padBottom(borderWidth)
            innerTable.background = BaseScreen.skinStrings.getUiBackground(
                "NewGameScreen/NationTable/RightInnerTable",
                tintColor = textBackgroundColor
            )
            val lines = nation.getCivilopediaTextLines(ruleset).filter { it.header != 3 }
            val description = if (royal) MarkupRenderer.renderWithTextColor(
                lines, Color.valueOf("453925"),
                (internalWidth - 25f).coerceAtLeast(1f), iconDisplay = IconDisplay.NoLink
            ) else MarkupRenderer.render(lines, internalWidth, iconDisplay = IconDisplay.NoLink)
            innerTable.add(description).pad(10f)
            val borderTable = Table()
            borderTable.background = BaseScreen.skinStrings.getUiBackground(
                "NewGameScreen/NationTable/BorderTable", tintColor = outerColor
            )
            borderTable.add(innerTable).pad(borderWidth).grow()
            add(borderTable).pad(borderWidth)
                .width(if (royal) (width - 10f).coerceAtLeast(1f) else width)
                .minHeight((minHeight - totalPadding).coerceAtLeast(0f))
        } else {
            add(innerTable).width(width).minHeight((minHeight - totalPadding).coerceAtLeast(0f))
        }

        touchable = Touchable.enabled
        background = BaseScreen.skinStrings.getUiBackground(
            "NewGameScreen/NationTable/Background", tintColor = innerColor
        )
    }
}

/*
Layout if ruleset != null:

       *Widgets*
         Text colour                     Background Colour
           Align                           Padding

+----- *NationTable* ----------------------------------------+
|                                        getInnerColor       |
| +---- *borderTable* -------------------------------------+ |
| |                                      getOuterColor     | |
| | +--- *innerTable* -----------------------------------+ | |
| | | +-- *titleTable* --------------------------------+ | | |
| | | |   getInnerColor                  getOuterColor | | | |
| | | |     *nationIndicator*   *leaderDisplayLabel*   | | | |
| | | |   center or left/wrap            0: all sides  | | | |
| | | +------------------------------------------------+ | | |
| | |     White                          Dark-blue       | | |
| | |   MarkupRenderer.render(getCivilopediaTextLines)   | | |
| | |     left/wrap                      10: all sides   | | |
| | |                                                    | | |
| | +----------------------------------------------------+ | |
| +--------------------------------------------------------+ |
+------------------------------------------------------------+

*/
