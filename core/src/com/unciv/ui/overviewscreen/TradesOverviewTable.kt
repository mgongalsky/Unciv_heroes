package com.unciv.ui.overviewscreen

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.trade.Trade
import com.unciv.logic.trade.TradeOffersList
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.toLabel

class TradesOverviewTab(
    viewingPlayer: CivilizationInfo,
    overviewScreen: EmpireOverviewScreen
) : EmpireOverviewTab(viewingPlayer, overviewScreen) {

    init {
        defaults().pad(10f)
        val diplomacies = viewingPlayer.diplomacy.values.filter { it.trades.isNotEmpty() }
            .sortedWith { diplomacyManager1, diplomacyManager2 ->
                val d1OffersFromFirstTrade = diplomacyManager1.trades.first().ourOffers
                val d2OffersFromFirstTrade = diplomacyManager2.trades.first().ourOffers
                val d1MaxDuration = if (d1OffersFromFirstTrade.isEmpty()) 0 else d1OffersFromFirstTrade.maxByOrNull { it.duration }!!.duration
                val d2MaxDuration = if (d2OffersFromFirstTrade.isEmpty()) 0 else d2OffersFromFirstTrade.maxByOrNull { it.duration }!!.duration
                when {
                    d1MaxDuration > d2MaxDuration -> 1
                    d1MaxDuration == d2MaxDuration -> 0
                    else -> -1
                }
            }
        for (diplomacy in diplomacies) {
            for (trade in diplomacy.trades)
                add(createTradeTable(trade, diplomacy.otherCiv())).row()
        }
    }

    private fun createTradeTable(trade: Trade, otherCiv: CivilizationInfo): Table {
        val generalTable = Table()
        val royal = overviewScreen.game.settings.skin == "Minimal"
        if (!royal) {
            generalTable.add(
                createOffersTable(
                    viewingPlayer,
                    trade.ourOffers,
                    trade.theirOffers.size
                )
            )
                .minWidth(overviewScreen.stage.width / 4).fillY()
            generalTable.add(createOffersTable(otherCiv, trade.theirOffers, trade.ourOffers.size))
                .minWidth(overviewScreen.stage.width / 4).fillY()
            return generalTable
        }
        val stacked = overviewScreen.stage.width < 700f
        val availableWidth = (overviewScreen.stage.width - 60f).coerceAtLeast(1f)
        val sideWidth = if (stacked) availableWidth else availableWidth / 2f
        generalTable.add(createOffersTable(viewingPlayer, trade.ourOffers, trade.theirOffers.size))
            .width(sideWidth).fillY()
        if (stacked) generalTable.row()
        generalTable.add(createOffersTable(otherCiv, trade.theirOffers, trade.ourOffers.size))
            .width(sideWidth).fillY()
        return generalTable
    }

    private fun createOffersTable(
        civ: CivilizationInfo,
        offersList: TradeOffersList,
        numberOfOtherSidesOffers: Int
    ): Table {
        val table = Table()
        val royal = overviewScreen.game.settings.skin == "Minimal"
        table.defaults().pad(10f)
        table.background = BaseScreen.skinStrings.getUiBackground(
            "OverviewScreen/TradesOverviewTab/OffersTable",
            tintColor = if (royal) com.badlogic.gdx.graphics.Color.valueOf("EADFBD") else civ.nation.getOuterColor()
        )
        val availableWidth = (overviewScreen.stage.width - 60f).coerceAtLeast(1f)
        val sideWidth =
                if (overviewScreen.stage.width < 700f) availableWidth else availableWidth / 2f
        val textWidth = (sideWidth - table.padLeft - table.padRight - 20f).coerceAtLeast(1f)
        val nameLabel = civ.civName.toLabel(civ.nation.getInnerColor())
        if (royal) {
            val heading = Table().apply {
                background = BaseScreen.skinStrings.getUiBackground(
                    "OverviewScreen/TradesOverviewTab/Nation",
                    tintColor = civ.nation.getOuterColor()
                )
            }
            nameLabel.setWrap(true)
            nameLabel.setAlignment(com.badlogic.gdx.utils.Align.center)
            heading.add(nameLabel)
                .width((textWidth - heading.padLeft - heading.padRight).coerceAtLeast(1f))
            table.add(heading).width(textWidth).row()
        } else table.add(nameLabel).row()
        table.addSeparator(color = if (royal) com.badlogic.gdx.graphics.Color.valueOf("A18B5A") else com.badlogic.gdx.graphics.Color.WHITE)
        for (offer in offersList) {
            var offerText = offer.getOfferText()
            if (!offerText.contains("\n")) offerText += "\n"
            val label = offerText.toLabel(
                if (royal) com.badlogic.gdx.graphics.Color.valueOf("453925") else civ.nation.getInnerColor()
            )
            val cell = table.add(label)
            if (royal) {
                label.setWrap(true)
                cell.width(textWidth)
            }
            cell.row()
        }
        // В двухколоночной раскладке сохраняем одинаковое число строк сторон сделки.
        if (!royal || overviewScreen.stage.width >= 700f)
            for (i in 1..numberOfOtherSidesOffers - offersList.size)
                table.add("\n".toLabel()).row()
        return table
    }
}
