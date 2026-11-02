package com.unciv.app.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.FitViewport
import com.unciv.logic.civilization.AlertType
import com.unciv.logic.civilization.PopupAlert
import com.unciv.logic.event.EventBus
import com.unciv.ui.UncivStage
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.worldscreen.AlertPopup
import com.unciv.ui.worldscreen.WorldScreen

/** Проверяет текстовые оповещения на подготовленном мире без дипломатических действий. */
internal object AlertPopupGoldenChecks {
    fun verify(screen: WorldScreen) {
        check(screen.game.settings.skin == "Minimal")
        val civ = screen.viewingCiv
        val stage = screen.stage as UncivStage
        val viewport = stage.viewport
        val visibleArea = Rectangle(stage.lastKnownVisibleArea)
        val alerts = civ.popupAlerts.toList()
        val gold = civ.gold
        val notifications = civ.notifications.toList()
        val cities = civ.cities.toList()
        val shouldUpdate = screen.shouldUpdate
        val types = listOf(
            AlertType.GoldenAge, AlertType.StartIntro, AlertType.FirstContact,
            AlertType.Defeated, AlertType.CitySettledNearOtherCivDespiteOurPromise
        )
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                for (type in types) {
                    val alert = PopupAlert(type, civ.civName)
                    val pending = PopupAlert(AlertType.GoldenAge, "")
                    civ.popupAlerts.clear()
                    civ.popupAlerts.add(alert)
                    civ.popupAlerts.add(pending)
                    val popup = AlertPopup(screen, alert)
                    var closed = 0
                    popup.closeListeners.add { closed++ }
                    try {
                        popup.open(force = true)
                        check(popup.innerTable.background is RoyalPanelDrawable)
                        val labels = popup.innerTable.children.filterIsInstance<Label>()
                        check(labels.isNotEmpty())
                        for (label in labels) {
                            val effective = label.color.cpy()
                            label.style.fontColor?.let { effective.mul(it) }
                            check(effective == Color.valueOf("453925")) {
                                "Alert $type has unreadable text color: $effective"
                            }
                        }
                        val longLabel = labels.first()
                        longLabel.setText("A long message from the distant royal provinces beyond the mountains. ".repeat(30))
                        val button = popup.innerTable.children.filterIsInstance<TextButton>().single()
                        button.setText("Acknowledge this message from the distant royal provinces")
                        popup.invalidateHierarchy()
                        popup.validate()
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        scroll.validate()
                        popup.innerTable.validate()
                        stage.draw()
                        val origin = scroll.localToStageCoordinates(Vector2())
                        val detail = "$type at ${width}x480: origin=$origin size=${scroll.width}x${scroll.height} maxX=${scroll.maxX} maxY=${scroll.maxY}"
                        check(origin.x >= -1f && origin.y >= -1f &&
                            origin.x + scroll.width <= width + 1f &&
                            origin.y + scroll.height <= 481f) { detail }
                        check(scroll.maxX <= 1f && scroll.maxY > 0f) { detail }
                        check(longLabel.wrap && longLabel.glyphLayout.runs.size > 1) { detail }
                        check(longLabel.height + 1f >= longLabel.prefHeight) { detail }
                        check(button.label.wrap && button.label.height + 1f >= button.label.prefHeight) { detail }
                        val local = button.localToAscendantCoordinates(scroll.actor, Vector2())
                        scroll.scrollTo(local.x, local.y, button.width, button.height)
                        scroll.updateVisualScroll()
                        stage.draw()
                        val point = button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
                        val hit = stage.hit(point.x, point.y, true)
                        check(hit === button || hit?.isDescendantOf(button) == true) {
                            "Alert close button inaccessible: $detail"
                        }
                        check(civ.popupAlerts.toList() == listOf(alert, pending))
                        stage.stageToScreenCoordinates(point)
                        stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        check(popup.parent == null && closed == 1) { "Alert $type did not close once" }
                        check(civ.popupAlerts.toList() == listOf(pending)) {
                            "Closing $type removed another pending alert"
                        }
                        check(screen.shouldUpdate)
                        check(civ.gold == gold && civ.cities.toList() == cities)
                        check(civ.notifications.toList() == notifications)
                    } finally {
                        if (popup.parent != null) popup.close()
                    }
                }
            }
        } finally {
            civ.popupAlerts.clear()
            civ.popupAlerts.addAll(alerts)
            screen.shouldUpdate = shouldUpdate
            stage.viewport = viewport
            viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(visibleArea))
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }

    fun verifyPairedResponses(screen: WorldScreen) {
        check(screen.game.settings.skin == "Minimal")
        val civ = screen.viewingCiv
        val stage = screen.stage as UncivStage
        val viewport = stage.viewport
        val visibleArea = Rectangle(stage.lastKnownVisibleArea)
        val alerts = civ.popupAlerts.toList()
        val shouldUpdate = screen.shouldUpdate
        val gold = civ.gold
        val notifications = civ.notifications.toList()
        val diplomacy = civ.diplomacy.toMap()
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                for (type in listOf(AlertType.WarDeclaration, AlertType.BorderConflict)) {
                    for (responseIndex in 0..1) {
                        val alert = PopupAlert(type, civ.civName)
                        val pending = PopupAlert(AlertType.GoldenAge, "")
                        civ.popupAlerts.clear()
                        civ.popupAlerts.addAll(listOf(alert, pending))
                        val popup = AlertPopup(screen, alert)
                        var closed = 0
                        popup.closeListeners.add { closed++ }
                        try {
                            popup.open(force = true)
                            check(popup.innerTable.background is RoyalPanelDrawable)
                            val responses = checkNotNull(
                                popup.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("alertResponses")
                            )
                            val buttons = responses.children.filterIsInstance<TextButton>()
                            check(buttons.size == 2)
                            val originalCaptions = buttons.map { it.text.toString() }
                            for ((index, button) in buttons.withIndex()) {
                                button.setText("Response $index to the message from the distant royal provinces beyond the mountains")
                            }
                            val labels = popup.innerTable.children.filterIsInstance<Label>()
                            check(labels.isNotEmpty())
                            labels.first().setText(
                                "A long diplomatic message from the distant provinces. ".repeat(25)
                            )
                            for (label in labels) {
                                val effective = label.color.cpy()
                                label.style.fontColor?.let { effective.mul(it) }
                                check(effective == Color.valueOf("453925"))
                            }
                            popup.invalidateHierarchy()
                            popup.validate()
                            val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                            scroll.validate()
                            popup.innerTable.validate()
                            stage.draw()
                            val origin = scroll.localToStageCoordinates(Vector2())
                            val detail = "$type response=$responseIndex width=$width"
                            check(scroll.maxX <= 1f && scroll.maxY > 0f) { detail }
                            check(
                                origin.x >= -1f && origin.y >= -1f &&
                                        origin.x + scroll.width <= width + 1f &&
                                        origin.y + scroll.height <= 481f
                            ) { detail }
                            check(buttons[0].y >= buttons[1].top - 1f) { "Responses overlap: $detail" }
                            for (button in buttons) {
                                check(button.label.wrap) { "Response wrapping disabled: $detail" }
                                if (width == 360f) check(button.label.glyphLayout.runs.size > 1) {
                                    "Long response did not wrap on narrow scene: $detail"
                                }
                                check(button.label.height + 1f >= button.label.prefHeight) { "Response text clipped: $detail" }
                                check(button.x >= -1f && button.right <= responses.width + 1f) { detail }
                            }
                            val button = buttons[responseIndex]
                            val local = button.localToAscendantCoordinates(scroll.actor, Vector2())
                            scroll.scrollTo(local.x, local.y, button.width, button.height)
                            scroll.updateVisualScroll()
                            stage.draw()
                            val point = button.localToStageCoordinates(
                                Vector2(
                                    button.width / 2f,
                                    button.height / 2f
                                )
                            )
                            val hit = stage.hit(point.x, point.y, true)
                            check(hit === button || hit?.isDescendantOf(button) == true) {
                                "Response inaccessible: $detail caption=${originalCaptions[responseIndex]}"
                            }
                            check(civ.popupAlerts.toList() == listOf(alert, pending))
                            stage.stageToScreenCoordinates(point)
                            stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                            stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                            check(popup.parent == null && closed == 1) { detail }
                            check(civ.popupAlerts.toList() == listOf(pending)) { detail }
                            check(screen.shouldUpdate)
                            check(civ.gold == gold && civ.notifications.toList() == notifications)
                            check(civ.diplomacy == diplomacy)
                        } finally {
                            if (popup.parent != null) popup.close()
                        }
                    }
                }
            }
        } finally {
            civ.popupAlerts.clear()
            civ.popupAlerts.addAll(alerts)
            screen.shouldUpdate = shouldUpdate
            stage.viewport = viewport
            viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(visibleArea))
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }
    fun verifyDiscoveries(screen: WorldScreen) {
        check(screen.game.settings.skin == "Minimal")
        val civ = screen.viewingCiv
        val stage = screen.stage as UncivStage
        val viewport = stage.viewport
        val visibleArea = Rectangle(stage.lastKnownVisibleArea)
        val alerts = civ.popupAlerts.toList()
        val shouldUpdate = screen.shouldUpdate
        val researched = civ.tech.techsResearched.toSet()
        val researchQueue = civ.tech.techsToResearch.toList()
        val buildings =
                civ.cities.associate { it.id to it.cityConstructions.builtBuildings.toSet() }
        val gold = civ.gold
        val ruleset = screen.gameInfo.ruleSet
        val wonders = ruleset.buildings.values.filter { it.isWonder }
            .groupBy { com.unciv.ui.images.ImageGetter.wonderImageExists(it.name) }
            .values.map { group -> group.sortedBy { it.name }.first() }
        check(wonders.isNotEmpty())
        val technology = ruleset.technologies.values.sortedBy { it.name }.first()
        val samples = wonders.map { AlertType.WonderBuilt to it.name } +
                (AlertType.TechResearched to technology.name)
        try {
            for (width in listOf(360f, 1000f)) {
                stage.viewport = FitViewport(width, 480f)
                stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
                EventBus.send(UncivStage.VisibleAreaChanged(Rectangle(0f, 0f, width, 480f)))
                for ((type, value) in samples) {
                    val alert = PopupAlert(type, value)
                    val pending = PopupAlert(AlertType.GoldenAge, "")
                    civ.popupAlerts.clear()
                    civ.popupAlerts.addAll(listOf(alert, pending))
                    val popup = AlertPopup(screen, alert)
                    var closed = 0
                    popup.closeListeners.add { closed++ }
                    try {
                        popup.open(force = true)
                        check(popup.innerTable.background is RoyalPanelDrawable)
                        val quote = checkNotNull(popup.findActor<Label>("alertDiscoveryQuote"))
                        val description =
                                checkNotNull(popup.findActor<Label>("alertDiscoveryDescription"))
                        val illustration = checkNotNull(
                            popup.findActor<com.badlogic.gdx.scenes.scene2d.Actor>("alertDiscoveryIllustration")
                        )
                        val textTable = checkNotNull(
                            popup.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("alertDiscoveryText")
                        )
                        quote.setText(
                            "A quotation about the ancient knowledge of the distant royal provinces. ".repeat(
                                20
                            )
                        )
                        description.setText(
                            "A detailed description of the discovery and its effects in the royal provinces. ".repeat(
                                25
                            )
                        )
                        popup.invalidateHierarchy()
                        popup.validate()
                        val scroll = popup.children.filterIsInstance<ScrollPane>().single()
                        scroll.validate()
                        popup.innerTable.validate()
                        stage.draw()
                        val detail = "$type $value at ${width}x480"
                        val origin = scroll.localToStageCoordinates(Vector2())
                        check(
                            origin.x >= -1f && origin.y >= -1f &&
                                    origin.x + scroll.width <= width + 1f &&
                                    origin.y + scroll.height <= 481f
                        ) { detail }
                        check(scroll.maxX <= 1f && scroll.maxY > 0f) { detail }
                        check(
                            illustration.width > 0f && illustration.height > 0f &&
                                    illustration.x >= -1f && illustration.right <= popup.innerTable.width + 1f
                        ) { detail }
                        for (label in listOf(quote, description)) {
                            val effective = label.color.cpy()
                            label.style.fontColor?.let { effective.mul(it) }
                            check(effective == Color.valueOf("453925")) { detail }
                            check(label.wrap && label.glyphLayout.runs.size > 1) { detail }
                            check(label.height + 1f >= label.prefHeight) { detail }
                            check(label.x >= -1f && label.right <= textTable.width + 1f) { detail }
                        }
                        if (width == 360f) check(quote.y >= description.top - 1f) { detail }
                        else check(quote.right <= description.x + 1f) { detail }
                        val close =
                                popup.innerTable.children.filterIsInstance<TextButton>().single()
                        val local = close.localToAscendantCoordinates(scroll.actor, Vector2())
                        scroll.scrollTo(local.x, local.y, close.width, close.height)
                        scroll.updateVisualScroll()
                        stage.draw()
                        val point = close.localToStageCoordinates(
                            Vector2(
                                close.width / 2f,
                                close.height / 2f
                            )
                        )
                        val hit = stage.hit(point.x, point.y, true)
                        check(hit === close || hit?.isDescendantOf(close) == true) { "Discovery close inaccessible: $detail" }
                        stage.stageToScreenCoordinates(point)
                        stage.touchDown(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        stage.touchUp(point.x.toInt(), point.y.toInt(), 0, Input.Buttons.LEFT)
                        check(popup.parent == null && closed == 1) { detail }
                        check(civ.popupAlerts.toList() == listOf(pending)) { detail }
                        check(civ.tech.techsResearched == researched && civ.tech.techsToResearch.toList() == researchQueue)
                        check(civ.cities.associate { it.id to it.cityConstructions.builtBuildings.toSet() } == buildings)
                        check(civ.gold == gold)
                    } finally {
                        if (popup.parent != null) popup.close()
                    }
                }
            }
            println(
                "Discovery alert checks passed: wonder image variants=${
                    wonders.map {
                        com.unciv.ui.images.ImageGetter.wonderImageExists(
                            it.name
                        )
                    }
                }, technology=${technology.name}"
            )
        } finally {
            civ.popupAlerts.clear()
            civ.popupAlerts.addAll(alerts)
            screen.shouldUpdate = shouldUpdate
            stage.viewport = viewport
            viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
            EventBus.send(UncivStage.VisibleAreaChanged(visibleArea))
            for (actor in stage.actors)
                (actor as? com.badlogic.gdx.scenes.scene2d.utils.Layout)?.invalidateHierarchy()
        }
    }
}
