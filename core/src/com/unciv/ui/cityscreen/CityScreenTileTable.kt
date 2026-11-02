package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.logic.map.TileInfo
import com.unciv.models.UncivSound
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.models.translations.tr
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.civilopedia.FormattedLine.IconDisplay
import com.unciv.ui.civilopedia.MarkupRenderer
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.ConfirmPopup
import com.unciv.ui.popup.closeAllPopups
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.UncivTooltip.Companion.addTooltip
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.isEnabled
import com.unciv.ui.utils.extensions.keyShortcuts
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import kotlin.math.roundToInt

class CityScreenTileTable(private val cityScreen: CityScreen): Table() {
    private val innerTable = Table()
    val city = cityScreen.city

    init {
        innerTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityScreenTileTable/InnerTable",
            tintColor = BaseScreen.skinStrings.skinConfig.baseColor.darken(0.5f)
        )
        add(innerTable).pad(2f).fill()
        background = BaseScreen.skinStrings.getUiBackground("CityScreen/CityScreenTileTable/Background", tintColor = Color.WHITE)
    }

    fun update(selectedTile: TileInfo?) {
        innerTable.clear()
        if (selectedTile == null) {
            isVisible = false
            return
        }
        isVisible = true
        val royal = UncivGame.Current.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 4f).coerceAtLeast(260f)
            .coerceAtMost(cityScreen.stage.width - 10f) - 108f).coerceAtLeast(1f)
        val ink = if (royal) Color.valueOf("453925") else Color.WHITE
        if (royal) {
            background = null
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
        } else innerTable.pad(5f)

        val markup = MarkupRenderer.renderWithTextColor(
            selectedTile.toMarkup(city.civInfo),
            defaultTextColor = if (royal) ink else null,
            labelWidth = if (royal) (contentWidth - 5f).coerceAtLeast(1f) else 0f,
            iconDisplay = IconDisplay.None
        ) {
            UncivGame.Current.pushScreen(CivilopediaScreen(city.getRuleset(), link = it))
        }
        val markupCell = innerTable.add(markup)
        if (royal) markupCell.width(contentWidth)
        markupCell.row()
        innerTable.add(getTileStatsTable(selectedTile.getTileStats(city, city.civInfo))).row()

        fun addText(text: String) {
            val label = text.toLabel(fontColor = ink)
            val cell = innerTable.add(label)
            if (royal) {
                label.setWrap(true)
                cell.width(contentWidth)
            }
            cell.row()
        }

        if (isTilePurchaseShown(selectedTile)) {
            val goldCostOfTile = city.expansion.getGoldCostOfTile(selectedTile)
            val buyTileButton = "Buy for [$goldCostOfTile] gold".toTextButton()
            buyTileButton.onActivation {
                buyTileButton.disable()
                askToBuyTile(selectedTile)
            }
            buyTileButton.keyShortcuts.add('T')
            buyTileButton.isEnabled = isTilePurchaseAllowed(goldCostOfTile)
            buyTileButton.addTooltip('T')
            innerTable.add(buyTileButton).padTop(5f).row()
        }
        if (selectedTile.owningCity != null)
            addText("Owned by [${selectedTile.owningCity!!.name}]")
        if (city.civInfo.cities.filterNot { it == city }.any { it.isWorked(selectedTile) })
            addText("Worked by [${selectedTile.getWorkingCity()!!.name}]")

        if (city.isWorked(selectedTile)) {
            if (selectedTile.isLocked()) {
                val unlockButton = "Unlock".toTextButton()
                unlockButton.onClick {
                    city.lockedTiles.remove(selectedTile.position)
                    update(selectedTile)
                    cityScreen.update()
                }
                if (!cityScreen.canChangeState) unlockButton.disable()
                innerTable.add(unlockButton).padTop(5f).row()
            } else {
                val lockButton = "Lock".toTextButton()
                lockButton.onClick {
                    city.lockedTiles.add(selectedTile.position)
                    update(selectedTile)
                    cityScreen.update()
                }
                if (!cityScreen.canChangeState) lockButton.disable()
                innerTable.add(lockButton).padTop(5f).row()
            }
        }
        if (selectedTile.isCityCenter() && selectedTile.getCity() != city && selectedTile.getCity()!!.civInfo == city.civInfo)
            innerTable.add("Move to city".toTextButton().onClick {
                cityScreen.game.replaceCurrentScreen(CityScreen(selectedTile.getCity()!!))
            })

        if (royal) {
            for (cell in innerTable.cells) {
                val button =
                        cell.actor as? com.badlogic.gdx.scenes.scene2d.ui.TextButton ?: continue
                button.label.setWrap(true)
                cell.width(contentWidth).minHeight(58f).fillX()
            }
            val scroll = tileScroll
            innerTable.pack()
            val scrollStyle = scroll.style
            val barWidth = maxOf(
                scrollStyle.vScroll?.minWidth ?: 0f,
                scrollStyle.vScrollKnob?.minWidth ?: 0f
            )
            val scrollBackground = scrollStyle.background
            val horizontalInsets = (scrollBackground?.leftWidth ?: 0f) +
                    (scrollBackground?.rightWidth ?: 0f)
            val verticalInsets = (scrollBackground?.topHeight ?: 0f) +
                    (scrollBackground?.bottomHeight ?: 0f)
            clearChildren()
            add(scroll).width(innerTable.prefWidth + barWidth + horizontalInsets)
                .height(
                    minOf(
                        innerTable.prefHeight + verticalInsets,
                        ((cityScreen.stage.height - 10f) / 2f).coerceAtLeast(1f)
                    )
                )
            pack()
            scroll.validate()
            scroll.scrollY = 0f
            scroll.updateVisualScroll()
        } else {
            innerTable.pack()
            pack()
        }
    }

    /** Ask whether user wants to buy [selectedTile] for gold.
     *
     * Used from onClick and keyboard dispatch, thus only minimal parameters are passed,
     * and it needs to do all checks and the sound as appropriate.
     */
    fun askToBuyTile(selectedTile: TileInfo) {
        // These checks are redundant for the onClick action, but not for the keyboard binding
        if (!isTilePurchaseShown(selectedTile)) return
        val goldCostOfTile = city.expansion.getGoldCostOfTile(selectedTile)
        if (!isTilePurchaseAllowed(goldCostOfTile)) return

        cityScreen.closeAllPopups()

        val purchasePrompt = "Currently you have [${city.civInfo.gold}] [Gold].".tr() + "\n\n" +
                "Would you like to purchase [Tile] for [$goldCostOfTile] [${Stat.Gold.character}]?".tr()
        ConfirmPopup(
            cityScreen,
            purchasePrompt,
            "Purchase",
            true,
            restoreDefault = { cityScreen.update() }
        ) {
            SoundPlayer.play(UncivSound.Coin)
            city.expansion.buyTile(selectedTile)
            // preselect the next tile on city screen rebuild so bulk buying can go faster
            UncivGame.Current.replaceCurrentScreen(CityScreen(city, initSelectedTile = city.expansion.chooseNewTileToOwn()))
        }.open()
    }

    /** This tests whether the buy button should be _shown_ */
    private fun isTilePurchaseShown(selectedTile: TileInfo) = when {
        selectedTile.getOwner() != null -> false
        selectedTile !in city.tilesInRange -> false
        else -> selectedTile.neighbors.any { it.getCity() == city }
    }
    /** This tests whether the buy button should be _enabled_ */
    private fun isTilePurchaseAllowed(goldCostOfTile: Int) = when {
        city.isPuppet -> false
        !cityScreen.canChangeState -> false
        city.civInfo.gameInfo.gameParameters.godMode -> true
        goldCostOfTile == 0 -> true
        else -> city.civInfo.gold >= goldCostOfTile
    }

    private fun getTileStatsTable(stats: Stats): Table {
        val royal = UncivGame.Current.settings.skin == "Minimal"
        val statsTable = Table()
        statsTable.defaults().pad(2f)
        for ((key, value) in stats) {
            statsTable.add(ImageGetter.getStatIcon(key.name)).size(20f)
            statsTable.add(
                value.roundToInt().toString().toLabel(
                    fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
                )
            ).padRight(5f)
            if (royal) statsTable.row()
        }
        return statsTable
    }

    private val tileScroll by lazy {
        com.unciv.ui.utils.AutoScrollPane(innerTable, BaseScreen.skin).apply {
            name = "cityTileScroll"
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
            setFadeScrollBars(false)
        }
    }
}
