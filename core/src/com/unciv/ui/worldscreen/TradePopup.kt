package com.unciv.ui.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.NotificationIcon
import com.unciv.logic.trade.TradeLogic
import com.unciv.logic.trade.TradeOffer
import com.unciv.logic.trade.TradeType
import com.unciv.models.translations.tr
import com.unciv.ui.popup.Popup
import com.unciv.ui.trade.DiplomacyScreen
import com.unciv.ui.trade.LeaderIntroTable
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.pad
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.max
import kotlin.math.min
import com.unciv.ui.utils.AutoScrollPane as ScrollPane

/* TODO:
    different Notification wording for peace treaties?
    Let Notification jump to Diplomacy.trade with empty offers (accepted)
        or a clone of the rejected offer (denied) ...?
*/

class TradePopup(worldScreen: WorldScreen) : Popup(worldScreen) {
    val viewingCiv = worldScreen.viewingCiv
    val tradeRequest = viewingCiv.tradeRequests.first()

    init {
        val contentWidth = configurePanel(this, worldScreen)
        val royal = contentWidth > 0f
        val requestingCiv = worldScreen.gameInfo.getCivilization(tradeRequest.requestingCiv)
        val nation = requestingCiv.nation
        val trade = tradeRequest.trade
        val ourResources = viewingCiv.getCivResourcesByName()
        val leaderIntroTable = LeaderIntroTable(requestingCiv, parchmentWidth = contentWidth)
        add(leaderIntroTable).apply { if (royal) width(contentWidth) }
        addSeparator()

        fun getOfferText(offer: TradeOffer): String {
            var tradeText = offer.getOfferText()
            if (offer.type == TradeType.Luxury_Resource || offer.type == TradeType.Strategic_Resource)
                tradeText += "\n" + "Owned by you: [${ourResources[offer.name]}]".tr()
            return tradeText
        }

        if (royal) {
            val stacked = contentWidth < 500f
            val columnWidth = if (stacked) contentWidth else (contentWidth - 20f) / 2f
            fun side(title: String, offers: Iterable<TradeOffer>, actorName: String): Table =
                Table().apply {
                    name = actorName
                    defaults().width(columnWidth).left().padBottom(10f)
                    add(
                        title.toLabel(fontColor = Color.valueOf("453925"))
                            .apply { wrap = true }).row()
                    for (offer in offers) {
                        add(getOfferText(offer).toLabel(fontColor = Color.valueOf("453925")).apply {
                            wrap = true
                        }).row()
                    }
                }

            val offers = Table().apply { name = "tradeOffers" }
            val theirs =
                side("[${nation.name}]'s trade offer", trade.theirOffers, "tradeTheirOffers")
            val ours = side("Our trade offer", trade.ourOffers, "tradeOurOffers")
            if (stacked) {
                offers.add(theirs).width(columnWidth).top().padBottom(12f).row()
                offers.add(ours).width(columnWidth).top().row()
            } else {
                offers.add(theirs).width(columnWidth).top().padRight(20f)
                offers.add(ours).width(columnWidth).top().row()
            }
            add(offers).width(contentWidth).row()
        } else {
            val tradeOffersTable = Table().apply { defaults().pad(10f) }
            tradeOffersTable.add("[${nation.name}]'s trade offer".toLabel())
            tradeOffersTable.add().pad(0f, 15f)
            tradeOffersTable.add("Our trade offer".toLabel())
            tradeOffersTable.row()
            for (i in 0..max(trade.theirOffers.lastIndex, trade.ourOffers.lastIndex)) {
                if (trade.theirOffers.lastIndex < i) tradeOffersTable.add()
                else tradeOffersTable.add(getOfferText(trade.theirOffers[i]).toLabel())
                tradeOffersTable.add()
                if (trade.ourOffers.lastIndex < i) tradeOffersTable.add()
                else tradeOffersTable.add(getOfferText(trade.ourOffers[i]).toLabel())
                tradeOffersTable.row()
            }
            tradeOffersTable.pack()
            val scrollHeight = min(tradeOffersTable.height, worldScreen.stage.height / 2)
            add(ScrollPane(tradeOffersTable)).height(scrollHeight).row()
        }

        addSeparator(Color.DARK_GRAY, height = 1f)
        if (royal) {
            add(nation.tradeRequest.toLabel(fontColor = Color.valueOf("453925")).apply {
                wrap = true
                setAlignment(com.badlogic.gdx.utils.Align.center)
            }).width(contentWidth).padTop(15f).padBottom(15f).row()
        } else addGoodSizedLabel(nation.tradeRequest).pad(15f).row()

        val accept = addButton("Sounds good!", 'y') {
            val tradeLogic = TradeLogic(viewingCiv, requestingCiv)
            tradeLogic.currentTrade.set(trade)
            tradeLogic.acceptTrade()
            close()
            TradeThanksPopup(leaderIntroTable, worldScreen)
            requestingCiv.addNotification(
                "[${viewingCiv.civName}] has accepted your trade request",
                viewingCiv.civName,
                NotificationIcon.Trade
            )
        }
        configureButton(accept, contentWidth)
        accept.row()
        val decline = addButton("Not this time.", 'n') {
            tradeRequest.decline(viewingCiv)
            close()
            requestingCiv.addNotification(
                "[${viewingCiv.civName}] has denied your trade request",
                viewingCiv.civName,
                NotificationIcon.Trade
            )
            worldScreen.shouldUpdate = true
        }
        configureButton(decline, contentWidth)
        decline.row()
        val negotiate = addButton("How about something else...", 'e') {
            close()
            worldScreen.game.pushScreen(DiplomacyScreen(viewingCiv, requestingCiv, trade))
            worldScreen.shouldUpdate = true
        }
        configureButton(negotiate, contentWidth)
        negotiate.row()
    }

