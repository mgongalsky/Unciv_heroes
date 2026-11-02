package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.event.EventBus
import com.unciv.logic.trade.Trade
import com.unciv.logic.trade.TradeOffer
import com.unciv.logic.trade.TradeRequest
import com.unciv.logic.trade.TradeType
import com.unciv.ui.UncivStage
import com.unciv.ui.popup.Popup
import com.unciv.ui.trade.LeaderIntroTable
import com.unciv.ui.worldscreen.TradePopup
import com.unciv.ui.worldscreen.WorldScreen

/** Проверяет представление временного запроса, не выполняя торговых действий. */
internal object TradePopupGoldenChecks {
    private fun descendants(actor: Actor): Sequence<Actor> = sequence {
        yield(actor)
        if (actor is Group)
            for (child in actor.children.toList()) yieldAll(descendants(child))
    }

    fun verify(screen: WorldScreen) {
        check(screen.game.settings.skin == "Minimal")
        val stage = screen.stage as UncivStage
        val viewport = stage.viewport
        val visibleArea = Rectangle(stage.lastKnownVisibleArea)
        val world = screen.gameInfo
        val civ = screen.viewingCiv
        val requests = civ.tradeRequests.toList()
        val gold = civ.gold
        val notifications = civ.notifications.toList()
        val shouldUpdate = screen.shouldUpdate
        val city = civ.cities.first()
        val cityName = city.name
        val other = CivilizationInfo().apply {
            civName = world.ruleSet.nations.keys.first { name ->
                world.civilizations.none { it.civName == name }
            }
            nation = world.ruleSet.nations.getValue(civName)
            gameInfo = world
        }
        val trade = Trade().apply {
            ourOffers.add(TradeOffer(city.id, TradeType.City, duration = -1))
            theirOffers.add(TradeOffer("Gold", TradeType.Gold, 456, duration = -1))
        }
        val request = TradeRequest(other.civName, trade)
        val offersBefore = trade.ourOffers.map { it.copy() } to trade.theirOffers.map { it.copy() }
        world.civilizations.add(other)
        try {
            city.name = "The ancient royal city beyond the northern mountains and distant river valleys ".repeat(5)
            civ.tradeRequests.clear()
            civ.tradeRequests.add(request)
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                val popup = TradePopup(screen)
                var thanks: TradePopup.TradeThanksPopup? = null
                try {
                    popup.open(force = true)
                    fun layout(view: Popup): ScrollPane {
                        view.validate()
                        val scroll = view.children.filterIsInstance<ScrollPane>().single()
                        scroll.validate()
                        view.innerTable.validate()
                        stage.draw()
                        val origin = scroll.localToStageCoordinates(Vector2())
                        check(origin.x >= -1f && origin.y >= -1f &&
                            origin.x + scroll.width <= width + 1f &&
                            origin.y + scroll.height <= 481f) {
                            "Trade panel outside ${width}x480: $origin ${scroll.width}x${scroll.height}"
                        }
                        check(scroll.maxX <= 1f) { "Trade panel horizontal overflow at $width: ${scroll.maxX}" }
                        return scroll
                    }
                    val scroll = layout(popup)
                    val theirs = checkNotNull(popup.findActor<Table>("tradeTheirOffers"))
                    val ours = checkNotNull(popup.findActor<Table>("tradeOurOffers"))
                    if (width == 360f) {
                        check(theirs.y >= ours.top - 1f) { "Narrow trade sides are not stacked" }
                        check(scroll.maxY > 0f) { "Long trade did not exercise scrolling" }
                    } else check(theirs.right <= ours.x + 1f) { "Trade columns overlap" }
                    val cityLabel = descendants(ours).filterIsInstance<Label>()
                        .single { it.text.toString().contains(city.name.trim()) }
                    check(cityLabel.wrap && cityLabel.glyphLayout.runs.size > 1)
                    check(cityLabel.height + 1f >= cityLabel.prefHeight)
                    val buttons = popup.innerTable.children.filterIsInstance<TextButton>()
                    check(buttons.size == 3)
                    fun reach(button: TextButton, pane: ScrollPane) {
                        val local = button.localToAscendantCoordinates(pane.actor, Vector2())
                        pane.scrollTo(local.x, local.y, button.width, button.height)
                        pane.updateVisualScroll()
                        stage.draw()
                        check(button.label.height + 1f >= button.label.prefHeight)
                        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
                        val hit = stage.hit(point.x, point.y, true)
                        check(hit === button || hit?.isDescendantOf(button) == true) {
                            "Trade action inaccessible at $width: ${button.text}"
                        }
                    }
                    for (button in buttons) reach(button, scroll)
                    check(civ.tradeRequests.toList() == listOf(request))
                    check(trade.ourOffers.map { it.copy() } == offersBefore.first)
                    check(trade.theirOffers.map { it.copy() } == offersBefore.second)
                    val leader = popup.innerTable.children.filterIsInstance<LeaderIntroTable>().single()
                    // Удаление представления не вызывает TradePopup.close, удаляющий запрос.
                    popup.events.stopReceiving()
                    popup.remove()
                    val thankYou = TradePopup.TradeThanksPopup(leader, screen)
                    thanks = thankYou
                    val thanksScroll = layout(thankYou)
                    val close = thankYou.innerTable.children.filterIsInstance<TextButton>().single()
                    var closed = 0
                    thankYou.closeListeners.add { closed++ }
                    reach(close, thanksScroll)
                    val point = close.localToStageCoordinates(Vector2(close.width / 2f, close.height / 2f))
                    stage.stageToScreenCoordinates(point)
                    stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                    check(thankYou.parent == null && closed == 1)
                    check(civ.tradeRequests.toList() == listOf(request))
                    check(civ.gold == gold && city.civInfo === civ)
                    check(civ.notifications.toList() == notifications && other.notifications.isEmpty())
                } finally {
                    popup.events.stopReceiving()
                    popup.remove()
                    thanks?.let { if (it.parent != null) it.close() }
                }
            }
        } finally {
            civ.tradeRequests.clear()
            civ.tradeRequests.addAll(requests)
            city.name = cityName
            world.civilizations.remove(other)
            screen.shouldUpdate = shouldUpdate
            stage.viewport = viewport
            viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(visibleArea))
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }
}
