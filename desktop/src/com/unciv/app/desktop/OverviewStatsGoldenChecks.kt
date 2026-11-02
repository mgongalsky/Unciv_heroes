package com.unciv.app.desktop

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.ui.overviewscreen.EmpireOverviewScreen
import com.unciv.ui.overviewscreen.StatsOverviewTab
import com.unciv.ui.utils.TabbedPager
import com.unciv.ui.utils.UncivSlider
import kotlin.math.abs
import com.unciv.models.translations.tr

/** Проверяет размещение статистики и обновление через обработчик ползунка, без утверждения эталона. */
internal object OverviewStatsGoldenChecks {
    private fun descendants(actor: Actor): Sequence<Actor> = sequence {
        yield(actor)
        if (actor is Group)
            for (child in actor.children.toList()) yieldAll(descendants(child))
    }

    fun verify(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        pager.validate()
        check(pager.activePage == pager.getPageIndex("Stats")) {
            "Обзор не открыл вкладку статистики"
        }
        val stats = descendants(pager).filterIsInstance<StatsOverviewTab>().single()
        val slider = descendants(stats).filterIsInstance<UncivSlider>().single()
        check(!slider.isDisabled) { "Ползунок недоступен в ход игрока" }
        val gold = civ.gold
        val cities = civ.cities.toList()
        val food = cities.map { it.population.foodStored }
        val queues = cities.map { it.cityConstructions.constructionQueue.toList() }
        val original = civ.tech.goldPercentConvertedToScience
        try {
            for (value in listOf(0f, 0.5f, 1f)) {
                slider.value = value
                check(abs(civ.tech.goldPercentConvertedToScience - value) < 0.001f) {
                    "Обработчик ползунка не обновил долю конвертации"
                }
                pager.invalidateHierarchy()
                pager.validate()
                stats.validate()
                val panels = stats.children.filterIsInstance<Table>()
                check(panels.size >= 6)
                for ((index, panel) in panels.withIndex()) {
                    panel.validate()
                    check(panel.width > 0f && panel.height > 0f)
                    check(panel.x >= -1f && panel.right <= stats.width + 1f) {
                        "Панель статистики выходит за ширину страницы"
                    }
                    for (other in panels.drop(index + 1)) {
                        val overlapX = minOf(panel.right, other.right) - maxOf(panel.x, other.x)
                        val overlapY = minOf(panel.top, other.top) - maxOf(panel.y, other.y)
                        check(overlapX <= 1f || overlapY <= 1f) {
                            "Панели статистики перекрываются после обновления"
                        }
                    }
                }
                check(civ.gold == gold) { "Просмотр доходов изменил запас золота" }
                check(cities.map { it.population.foodStored } == food)
                check(cities.map { it.cityConstructions.constructionQueue.toList() } == queues)
            }
        } finally {
            slider.value = original
            stats.update()
            pager.invalidateHierarchy()
            pager.validate()
        }
    }
    fun verifyInput(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        val stats = descendants(pager).filterIsInstance<StatsOverviewTab>().single()
        val slider = descendants(stats).filterIsInstance<UncivSlider>().single()
        val plus = descendants(slider).filterIsInstance<com.unciv.ui.images.IconCircleGroup>()
            .single { group ->
                descendants(group).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                    .any { it.text.toString() == "+" }
            }
        val original = slider.value
        val disabled = slider.isDisabled
        val gold = civ.gold
        try {
            slider.value = 0.5f
            pager.validate()
            var parent = slider.parent
            while (parent != null && parent !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                parent = parent.parent
            val scroll = checkNotNull(parent as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
            scroll.validate()
            val location =
                    plus.localToAscendantCoordinates(scroll.actor, com.badlogic.gdx.math.Vector2())
            scroll.scrollTo(location.x, location.y, plus.width, plus.height)
            scroll.updateVisualScroll()
            scroll.layout()
            fun clickPlus() {
                val point = plus.localToStageCoordinates(
                    com.badlogic.gdx.math.Vector2(plus.width / 2f, plus.height / 2f)
                )
                check(point.x in 0f..screen.stage.width && point.y in 0f..screen.stage.height)
                screen.stage.stageToScreenCoordinates(point)
                screen.stage.touchDown(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                screen.stage.touchUp(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
            }

            val center = plus.localToStageCoordinates(
                com.badlogic.gdx.math.Vector2(plus.width / 2f, plus.height / 2f)
            )
            val hit = checkNotNull(screen.stage.hit(center.x, center.y, true))
            check(hit === plus || hit.isDescendantOf(plus)) { "Кнопка ползунка перекрыта" }
            clickPlus()
            check(abs(slider.value - 0.6f) < 0.001f)
            check(abs(civ.tech.goldPercentConvertedToScience - 0.6f) < 0.001f)
            slider.isDisabled = true
            clickPlus()
            check(abs(slider.value - 0.6f) < 0.001f) { "Отключённый ползунок принимает нажатие" }
            check(abs(civ.tech.goldPercentConvertedToScience - 0.6f) < 0.001f)
            check(civ.gold == gold)
        } finally {
            slider.isDisabled = disabled
            slider.value = original
            pager.setPageScrollY(pager.getPageIndex("Stats"), 0f)
            pager.invalidateHierarchy()
            pager.validate()
        }
    }
    fun verifyCities(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        val previousPage = pager.activePage
        val gold = civ.gold
        val cities = civ.cities.toList()
        val names = cities.map { it.name }
        val queues = cities.map { it.cityConstructions.constructionQueue.toList() }
        try {
            pager.selectPage("Cities")
            pager.validate()
            val tab = descendants(pager)
                .filterIsInstance<com.unciv.ui.overviewscreen.CityOverviewTab>().single()
            val header = checkNotNull(pager.findActor<Table>("overviewCitiesHeader"))
            val details = checkNotNull(pager.findActor<Table>("overviewCitiesDetails"))
            val total = checkNotNull(pager.findActor<Table>("overviewCitiesTotal"))
            fun checkColumns() {
                pager.validate()
                for (table in listOf(header, details, total)) table.validate()
                check(header.columns == details.columns && header.columns == total.columns)
                for (column in 0 until header.columns) {
                    check(abs(header.getColumnWidth(column) - details.getColumnWidth(column)) <= 1f) {
                        "Ширина столбца $column различается в заголовке и строках городов"
                    }
                    check(abs(header.getColumnWidth(column) - total.getColumnWidth(column)) <= 1f) {
                        "Ширина столбца $column различается в заголовке и итогах"
                    }
                }
            }
            checkColumns()
            check(tab.persistableData.sortedBy == "City")
            val originalDescending = tab.persistableData.descending
            repeat(2) { iteration ->
                val sort = header.cells.first().actor
                var ancestor = header.parent
                while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                    ancestor = ancestor.parent
                val scroll =
                        checkNotNull(ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                scroll.scrollX = 0f
                scroll.updateVisualScroll()
                scroll.layout()
                val point = sort.localToStageCoordinates(
                    com.badlogic.gdx.math.Vector2(sort.width / 2f, sort.height / 2f)
                )
                val hit = checkNotNull(screen.stage.hit(point.x, point.y, true))
                check(hit === sort || hit.isDescendantOf(sort)) { "Заголовок сортировки перекрыт" }
                screen.stage.stageToScreenCoordinates(point)
                screen.stage.touchDown(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                screen.stage.touchUp(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                check(tab.persistableData.sortedBy == "City")
                check(tab.persistableData.descending == if (iteration == 0) !originalDescending else originalDescending)
                checkColumns()
            }
            check(civ.gold == gold)
            check(civ.cities.toList() == cities && cities.map { it.name } == names)
            check(cities.map { it.cityConstructions.constructionQueue.toList() } == queues)
        } finally {
            pager.selectPage(previousPage)
            pager.validate()
        }
    }
    fun verifyResources(
        screen: EmpireOverviewScreen,
        supply: com.unciv.models.ruleset.tile.ResourceSupplyList
    ) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        val previousPage = pager.activePage
        val before = supply.map { Triple(it.resource.name, it.origin, it.amount) }
        try {
            pager.selectPage("Resources")
            pager.validate()
            val tab = descendants(pager)
                .filterIsInstance<com.unciv.ui.overviewscreen.ResourcesOverviewTab>().single()
            check(!tab.persistableData.vertical)
            fun verifyValues() {
                pager.validate()
                tab.validate()
                val labels =
                        descendants(tab).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                            .toList()
                for (text in listOf("7", "-3", "4"))
                    check(labels.any { it.text.toString() == text }) { "В ресурсах отсутствует значение $text" }
                if (screen.game.settings.skin == "Minimal")
                    check(labels.filter { it.text.toString() == "-3" }
                        .all { it.color == com.badlogic.gdx.graphics.Color.valueOf("8C2923") })
                check(supply.map { Triple(it.resource.name, it.origin, it.amount) } == before)
            }
            verifyValues()
            for (vertical in listOf(true, false)) {
                val name =
                        if (vertical) "overviewResourcesToVertical" else "overviewResourcesToHorizontal"
                val button = checkNotNull(pager.findActor<Actor>(name))
                var ancestor = button.parent
                while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                    ancestor = ancestor.parent
                val scroll =
                        checkNotNull(ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                scroll.validate()
                val position = button.localToAscendantCoordinates(
                    scroll.actor,
                    com.badlogic.gdx.math.Vector2()
                )
                scroll.scrollTo(position.x, position.y, button.width, button.height)
                scroll.updateVisualScroll()
                scroll.layout()
                val point = button.localToStageCoordinates(
                    com.badlogic.gdx.math.Vector2(button.width / 2f, button.height / 2f)
                )
                val hit = checkNotNull(screen.stage.hit(point.x, point.y, true))
                check(hit === button || hit.isDescendantOf(button)) { "Переключатель ресурсов перекрыт" }
                screen.stage.stageToScreenCoordinates(point)
                screen.stage.touchDown(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                screen.stage.touchUp(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                check(tab.persistableData.vertical == vertical)
                verifyValues()
            }
        } finally {
            pager.selectPage(previousPage)
            pager.validate()
        }
    }
    fun verifyTrades(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val world = civ.gameInfo
        val other = CivilizationInfo().apply {
            civName =
                    world.ruleSet.nations.keys.first { it != civ.civName && world.civilizations.none { existing -> existing.civName == it } }
            nation = world.ruleSet.nations.getValue(civName)
            gameInfo = world
        }
        val city = civ.cities.first()
        val originalName = city.name
        val trade = com.unciv.logic.trade.Trade().apply {
            ourOffers.add(
                com.unciv.logic.trade.TradeOffer(
                    "Gold",
                    com.unciv.logic.trade.TradeType.Gold,
                    123,
                    duration = -1
                )
            )
            ourOffers.add(
                com.unciv.logic.trade.TradeOffer(
                    city.id,
                    com.unciv.logic.trade.TradeType.City,
                    duration = -1
                )
            )
            theirOffers.add(
                com.unciv.logic.trade.TradeOffer(
                    "Gold",
                    com.unciv.logic.trade.TradeType.Gold,
                    456,
                    duration = -1
                )
            )
        }
        val diplomacy = com.unciv.logic.civilization.diplomacy.DiplomacyManager().apply {
            civInfo = civ
            otherCivName = other.civName
            trades.add(trade)
        }
        val originalDiplomacy = civ.diplomacy[other.civName]
        val originalOffers =
                trade.ourOffers.map { it.copy() } to trade.theirOffers.map { it.copy() }
        val gold = civ.gold
        val viewport = screen.stage.viewport
        val originalWidth = viewport.worldWidth
        val originalHeight = viewport.worldHeight
        world.civilizations.add(other)
        civ.diplomacy[other.civName] = diplomacy
        try {
            city.name =
                    "The ancient royal city beyond the northern mountains and the distant river valleys"
            for (width in listOf(500f, 1000f)) {
                viewport.setWorldSize(width, originalHeight)
                val tab = com.unciv.ui.overviewscreen.TradesOverviewTab(civ, screen)
                tab.pack()
                check(tab.width <= width + 1f) { "Торговая вкладка шире сцены $width: ${tab.width}" }
                val deal = tab.children.filterIsInstance<Table>().single()
                val sides = deal.children.filterIsInstance<Table>()
                check(sides.size == 2)
                sides.forEach { it.validate() }
                if (width < 700f) {
                    check(sides[0].y >= sides[1].top - 1f) { "Стороны сделки не расположены вертикально" }
                } else {
                    check(sides[0].right <= sides[1].x + 1f) { "Стороны сделки перекрываются" }
                }
                val cityLabel =
                        descendants(tab).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                            .single { it.text.toString().trim() == city.name }
                cityLabel.validate()
                check(cityLabel.glyphLayout.runs.size > 1) { "Длинное название в предложении не перенесено" }
                check(trade.ourOffers.map { it.copy() } == originalOffers.first)
                check(trade.theirOffers.map { it.copy() } == originalOffers.second)
                check(civ.gold == gold && city.civInfo === civ)
            }
        } finally {
            viewport.setWorldSize(originalWidth, originalHeight)
            city.name = originalName
            if (originalDiplomacy == null) civ.diplomacy.remove(other.civName)
            else civ.diplomacy[other.civName] = originalDiplomacy
            world.civilizations.remove(other)
        }
    }
    fun verifyNotifications(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val originalNotifications = civ.notifications.toList()
        val viewport = screen.stage.viewport
        val originalWidth = viewport.worldWidth
        val originalHeight = viewport.worldHeight
        val message = "A long notification from the distant royal provinces. ".repeat(20)
        val samples = listOf(
            com.unciv.logic.civilization.Notification(message, arrayListOf()),
            com.unciv.logic.civilization.Notification(
                message,
                ArrayList(List(40) { com.unciv.logic.civilization.NotificationIcon.Gold })
            )
        )
        try {
            civ.notifications.clear()
            civ.notifications.addAll(samples)
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, originalHeight)
                val tab = com.unciv.ui.overviewscreen.NotificationsOverviewTable(
                    checkNotNull(screen.game.worldScreen), civ, screen
                )
                tab.pack()
                check(tab.width <= width + 1f) { "Уведомления шире сцены $width: ${tab.width}" }
                val entries = descendants(tab).filterIsInstance<Table>()
                    .filter { it.name == "overviewNotificationEntry" }
                    .filter { entry ->
                        entry.children.filterIsInstance<com.unciv.ui.utils.WrappableLabel>()
                            .any { it.text.toString() == message }
                    }.toList()
                check(entries.size == 2)
                for (entry in entries) {
                    entry.validate()
                    val label = entry.children.filterIsInstance<com.unciv.ui.utils.WrappableLabel>()
                        .single()
                    label.validate()
                    check(label.width >= 48f && label.height > 0f)
                    check(label.glyphLayout.runs.size > 1) { "Длинное уведомление не перенесено" }
                    check(label.x >= 0f && label.right <= entry.width + 1f)
                }
                check(entries[0].children.none { it is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane })
                val icons =
                        entries[1].children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()
                            .single()
                icons.validate()
                check(icons.maxX > 0f) { "Длинный ряд значков не требует прокрутки" }
                icons.scrollPercentX = 1f
                icons.updateVisualScroll()
                icons.layout()
                val end = icons.actor.localToAscendantCoordinates(
                    icons, com.badlogic.gdx.math.Vector2(icons.actor.width, 0f)
                )
                check(end.x >= 0f && end.x <= icons.width + 1f) { "Последний значок недоступен" }
                check(civ.notifications.toList() == samples)
                check(samples.all { it.text == message })
                check(samples[0].icons.isEmpty() && samples[1].icons.size == 40)
            }
        } finally {
            civ.notifications.clear()
            civ.notifications.addAll(originalNotifications)
            viewport.setWorldSize(originalWidth, originalHeight)
        }
    }
    fun verifyUnits(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        val previousPage = pager.activePage
        val units = civ.getCivUnits().toList()
        check(units.isNotEmpty())
        val before = units.map {
            listOf(
                it.health,
                it.currentMovement,
                it.currentTile.position.cpy(),
                it.action
            )
        }
        try {
            pager.selectPage("Units")
            pager.validate()
            val tab =
                    descendants(pager).filterIsInstance<com.unciv.ui.overviewscreen.UnitOverviewTab>()
                        .single()
            val fixed = tab.getFixedContent()
            val labels =
                    descendants(tab).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                        .toList()
            val colors = labels.map { it.color.cpy() }
            if (screen.game.settings.skin == "Minimal") {
                check(labels.any { it.color == com.badlogic.gdx.graphics.Color.valueOf("285C32") }) {
                    "Юнит в городе не имеет зелёной подписи местоположения"
                }
                repeat(2) {
                    check(tab.getFixedContent() === fixed)
                    check(labels.map { it.color } == colors) { "Повторное оформление изменило цвета списка юнитов" }
                }
            }
            val expander =
                    descendants(fixed).filterIsInstance<com.unciv.ui.utils.ExpanderTab>().single()
            val wasOpen = expander.isOpen
            try {
                repeat(2) { iteration ->
                    expander.toggle()
                    expander.act(1f)
                    pager.validate()
                    check(expander.isOpen == if (iteration == 0) !wasOpen else wasOpen)
                    check(expander.width <= screen.stage.width + 1f)
                    check(pager.activePage == pager.getPageIndex("Units"))
                }
            } finally {
                if (expander.isOpen != wasOpen) {
                    expander.toggle()
                    expander.act(1f)
                }
            }
            check(civ.getCivUnits().toList() == units)
            check(units.map {
                listOf(
                    it.health,
                    it.currentMovement,
                    it.currentTile.position.cpy(),
                    it.action
                )
            } == before)
        } finally {
            pager.selectPage(previousPage)
            pager.validate()
        }
    }
    fun verifyPolitics(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val pager = screen.stage.actors.filterIsInstance<TabbedPager>().single()
        val previousPage = pager.activePage
        val diplomacy = civ.diplomacy.mapValues { (_, manager) ->
            manager.diplomaticStatus to manager.trades.map { trade ->
                trade.ourOffers.map { it.copy() } to trade.theirOffers.map { it.copy() }
            }
        }
        try {
            pager.selectPage("Politics")
            pager.validate()
            val tab =
                    descendants(pager).filterIsInstance<com.unciv.ui.overviewscreen.GlobalPoliticsOverviewTable>()
                        .single()

            fun click(caption: String) {
                pager.validate()
                val button =
                        descendants(pager).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextButton>()
                            .single { it.text.toString() == caption }
                var ancestor = button.parent
                while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                    ancestor = ancestor.parent
                val scroll =
                        checkNotNull(ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                scroll.validate()
                val local = button.localToAscendantCoordinates(
                    scroll.actor,
                    com.badlogic.gdx.math.Vector2()
                )
                scroll.scrollTo(local.x, local.y, button.width, button.height)
                scroll.updateVisualScroll()
                scroll.layout()
                val point = button.localToStageCoordinates(
                    com.badlogic.gdx.math.Vector2(
                        button.width / 2f,
                        button.height / 2f
                    )
                )
                val hit = checkNotNull(screen.stage.hit(point.x, point.y, true))
                check(hit === button || hit.isDescendantOf(button)) { "Кнопка Politics перекрыта: $caption" }
                screen.stage.stageToScreenCoordinates(point)
                screen.stage.touchDown(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                screen.stage.touchUp(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                pager.validate()
            }

            val included = tab.persistableData.includeCityStates
            click("Show diagram")
            repeat(2) { iteration ->
                click(com.unciv.Constants.cityStates)
                check(tab.persistableData.includeCityStates == if (iteration == 0) !included else included)
            }
            click("Show global politics")
            check(
                descendants(tab).filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextButton>()
                    .any { it.text.toString() == "Show diagram" })
            check(civ.diplomacy.mapValues { (_, manager) ->
                manager.diplomaticStatus to manager.trades.map { trade ->
                    trade.ourOffers.map { it.copy() } to trade.theirOffers.map { it.copy() }
                }
            } == diplomacy)
        } finally {
            pager.selectPage(previousPage)
            pager.validate()
        }
    }

    /** Проверяет заполненный пантеон, узкую раскладку и повторное построение без изменения игровых данных. */
    fun verifyReligion(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val world = civ.gameInfo
        val manager = civ.religionManager
        val originalReligion = manager.religion
        val originalFaith = manager.storedFaith
        val originalState = manager.religionState
        val viewport = screen.stage.viewport
        val originalWidth = viewport.worldWidth
        val originalHeight = viewport.worldHeight
        val belief = world.ruleSet.beliefs.values.first {
            it.type == com.unciv.models.ruleset.BeliefType.Pantheon
        }
        val name = "Royal overview verification pantheon"
        val displayName =
                "The ancient faith of the distant royal provinces beyond the northern mountains"
        val originalEntry = world.religions[name]
        val religion = com.unciv.models.Religion(name, world, civ.civName).apply {
            this.displayName = displayName
            followerBeliefs.add(belief.name)
        }
        try {
            world.religions[name] = religion
            manager.religion = religion
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, originalHeight)
                val tab = com.unciv.ui.overviewscreen.ReligionOverviewTab(civ, screen)
                var summaryLabelCount: Int? = null
                repeat(3) {
                    tab.select(name)
                    tab.pack()
                    check(tab.persistableData.selectedReligion == name)
                    val labels = descendants(tab)
                        .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>().toList()
                    val nameLabel = labels.single { it.text.toString() == displayName }
                    check(labels.any { it.text.toString().contains(belief.name) }) {
                        "Описание выбранного верования отсутствует"
                    }
                    val fixed = tab.getFixedContent()
                    fixed.pack()
                    val fixedLabels = descendants(fixed)
                        .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>().toList()
                    val count = fixedLabels.size
                    check(count > 0)
                    check(summaryLabelCount == null || summaryLabelCount == count) {
                        "Повторное построение дублирует строки сводки религии"
                    }
                    summaryLabelCount = count
                    val buttons = descendants(fixed)
                        .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Button>().toList()
                    check(buttons.any { it.touchable == com.badlogic.gdx.scenes.scene2d.Touchable.disabled }) {
                        "Кнопка выбранной религии принимает ввод"
                    }
                    if (screen.game.settings.skin == "Minimal") {
                        check(tab.width <= width + 1f) { "Содержимое Religion шире сцены $width: ${tab.width}" }
                        check(fixed.width <= width + 1f) { "Заголовок Religion шире сцены $width: ${fixed.width}" }
                        val heading = fixedLabels.single { it.text.toString() == displayName }
                        for (label in listOf(nameLabel, heading)) {
                            label.validate()
                            check(label.wrap && label.width > 0f && label.height > 0f)
                            check(label.color == com.badlogic.gdx.graphics.Color.valueOf("453925"))
                            if (width == 360f) check(label.glyphLayout.runs.size > 1) {
                                "Длинное название религии не перенесено"
                            }
                        }
                    }
                }
            }
            check(manager.storedFaith == originalFaith && manager.religionState == originalState)
            check(manager.religion === religion && world.religions[name] === religion)
            check(religion.displayName == displayName)
            check(religion.followerBeliefs == setOf(belief.name))
            check(religion.founderBeliefs.isEmpty())
        } finally {
            viewport.setWorldSize(originalWidth, originalHeight)
            manager.religion = originalReligion
            if (originalEntry == null) world.religions.remove(name)
            else world.religions[name] = originalEntry
        }
    }

    /** Проверяет сведения о чудесах и выравнивание таблицы на временном снимке построек. */
    fun verifyWonders(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val city = civ.cities.first()
        val constructions = city.cityConstructions
        val originalBuildings = constructions.builtBuildings
        val originalName = city.name
        val originalGold = civ.gold
        val originalQueue = constructions.constructionQueue.toList()
        val wonder = civ.gameInfo.ruleSet.buildings.values.first {
            it.isWonder && it.name !in originalBuildings
        }
        val sampleBuildings = HashSet(originalBuildings).apply { add(wonder.name) }
        val longName =
                "The ancient royal city beyond the northern mountains and the distant river valleys"
        try {
            // collectInfo читает имена построек напрямую; завершение строительства и его эффекты не вызываются.
            constructions.builtBuildings = sampleBuildings
            city.name = longName
            val tab = com.unciv.ui.overviewscreen.WonderOverviewTab(civ, screen)
            tab.pack()
            val header = tab.getFixedContent()
            header.pack()
            check(header.columns == tab.columns)
            for (column in 0 until header.columns) {
                check(abs(header.getColumnWidth(column) - tab.getColumnWidth(column)) <= 1f) {
                    "Столбец Wonders $column не совпадает с заголовком"
                }
            }
            val labels = descendants(tab)
                .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>().toList()
            val entries = tab.wonderInfo.collectInfo()
            val owned = entries.single { it.name == wonder.name }
            check(owned.status == com.unciv.logic.civilization.WonderInfo.WonderStatus.Owned)
            check(labels.any { it.text.toString() == owned.getNameColumn().tr() })
            val location = labels.single { it.text.toString() == longName }
            location.validate()
            if (screen.game.settings.skin == "Minimal") {
                check(location.wrap && location.glyphLayout.runs.size > 1) {
                    "Длинное местоположение чуда не перенесено"
                }
            }
            val hidden = entries.filter {
                it.status == com.unciv.logic.civilization.WonderInfo.WonderStatus.Hidden
            }
            check(hidden.isNotEmpty()) { "Заготовка не содержит скрытых чудес" }
            for (entry in hidden) {
                check(labels.none { it.text.toString() == entry.name.tr() }) {
                    "Скрытое чудо появилось в обзоре: ${entry.name}"
                }
            }
            check(constructions.builtBuildings === sampleBuildings)
            check(sampleBuildings == originalBuildings + wonder.name)
            check(city.name == longName && civ.gold == originalGold)
            check(constructions.constructionQueue.toList() == originalQueue)
        } finally {
            constructions.builtBuildings = originalBuildings
            city.name = originalName
        }
    }

    /** Проверяет состояния дерева и отмену выбора через Stage, допуская пересоздание обзора при возврате. */
    fun verifyTechnologies(
        screen: EmpireOverviewScreen,
        civ: CivilizationInfo
    ): EmpireOverviewScreen {
        val manager = civ.tech
        val originalQueue = manager.techsToResearch
        val researched = manager.techsResearched.toSet()
        val progress = manager.techsInProgress.toMap()
        val freeTechs = manager.freeTechs
        val overflow = manager.overflowScience
        val technologies = civ.gameInfo.ruleSet.technologies.values
        val path = technologies.asSequence().map { manager.getRequiredTechsToDestination(it) }
            .first { it.size >= 2 && it.any { tech -> !manager.canBeResearched(tech.name) } }
        val sampleQueue = ArrayList(path.map { it.name })
        val sampleBefore = sampleQueue.toList()
        var picker: com.unciv.ui.pickerscreens.TechPickerScreen? = null
        try {
            manager.techsToResearch = sampleQueue
            val tree = com.unciv.ui.pickerscreens.TechPickerScreen(civ)
            picker = tree
            screen.game.pushScreen(tree)
            tree.pickerPane.validate()
            val cards = descendants(tree.topTable)
                .filterIsInstance<com.unciv.ui.pickerscreens.TechButton>().toList()
            check(cards.size == technologies.size)
            fun card(name: String) = cards.single { it.text.text.toString() == name.tr() }
            fun verifyEraHeadings() {
                if (screen.game.settings.skin != "Minimal") return
                val labels = descendants(tree.topTable)
                    .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>().toList()
                for (era in technologies.map { it.era() }.toSet()) {
                    val heading = labels.single { it.text.toString() == era.tr() }
                    heading.validate()
                    val effective = heading.color.cpy()
                    heading.style.fontColor?.let { effective.mul(it) }
                    check(effective == com.badlogic.gdx.graphics.Color.valueOf("453925")) {
                        "Подпись эпохи $era потеряла тёмный цвет"
                    }
                    check(heading.width > 0f && heading.height + 1f >= heading.prefHeight) {
                        "Подпись эпохи $era обрезана по высоте"
                    }
                }
            }
            if (screen.game.settings.skin == "Minimal") {
                val colors = cards.map { it.bg.color.toString() }.toSet()
                for (hex in listOf("D8BD72", "9BC6D1", "B8C997", "A6BDB2", "C9C1AC")) {
                    check(com.badlogic.gdx.graphics.Color.valueOf(hex).toString() in colors) {
                        "В заготовке дерева отсутствует состояние $hex"
                    }
                }
                check(cards.all { it.color == com.badlogic.gdx.graphics.Color.WHITE }) {
                    "Общее тонирование карточки изменяет цвета значков"
                }
            }
            verifyEraHeadings()
            fun click(actor: Actor) {
                tree.pickerPane.validate()
                var ancestor = actor.parent
                while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                    ancestor = ancestor.parent
                (ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)?.let { scroll ->
                    scroll.validate()
                    val local = actor.localToAscendantCoordinates(
                        scroll.actor,
                        com.badlogic.gdx.math.Vector2()
                    )
                    scroll.scrollTo(local.x, local.y, actor.width, actor.height)
                    scroll.updateVisualScroll()
                    scroll.layout()
                }
                val point = actor.localToStageCoordinates(
                    com.badlogic.gdx.math.Vector2(actor.width / 2f, actor.height / 2f)
                )
                val hit = checkNotNull(tree.stage.hit(point.x, point.y, true))
                check(hit === actor || hit.isDescendantOf(actor)) { "Контрол дерева технологий перекрыт" }
                tree.stage.stageToScreenCoordinates(point)
                tree.stage.touchDown(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
                tree.stage.touchUp(
                    point.x.toInt(),
                    point.y.toInt(),
                    0,
                    com.badlogic.gdx.Input.Buttons.LEFT
                )
            }

            val alternative = technologies.first {
                manager.canBeResearched(it.name) && it.name != sampleBefore.first()
            }
            click(card(alternative.name))
            if (screen.game.settings.skin == "Minimal") {
                check(card(alternative.name).bg.color == com.badlogic.gdx.graphics.Color.valueOf("9BC6D1")) {
                    "Выбор не обновил текущее состояние карточки"
                }
            }
            verifyEraHeadings()
            check(manager.techsToResearch === sampleQueue && sampleQueue == sampleBefore)
            click(tree.closeButton)
            val returned =
                    checkNotNull(screen.game.screen as? EmpireOverviewScreen) { "Отмена не вернула обзор" }
            val pager = returned.stage.actors.filterIsInstance<TabbedPager>().single()
            pager.validate()
            check(pager.activePage == pager.getPageIndex("Stats")) { "Возврат из дерева не сохранил вкладку Stats" }
            check(manager.techsToResearch === sampleQueue && sampleQueue == sampleBefore)
            check(manager.techsResearched == researched && manager.techsInProgress == progress)
            check(manager.freeTechs == freeTechs && manager.overflowScience == overflow)
            return returned
        } finally {
            if (picker != null && screen.game.screen === picker) screen.game.popScreen()
            manager.techsToResearch = originalQueue
        }
    }

    /** Проверяет локальные цвета описания и внутреннюю ссылку цивилопедии без изменения ruleset. */
    fun verifyCivilopedia(screen: EmpireOverviewScreen, civ: CivilizationInfo) {
        val entries = civ.gameInfo.ruleSet.technologies.values.take(2)
        check(entries.size == 2)
        val source = entries[0]
        val target = entries[1]
        val sourceText = source.civilopediaText
        val targetText = target.civilopediaText
        val ordinary = "Royal civilopedia ordinary text"
        val explicit = "Royal civilopedia explicit red text"
        val linkCaption = "Royal civilopedia internal navigation"
        val destination = "Royal civilopedia destination text"
        var pedia: com.unciv.ui.civilopedia.CivilopediaScreen? = null
        try {
            source.civilopediaText = listOf(
                com.unciv.ui.civilopedia.FormattedLine(ordinary),
                com.unciv.ui.civilopedia.FormattedLine(explicit, color = "#8C2923"),
                com.unciv.ui.civilopedia.FormattedLine(
                    linkCaption,
                    link = "Technology/${target.name}"
                )
            )
            target.civilopediaText = listOf(com.unciv.ui.civilopedia.FormattedLine(destination))
            val view = com.unciv.ui.civilopedia.CivilopediaScreen(
                civ.gameInfo.ruleSet,
                com.unciv.ui.civilopedia.CivilopediaCategories.Technology,
                source.name
            )
            pedia = view
            fun layout() {
                for (root in view.stage.actors.toList()) {
                    descendants(root).filterIsInstance<com.badlogic.gdx.scenes.scene2d.utils.Layout>()
                        .forEach { it.validate() }
                }
            }

            fun labels() = view.stage.actors.toList().asSequence().flatMap { descendants(it) }
                .filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Label>().toList()
            layout()
            val plainLabel = labels().single { it.text.toString() == ordinary }
            val redLabel = labels().single { it.text.toString() == explicit }
            fun effectiveColor(label: com.badlogic.gdx.scenes.scene2d.ui.Label): com.badlogic.gdx.graphics.Color {
                val color = label.color.cpy()
                label.style.fontColor?.let { color.mul(it) }
                return color
            }
            if (screen.game.settings.skin == "Minimal") {
                check(effectiveColor(plainLabel) == com.badlogic.gdx.graphics.Color.valueOf("453925"))
            }
            check(effectiveColor(redLabel) == com.badlogic.gdx.graphics.Color.valueOf("8C2923")) {
                "Цивилопедия потеряла явный цвет разметки"
            }
            val linkLabel = labels().single { it.text.toString() == linkCaption }
            var linkActor: Actor = linkLabel
            while (linkActor.listeners.size == 0) {
                linkActor =
                        checkNotNull(linkActor.parent) { "Внутренняя ссылка не имеет обработчика" }
            }
            var ancestor = linkActor.parent
            while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                ancestor = ancestor.parent
            val scroll = checkNotNull(ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
            scroll.validate()
            val local = linkActor.localToAscendantCoordinates(
                scroll.actor,
                com.badlogic.gdx.math.Vector2()
            )
            scroll.scrollTo(local.x, local.y, linkActor.width, linkActor.height)
            scroll.updateVisualScroll()
            scroll.layout()
            val point = linkLabel.localToStageCoordinates(
                com.badlogic.gdx.math.Vector2(linkLabel.width / 2f, linkLabel.height / 2f)
            )
            val hit = checkNotNull(view.stage.hit(point.x, point.y, true))
            check(hit === linkActor || hit.isDescendantOf(linkActor)) { "Ссылка цивилопедии перекрыта" }
            view.stage.stageToScreenCoordinates(point)
            view.stage.touchDown(
                point.x.toInt(),
                point.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            view.stage.touchUp(
                point.x.toInt(),
                point.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            layout()
            check(labels().any { it.text.toString() == destination }) { "Ссылка не открыла целевую запись" }
            check(labels().none { it.text.toString() == ordinary }) { "Старое описание осталось после перехода" }
            view.selectEntry(source.name, noScrollAnimation = true)
            layout()
            check(labels().count { it.text.toString() == ordinary } == 1)
            check(source.civilopediaText.map { it.text } == listOf(ordinary, explicit, linkCaption))
            check(target.civilopediaText.single().text == destination)
        } finally {
            pedia?.dispose()
            source.civilopediaText = sourceText
            target.civilopediaText = targetText
        }
    }
}