    override fun close() {
        viewingCiv.tradeRequests.remove(tradeRequest)
        super.close()
    }

    class TradeThanksPopup(leaderIntroTable: LeaderIntroTable, worldScreen: WorldScreen) :
        Popup(worldScreen) {
        init {
            val contentWidth = configurePanel(this, worldScreen)
            add(leaderIntroTable).apply { if (contentWidth > 0f) width(contentWidth) }
            addSeparator().padBottom(15f)
            if (contentWidth > 0f) {
                add("Excellent!".toLabel(fontColor = Color.valueOf("453925")).apply {
                    wrap = true
                    setAlignment(com.badlogic.gdx.utils.Align.center)
                }).width(contentWidth).row()
            } else addGoodSizedLabel("Excellent!").row()
            val close = addCloseButton("Farewell.", KeyCharAndCode.SPACE) {
                worldScreen.shouldUpdate = true
            }
            configureButton(close, contentWidth)
            open()
        }
    }

    companion object {
        /** Нулевая ширина сохраняет оформление пользовательского скина. */
        private fun configurePanel(popup: Popup, worldScreen: WorldScreen): Float {
            if (worldScreen.game.settings.skin != "Minimal") return 0f
            popup.innerTable.background =
                com.unciv.ui.utils.RoyalPanelDrawable(com.unciv.ui.utils.BaseScreen.skin)
            popup.innerTable.pad(52f).padTop(58f)
            val scroll =
                popup.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()
                    .single()
            val style = scroll.style
            val chrome = popup.innerTable.padLeft + popup.innerTable.padRight + 10f +
                    maxOf(style.vScroll?.minWidth ?: 0f, style.vScrollKnob?.minWidth ?: 0f) +
                    (style.background?.leftWidth ?: 0f) + (style.background?.rightWidth ?: 0f)
            val contentWidth =
                minOf(700f, popup.stageToShowOn.width - 24f - chrome).coerceAtLeast(1f)
            popup.getCell(scroll).width(contentWidth + chrome)
                .maxHeight((popup.stageToShowOn.height - 24f).coerceAtLeast(1f))
            return contentWidth
        }

        private fun configureButton(
            cell: com.badlogic.gdx.scenes.scene2d.ui.Cell<com.badlogic.gdx.scenes.scene2d.ui.TextButton>,
            contentWidth: Float
        ) {
            if (contentWidth <= 0f) return
            val button = cell.actor
            button.label.wrap = true
            button.labelCell.width(
                (contentWidth - button.padLeft - button.padRight - 20f).coerceAtLeast(
                    1f
                )
            ).pad(10f)
            cell.width(contentWidth).minHeight(48f).fillX()
        }
    }
}
