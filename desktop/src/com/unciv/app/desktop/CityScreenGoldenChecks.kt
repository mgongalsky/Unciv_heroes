package com.unciv.app.desktop

import com.badlogic.gdx.math.Vector2
import com.unciv.logic.GameInfo
import com.unciv.logic.city.CityInfo
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.PlayerType
import com.unciv.logic.map.MapUnit
import com.unciv.logic.map.TileMap
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.ui.cityscreen.CityScreen
import com.unciv.ui.utils.BaseScreen
import com.unciv.utils.concurrency.launchOnGLThread

/** Loads an in-memory world through the production lifecycle before opening its city. */
internal object CityScreenGoldenChecks {
    fun loadingScreen(onReady: (BaseScreen) -> Unit): BaseScreen = object : BaseScreen() {
        private var started = false
        override fun show() {
            if (started) return
            started = true
            com.unciv.utils.concurrency.Concurrency.run("City golden setup") {
                launchOnGLThread {
                    val world = createWorld()
                    game.loadGame(world)
                    val city = world.currentPlayerCiv.cities.single()
                    addVisitingHero(city)
                    prepareReligion(city)
                    val specialistBuilding = world.ruleSet.buildings.values
                        .filter { !it.newSpecialists().isEmpty() }
                        .minByOrNull { it.name } ?: error("Ruleset has no specialist building")
                    city.cityConstructions.addBuilding(specialistBuilding.name)
                    prepareProductionSections(city)
                    val construction = world.ruleSet.buildings.getValue("Granary")
                    val screen = CityScreen(city, initSelectedConstruction = construction)
                    game.pushScreen(screen)
                    check(game.worldScreen != null)
                    check(screen.canChangeState)
                    check(screen.selectedConstruction === construction)
                    verifyConstructionCard(screen, city)
                    verifyTileCard(screen, city)
                    verifyCitizenManagement(screen, city)
                    verifyHeroSupply(screen, city)
                    verifySpecialists(screen, city)
                    verifyReligion(screen, city)
                    verifyCityPicker(screen, city)
                    verifyArmySlotAppearance(screen, city)
                    verifyDetailedStatsModel(screen)
                    verifyDetailedStats(screen)
                    screen.stage.root.addAction(object : com.badlogic.gdx.scenes.scene2d.Action() {
                        override fun act(delta: Float): Boolean {
                            if (!constructionsReady(screen)) return false
                            onReady(screen)
                            return true
                        }
                    })
                }
            }
        }
    }

    private fun createWorld(): GameInfo {
        val ruleset = RulesetCache.getComplexRuleset(linkedSetOf(), BaseRuleset.Civ_V_GnK.fullName)
        for (unit in ruleset.units.values) unit.ruleset = ruleset
        val world = GameInfo().apply {
            ruleSet = ruleset
            gameParameters.baseRuleset = BaseRuleset.Civ_V_GnK.fullName
            gameParameters.numberOfCityStates = 0
            gameParameters.noBarbarians = true
            difficulty = "Prince"
            difficultyObject = ruleset.difficulties.getValue(difficulty)
            speed = ruleset.speeds.getValue(gameParameters.speed)
            turns = 1
            tileMap = TileMap(6, ruleset, false)
            tileMap.gameInfo = this
        }
        val civilization = CivilizationInfo().apply {
            civName = "Rome"
            nation = ruleset.nations.getValue(civName)
            gameInfo = world
            playerType = PlayerType.Human
        }
        world.civilizations.add(civilization)
        world.currentPlayer = civilization.civName
        world.currentPlayerCiv = civilization
        ruleset.technologies.values.minByOrNull { it.era() }?.let {
            civilization.tech.techsResearched.add(it.name)
        }
        civilization.setTransients()
        val founder = MapUnit().apply {
            civInfo = civilization
            owner = civilization.civName
            army.setTransients(civilization, this)
        }
        CityInfo(civilization, Vector2.Zero, founder).apply {
            name = "Royal City"
            cityConstructions.constructionQueue.clear()
            cityConstructions.constructionQueue.add("Granary")
        }
        civilization.exploredTiles = world.tileMap.values.map { it.position.cpy() }.toHashSet()
        world.setTransients()
        check(world.currentPlayerCiv.cities.size == 1)
        return world
    }

    private fun verifyConstructionCard(screen: CityScreen, city: CityInfo) {
        val card = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.ConstructionInfoTable>().single()
        val longest = city.getRuleset().buildings.values.maxByOrNull {
            it.getDescription(city, true).length
        } ?: error("City fixture has no buildings")
        val original = screen.selectedConstruction
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (construction in listOfNotNull(original, longest, original)) {
                    card.update(construction)
                    card.validate()
                    check(card.width <= width - 8f) {
                        "Construction card ${construction.name} exceeds scene width: ${card.width} > $width"
                    }
                    check(card.height <= 470f) {
                        "Construction card ${construction.name} exceeds scene height: ${card.height} at width $width"
                    }
                    if (screen.game.settings.skin == "Minimal") {
                        val scroll = checkNotNull(
                            card.findActor<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>(
                                "cityConstructionScroll"
                            )
                        )
                        scroll.validate()
                        check(card.height <= 235f) { "Construction card leaves no room for city statistics" }
                        check(scroll.scrollY == 0f) { "New construction retains the previous scroll position" }
                        check(scroll.maxX <= 1f) { "Construction card requires horizontal scrolling" }
                        val content = scroll.actor as com.badlogic.gdx.scenes.scene2d.ui.Table
                        check(content.width >= content.prefWidth - 1f) {
                            "Construction content is narrower than its wrapped text layout"
                        }
                        if (construction === longest) {
                            check(scroll.maxY > 0f) { "Long construction description did not exercise scrolling" }
                            scroll.scrollPercentY = 1f
                            scroll.updateVisualScroll()
                            scroll.layout()
                            check(kotlin.math.abs(scroll.visualScrollY - scroll.maxY) <= 1f) {
                                "Construction description cannot scroll to its end"
                            }
                            val bottom =
                                    content.localToAscendantCoordinates(scroll, Vector2(0f, 0f))
                            check(bottom.y >= -1f && bottom.y <= scroll.height) {
                                "Bottom of construction content remains outside the scroll viewport"
                            }
                        }
                    }
                }
            }
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            card.update(original)
            card.setPosition(
                oldWidth - CityScreen.posFromEdge,
                CityScreen.posFromEdge,
                com.badlogic.gdx.utils.Align.bottomRight
            )
        }
    }
    private fun verifyTileCard(screen: CityScreen, city: CityInfo) {
        val card = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.CityScreenTileTable>().single()
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val gold = city.civInfo.gold
        val territory = city.tiles.toSet()
        val locked = city.lockedTiles.toSet()
        val worked = city.workedTiles.toSet()
        val tiles = city.tilesInRange.toList()
        check(tiles.isNotEmpty())
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (tile in tiles) {
                    card.update(tile)
                    card.validate()
                    check(card.isVisible)
                    check(card.width <= width - 8f && card.height <= 470f) {
                        "Tile card ${tile.position} exceeds scene bounds: ${card.width} x ${card.height}"
                    }
                    if (screen.game.settings.skin == "Minimal") {
                        val scroll = checkNotNull(
                            card.findActor<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>(
                                "cityTileScroll"
                            )
                        )
                        scroll.validate()
                        check(card.height <= 235f)
                        check(scroll.scrollY == 0f)
                        check(scroll.maxX <= 1f) { "Tile card requires horizontal scrolling" }
                        val content = scroll.actor as com.badlogic.gdx.scenes.scene2d.ui.Table
                        check(content.background is com.unciv.ui.utils.RoyalPanelDrawable)
                        check(content.width >= content.prefWidth - 1f) { "Tile card content is clipped horizontally" }
                        scroll.scrollPercentY = 1f
                        scroll.updateVisualScroll()
                        scroll.layout()
                        val bottom = content.localToAscendantCoordinates(scroll, Vector2(0f, 0f))
                        check(bottom.y >= -1f && bottom.y <= scroll.height) {
                            "Bottom of tile card cannot be reached"
                        }
                    }
                }
            }
            check(city.civInfo.gold == gold)
            check(city.tiles.toSet() == territory)
            check(city.lockedTiles.toSet() == locked && city.workedTiles.toSet() == worked)
            card.update(null)
            check(!card.isVisible)
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            card.update(screen.selectedTile)
            card.setPosition(
                oldWidth - CityScreen.posFromEdge, CityScreen.posFromEdge,
                com.badlogic.gdx.utils.Align.bottomRight
            )
        }
    }

    private fun constructionsReady(screen: CityScreen): Boolean {
        val actors = ArrayList<com.badlogic.gdx.scenes.scene2d.Actor>()
        val visited = HashSet<com.badlogic.gdx.scenes.scene2d.Actor>()
        fun collect(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            if (!visited.add(actor)) return
            actors.add(actor)
            if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                actor.children.toList().forEach { collect(it) }
            if (actor is com.unciv.ui.utils.ExpanderTab) collect(actor.innerTable)
        }
        collect(screen.stage.root)
        val rows = actors.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Table>()
            .filter { it.name?.startsWith("cityConstruction:") == true }
        if (rows.isEmpty()) return false
        screen.stage.actors.filterIsInstance<com.badlogic.gdx.scenes.scene2d.utils.Layout>()
            .forEach { it.validate() }
        val queue = actors.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.Table>()
            .filter { it.name?.startsWith("cityQueueEntry:") == true }
        check(queue.isNotEmpty()) { "City fixture has no rendered construction queue" }
        for (row in rows + queue) {
            row.validate()
            check(row.prefWidth <= screen.stage.width - 10f) {
                "Construction row ${row.name} exceeds scene width: ${row.prefWidth}"
            }
            check(row.prefHeight > 0f)
            if (screen.game.settings.skin == "Minimal") {
                check(row.color == com.badlogic.gdx.graphics.Color.WHITE) {
                    "Construction row tint changes its text and rejection colors"
                }
            }
        }
        if (screen.game.settings.skin == "Minimal") {
            // Проверяем видимую область списка после асинхронного наполнения, а не отступ контейнера.
            val lower = checkNotNull(
                screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityConstructionLowerPanel")
            )
            val upper = checkNotNull(
                screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityConstructionUpperPanel")
            )
            val scroll =
                    lower.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>()
                        .single()
            scroll.validate()
            val bottom = scroll.localToStageCoordinates(Vector2(0f, 0f))
            val top = scroll.localToStageCoordinates(Vector2(scroll.width, scroll.height))
            check(scroll.height > 0f) { "Construction list has no visible area" }
            check(top.y <= upper.y + 1f) { "Construction list overlaps the queue panel" }
            for (army in screen.stage.actors.filterIsInstance<com.unciv.ui.army.ArmyView>()) {
                if (!army.isVisible) continue
                val armyBottom = army.localToStageCoordinates(Vector2(0f, 0f))
                val armyTop = army.localToStageCoordinates(Vector2(army.width, army.height))
                val overlapX = minOf(top.x, armyTop.x) - maxOf(bottom.x, armyBottom.x)
                if (overlapX > 0f)
                    check(bottom.y >= armyTop.y - 1f) { "Construction list overlaps a city army" }
            }
        }
        verifyStatsPanel(screen)
        return true
    }
    private fun verifyCitizenManagement(screen: CityScreen, city: CityInfo) {
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val focus = city.cityAIFocus
        val avoidGrowth = city.avoidGrowth
        val worked = city.workedTiles.toSet()
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                val content = com.unciv.ui.cityscreen.CitizenManagementTable(screen)
                var changes = 0
                val expander = content.asExpander { changes++ }
                val initiallyOpen = expander.isOpen
                screen.stage.addActor(expander)
                try {
                    expander.pack()
                    check(expander.width <= width - 10f) {
                        "Citizen management exceeds scene width: ${expander.width} at $width"
                    }
                    expander.toggle()
                    expander.act(1f)
                    expander.pack()
                    check(expander.isOpen != initiallyOpen)
                    check(changes == 1)
                    check((expander.innerTable.parent != null) == expander.isOpen)
                    expander.toggle()
                    expander.act(1f)
                    expander.pack()
                    check(expander.isOpen == initiallyOpen)
                    check(changes == 2)
                    check((expander.innerTable.parent != null) == expander.isOpen)
                    check(expander.width <= width - 10f)
                } finally {
                    if (expander.isOpen != initiallyOpen) {
                        expander.toggle()
                        expander.act(1f)
                    }
                    expander.remove()
                }
            }
            check(city.cityAIFocus == focus && city.avoidGrowth == avoidGrowth)
            check(city.workedTiles.toSet() == worked)
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
        }
    }
    private fun addVisitingHero(city: CityInfo) {
        val ruleset = city.getRuleset()
        val hero = checkNotNull(city.civInfo.placeUnitNearTile(city.location, "Warrior"))
        check(city.getVisitingHero() === hero) { "Fixture hero was not placed in the city" }
        hero.hero = com.unciv.pure.domain.hero.HeroFactory.create(
            source = com.unciv.pure.infrastructure.hero.RulesetHeroDefinitionSource(ruleset),
            unitName = "Warrior", currentFood = 10f, morale = 2, luck = 3
        )
        hero.army = com.unciv.logic.army.ArmyInfo(city.civInfo, 7).apply {
            setTroopAt(
                0, com.unciv.pure.domain.troop.TroopFactory.create(
                    "Spearman",
                    10,
                    com.unciv.pure.domain.troop.RulesetTroopDefinitionSource(ruleset)
                )
            )
        }
        hero.army.setTransients(city.civInfo, hero)
    }
    private fun verifyHeroSupply(screen: CityScreen, city: CityInfo) {
        val hero = checkNotNull(city.getVisitingHero())
        val cityFood = city.population.foodStored
        val heroFood = hero.hero.currentFood
        val autoFeed = city.autoFeedHero
        val amounts = hero.army.getAllTroops().map { it?.amount }
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                val panel = com.unciv.ui.cityscreen.HeroSupplyTable(screen)
                screen.stage.addActor(panel)
                try {
                    panel.update()
                    panel.validate()
                    check(panel.width <= width - 10f) {
                        "Hero supply exceeds scene width: ${panel.width} at $width"
                    }
                    check(panel.height > 0f)
                    check(city.population.foodStored == cityFood && hero.hero.currentFood == heroFood) {
                        "Building hero supply UI changed food reserves"
                    }
                    panel.update()
                    check(city.population.foodStored == cityFood && hero.hero.currentFood == heroFood)
                } finally {
                    panel.remove()
                }
            }
            check(city.autoFeedHero == autoFeed)
            check(hero.army.getAllTroops().map { it?.amount } == amounts)
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
        }
    }
    private fun verifySpecialists(screen: CityScreen, city: CityInfo) {
        val capacities = city.population.getMaxSpecialists()
        check(!capacities.isEmpty()) { "Fixture has no specialist slots" }
        val specialistName = capacities.keys.first()
        fun assignments() =
                capacities.keys.associateWith { city.population.getNewSpecialists()[it] }

        val originalAssignments = assignments()
        val originalManual = city.manualSpecialists
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (manual in listOf(false, true)) {
                    city.manualSpecialists = manual
                    val panel = com.unciv.ui.cityscreen.SpecialistAllocationTable(screen)
                    val expander = panel.asExpander(null)
                    screen.stage.addActor(expander)
                    try {
                        expander.pack()
                        check(expander.width <= width - 10f) {
                            "Specialists exceed scene width: ${expander.width} at $width"
                        }
                        check(assignments() == originalAssignments)
                        check(city.manualSpecialists == manual)
                        val manySlots = panel.getAllocationTable(12, 31, specialistName)
                        manySlots.pack()
                        if (screen.game.settings.skin == "Minimal") {
                            check(manySlots.rows > 1) { "Specialist slots do not wrap" }
                            check(manySlots.width <= width - 100f)
                        }
                    } finally {
                        expander.remove()
                    }
                }
            }
        } finally {
            city.manualSpecialists = originalManual
            viewport.setWorldSize(oldWidth, oldHeight)
        }
        check(assignments() == originalAssignments)
    }
    private fun prepareReligion(city: CityInfo) {
        val world = city.civInfo.gameInfo
        check(world.isReligionEnabled())
        val religion =
                com.unciv.models.Religion("Christianity", world, city.civInfo.civName).apply {
                    displayName =
                            "The fellowship of the distant valleys and the ancient royal cities"
                }
        world.religions[religion.name] = religion
        city.religion.religionThisIsTheHolyCityOf = religion.name
        city.religion.addPressure(religion.name, 1000, shouldUpdateFollowers = false)
        city.religion.setTransients(city)
        check((city.religion.getFollowersOf(religion.name) ?: 0) > 0)
        check((city.religion.getPressuresFromSurroundingCities()[religion.name] ?: 0) > 0)
    }
    private fun verifyReligion(screen: CityScreen, city: CityInfo) {
        fun followers() = city.religion.getNumberOfFollowers().let { values ->
            values.keys.associateWith { values[it] }
        }

        fun pressures() = city.religion.getPressures().let { values ->
            values.keys.associateWith { values[it] }
        }

        val originalFollowers = followers()
        val originalPressures = pressures()
        val originalBlocked = city.religion.isBlockedHolyCity
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (blocked in listOf(false, true)) {
                    city.religion.isBlockedHolyCity = blocked
                    val contentWidth =
                            (width / 3f).coerceAtLeast(300f).coerceAtMost(width - 24f) - 20f
                    val panel = com.unciv.ui.cityscreen.CityReligionInfoTable(
                        city.religion, showMajority = true, parchmentWidth = contentWidth
                    )
                    val expander = panel.asExpander(null)
                    val originallyOpen = expander.isOpen
                    screen.stage.addActor(expander)
                    try {
                        if (!expander.isOpen) {
                            expander.toggle()
                            expander.act(1f)
                        }
                        expander.pack()
                        check(expander.width <= width - 10f) {
                            "Religion panel exceeds scene width: ${expander.width} at $width"
                        }
                        check(panel.height > 0f)
                        check(followers() == originalFollowers && pressures() == originalPressures)
                    } finally {
                        if (expander.isOpen != originallyOpen) {
                            expander.toggle()
                            expander.act(1f)
                        }
                        expander.remove()
                    }
                }
            }
            val legacy = com.unciv.ui.cityscreen.CityReligionInfoTable(city.religion, true)
            legacy.pack()
            check(legacy.columns == 5) { "Map religion table no longer uses its original grid" }
            check(followers() == originalFollowers && pressures() == originalPressures)
        } finally {
            city.religion.isBlockedHolyCity = originalBlocked
            viewport.setWorldSize(oldWidth, oldHeight)
        }
    }

    /** Проверяет ограничение всей панели, раскрытие разделов и доступ к концу содержимого. */
    private fun verifyStatsPanel(screen: CityScreen) {
        if (screen.game.settings.skin != "Minimal") return
        val panel = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.CityStatsTable>().single()
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val city = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.CityScreenTileTable>().single().city
        val food = city.population.foodStored
        val worked = city.workedTiles.toSet()
        val manual = city.manualSpecialists
        try {
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                for (availableHeight in listOf(180f, 470f)) {
                    panel.update(availableHeight)
                    panel.constrainPanelHeight(availableHeight)
                    val scroll = checkNotNull(
                        panel.findActor<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>("cityStatsScroll")
                    )
                    val expanders = ArrayList<com.unciv.ui.utils.ExpanderTab>()
                    fun collect(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                        if (actor is com.unciv.ui.utils.ExpanderTab) expanders.add(actor)
                        if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                            actor.children.toList().forEach { collect(it) }
                    }
                    collect(scroll.actor)
                    check(expanders.isNotEmpty())
                    val initiallyClosed = expanders.filter { !it.isOpen }
                    try {
                        for (expander in initiallyClosed) {
                            expander.toggle()
                            expander.act(1f)
                        }
                        panel.constrainPanelHeight(availableHeight)
                        panel.validate()
                        scroll.validate()
                        check(panel.width <= width - 10f) {
                            "City statistics exceed scene width: ${panel.width} at $width"
                        }
                        check(panel.height <= availableHeight + 1f) {
                            "City statistics exceed allocated height: ${panel.height} > $availableHeight"
                        }
                        check(scroll.height > 0f)
                        val content = scroll.actor as com.badlogic.gdx.scenes.scene2d.ui.Table
                        check(content.width >= content.prefWidth - 1f) { "Statistics content is clipped horizontally" }
                        check(scroll.maxX <= 1f)
                        if (availableHeight == 180f)
                            check(scroll.maxY > 0f) { "Short panel did not exercise scrolling" }
                        scroll.scrollPercentY = 1f
                        scroll.updateVisualScroll()
                        scroll.layout()
                        val bottom = content.localToAscendantCoordinates(scroll, Vector2.Zero.cpy())
                        check(bottom.y >= -1f && bottom.y <= scroll.height) {
                            "Bottom of city statistics cannot be reached"
                        }
                    } finally {
                        for (expander in initiallyClosed) {
                            if (expander.isOpen) {
                                expander.toggle()
                                expander.act(1f)
                            }
                        }
                    }
                }
            }
            check(city.population.foodStored == food)
            check(city.workedTiles.toSet() == worked && city.manualSpecialists == manual)
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            screen.updateCityStatsTable()
            panel.findActor<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>("cityStatsScroll")
                ?.apply {
                    scrollY = 0f
                    updateVisualScroll()
                }
        }
    }

    /** Наполняет разделы событий и великих людей существующими определениями ruleset. */
    private fun prepareProductionSections(city: CityInfo) {
        val ruleset = city.getRuleset()
        // Чтение по ключу использует преобразование Float из JSON в Int внутри Counter.get.
        val building = ruleset.buildings.values
            .filter { building ->
                building.greatPersonPoints.keys.any { key ->
                    (building.greatPersonPoints[key] ?: 0) > 0
                }
            }
            .minByOrNull { it.name }
            ?: error("Ruleset has no building producing great person points")
        if (!city.cityConstructions.isBuilt(building.name))
            city.cityConstructions.addBuilding(building.name)
        check(city.getGreatPersonPointsForNextTurn().values.any { counter ->
            counter.keys.any { (counter[it] ?: 0) > 0 }
        }) { "Fixture has no positive great person point income" }

        val event = ruleset.cityEvents.values.sortedBy { it.name }
            .maxByOrNull { it.name.length } ?: error("Ruleset has no city events")
        city.cityConstructions.addCityEvent(event.name)
        check(event.name in city.cityConstructions.preparedCityEvents)
    }

    /** Проверяет перенос длинного имени города без открытия диалога переименования. */
    private fun verifyCityPicker(screen: CityScreen, city: CityInfo) {
        if (screen.game.settings.skin != "Minimal") return
        val picker = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.CityScreenCityPickerTable>().single()
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        val originalName = city.name
        val oldX = picker.x
        val oldY = picker.y
        val testName =
                "The ancient royal city beyond the northern mountains and the distant river valleys"
        try {
            city.name = testName
            for (width in listOf(360f, 1000f)) {
                viewport.setWorldSize(width, 480f)
                picker.update()
                picker.validate()
                check(picker.width <= width - 24f + 1f) {
                    "City picker exceeds scene width: ${picker.width} at $width"
                }
                val labels = ArrayList<com.badlogic.gdx.scenes.scene2d.ui.Label>()
                fun collect(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                    if (actor is com.badlogic.gdx.scenes.scene2d.ui.Label) labels.add(actor)
                    if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                        actor.children.toList().forEach { collect(it) }
                }
                collect(picker)
                val label = labels.single { it.text.toString() == testName }
                label.validate()
                check(label.glyphLayout.runs.size > 1) { "Long city name did not wrap" }
                val bottomLeft = label.localToAscendantCoordinates(picker, Vector2(0f, 0f))
                val topRight = label.localToAscendantCoordinates(
                    picker,
                    Vector2(label.width, label.height)
                )
                check(bottomLeft.x >= -1f && bottomLeft.y >= -1f)
                check(topRight.x <= picker.width + 1f && topRight.y <= picker.height + 1f) {
                    "City name lies outside the picker"
                }
                check(city.name == testName) { "Building city picker changed the city name" }
            }
        } finally {
            city.name = originalName
            viewport.setWorldSize(oldWidth, oldHeight)
            picker.update()
            picker.setPosition(oldX, oldY)
        }
    }

    /** Проверяет перенос слотов, сохранение их порядка и локальность выделения без игровых действий. */
    private fun verifyArmySlotAppearance(screen: CityScreen, city: CityInfo) {
        val hero = checkNotNull(city.getVisitingHero())
        val heroAmounts = hero.army.getAllTroops().map { it?.amount }
        val garrisonAmounts = city.garrisonInfo.getAllTroops().map { it?.amount }
        val armies = screen.stage.actors.filterIsInstance<com.unciv.ui.army.ArmyView>()
        check(armies.size >= 2) { "Fixture must display both city armies" }
        fun slots(army: com.unciv.ui.army.ArmyView) =
                army.children.filterIsInstance<com.unciv.ui.army.TroopArmyView>()

        fun displayedAmounts(army: com.unciv.ui.army.ArmyView) = slots(army).map {
            if (it.isEmptySlot()) null
            else checkNotNull(it.findActor<com.badlogic.gdx.scenes.scene2d.ui.Label>("amountLabel")).text.toString()
        }

        val originalSlots = armies.map { displayedAmounts(it) }
        val viewport = screen.stage.viewport
        val oldWidth = viewport.worldWidth
        val oldHeight = viewport.worldHeight
        try {
            if (screen.game.settings.skin == "Minimal") {
                for (width in listOf(360f, 1000f)) {
                    viewport.setWorldSize(width, 480f)
                    for ((index, army) in armies.withIndex()) {
                        // Только перестроение представления, без обратного вызова обновления города.
                        army.updateView(initialized = false)
                        army.validate()
                        check(army.width <= width - 10f) { "City army exceeds scene width" }
                        check(displayedAmounts(army) == originalSlots[index]) { "Army slot order changed" }
                        val troopSlots = slots(army)
                        if (troopSlots.size * 74f > army.width + 1f)
                            check(troopSlots.map { it.y }
                                .distinct().size > 1) { "Army slots did not wrap" }
                        for ((slotIndex, slot) in troopSlots.withIndex()) {
                            check(slot.x >= -1f && slot.y >= -1f)
                            check(slot.x + slot.width <= army.width + 1f)
                            check(slot.y + slot.height <= army.height + 1f)
                            for (other in troopSlots.drop(slotIndex + 1)) {
                                val overlapX =
                                        minOf(slot.x + slot.width, other.x + other.width) - maxOf(
                                            slot.x,
                                            other.x
                                        )
                                val overlapY =
                                        minOf(slot.y + slot.height, other.y + other.height) - maxOf(
                                            slot.y,
                                            other.y
                                        )
                                check(overlapX <= 0f || overlapY <= 0f) { "Army slots overlap" }
                            }
                        }
                    }
                }
            }
            val allSlots = armies.flatMap { slots(it) }
            val selectedSlot = allSlots.first { !it.isEmptySlot() }
            val backgrounds = allSlots.map {
                checkNotNull(it.findActor<com.badlogic.gdx.scenes.scene2d.ui.Image>("backgroundImage"))
            }
            val colors = backgrounds.map { it.color.cpy() }
            val drawables = backgrounds.map { it.drawable }
            val selectedIndex = allSlots.indexOf(selectedSlot)
            try {
                selectedSlot.select()
                check(backgrounds[selectedIndex].color != colors[selectedIndex]) {
                    "Selecting a troop does not change its background"
                }
                for (index in backgrounds.indices) {
                    check(backgrounds[index].drawable === drawables[index]) {
                        "Selection replaces the shared background resource"
                    }
                    if (index != selectedIndex)
                        check(backgrounds[index].color == colors[index]) { "Selection recolors another slot" }
                }
                selectedSlot.deselect()
                for (index in backgrounds.indices) check(backgrounds[index].color == colors[index])
            } finally {
                selectedSlot.deselect()
            }
            check(hero.army.getAllTroops().map { it?.amount } == heroAmounts)
            check(city.garrisonInfo.getAllTroops().map { it?.amount } == garrisonAmounts)
        } finally {
            viewport.setWorldSize(oldWidth, oldHeight)
            armies.forEach { it.updateView(initialized = false) }
        }
    }

    /** Проверяет границы подробной статистики и доступ к таблице через прокрутку. */
    private fun verifyDetailedStats(screen: CityScreen) {
        if (screen.game.settings.skin != "Minimal") return
        for (width in listOf(360f, 1000f)) {
            val stage = com.unciv.ui.UncivStage(
                com.badlogic.gdx.utils.viewport.FitViewport(width, 480f)
            )
            try {
                check(stage.width == width && stage.height == 480f)
                val popup = com.unciv.ui.cityscreen.DetailedStatsPopup(screen, stage)
                try {
                    popup.open()
                    popup.validate()
                    popup.innerTable.validate()
                    val scroll = checkNotNull(
                        popup.innerTable.findActor<com.badlogic.gdx.scenes.scene2d.ui.ScrollPane>("detailedStatsScroll")
                    )
                    val close = checkNotNull(
                        popup.innerTable.findActor<com.badlogic.gdx.scenes.scene2d.ui.TextButton>("detailedStatsClose")
                    )
                    scroll.validate()
                    fun checkBounds(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
                        val bottom = actor.localToStageCoordinates(Vector2(0f, 0f))
                        val top = actor.localToStageCoordinates(Vector2(actor.width, actor.height))
                        check(bottom.x >= -1f && bottom.y >= -1f && top.x <= width + 1f && top.y <= 481f) {
                            "Detailed statistics actor ${actor.name} exceeds ${width}x480: $bottom .. $top"
                        }
                    }
                    checkBounds(popup.innerTable)
                    checkBounds(scroll)
                    checkBounds(close)
                    check(scroll.width > 0f && scroll.height > 0f)
                    check(close.height >= 48f)
                    val closeTop = close.localToStageCoordinates(Vector2(0f, close.height))
                    val scrollBottom = scroll.localToStageCoordinates(Vector2(0f, 0f))
                    check(closeTop.y <= scrollBottom.y + 1f)
                    if (width == 360f) check(scroll.maxX > 0f) {
                        "Narrow statistics table did not exercise horizontal scrolling"
                    }
                    scroll.scrollPercentX = 1f
                    scroll.scrollPercentY = 1f
                    scroll.updateVisualScroll()
                    scroll.layout()
                    val content = scroll.actor
                    val end =
                            content.localToAscendantCoordinates(scroll, Vector2(content.width, 0f))
                    check(end.x >= -1f && end.x <= scroll.width + 1f)
                    check(end.y >= -1f && end.y <= scroll.height + 1f)
                    val click = close.localToStageCoordinates(
                        Vector2(
                            close.width / 2f,
                            close.height / 2f
                        )
                    )
                    stage.stageToScreenCoordinates(click)
                    stage.touchDown(
                        click.x.toInt(),
                        click.y.toInt(),
                        0,
                        com.badlogic.gdx.Input.Buttons.LEFT
                    )
                    stage.touchUp(
                        click.x.toInt(),
                        click.y.toInt(),
                        0,
                        com.badlogic.gdx.Input.Buttons.LEFT
                    )
                    check(popup.parent == null) { "Detailed statistics close button did not close the popup" }
                } finally {
                    if (popup.parent != null) popup.close()
                }
            } finally {
                stage.dispose()
            }
        }
    }

    /** Объединение счастья для отображения не должно записывать его в модель города. */
    private fun verifyDetailedStatsModel(screen: CityScreen) {
        val city = screen.stage.actors
            .filterIsInstance<com.unciv.ui.cityscreen.CityScreenTileTable>().single().city
        val stats = city.cityStats
        val source = "Royal statistics isolation check"
        check(source !in stats.finalStatList && source !in stats.baseStatTree.children && source !in stats.happinessList)
        val finalValues = com.unciv.models.stats.Stats(gold = 3f)
        val node = com.unciv.logic.city.StatTreeNode().apply {
            setInnerStat(com.unciv.models.stats.Stat.Gold, 3f)
            addStats(com.unciv.models.stats.Stats(science = 2f), "Nested source")
        }
        stats.finalStatList[source] = finalValues
        stats.baseStatTree.children[source] = node
        stats.happinessList[source] = 6f
        fun treeSnapshot(root: com.unciv.logic.city.StatTreeNode): Map<List<String>, List<Float>> {
            val result = linkedMapOf<List<String>, List<Float>>()
            fun visit(current: com.unciv.logic.city.StatTreeNode, path: List<String>) {
                result[path] = current.totalStats.values.toList()
                current.children.forEach { (name, child) -> visit(child, path + name) }
            }
            visit(root, emptyList())
            return result
        }

        val finalBefore = stats.finalStatList.mapValues { it.value.values.toList() }
        val baseBefore = treeSnapshot(stats.baseStatTree)
        val bonusesBefore = treeSnapshot(stats.statPercentBonusTree)
        val happinessBefore = stats.happinessList.toMap()
        try {
            repeat(2) {
                val popup = com.unciv.ui.cityscreen.DetailedStatsPopup(screen)
                try {
                    popup.open()
                    popup.validate()
                    check(stats.finalStatList.mapValues { it.value.values.toList() } == finalBefore) {
                        "Opening detailed statistics changed finalStatList"
                    }
                    check(treeSnapshot(stats.baseStatTree) == baseBefore) {
                        "Opening detailed statistics changed baseStatTree"
                    }
                    check(treeSnapshot(stats.statPercentBonusTree) == bonusesBefore)
                    check(stats.happinessList == happinessBefore)
                    check(finalValues.happiness == 0f && node.totalStats.happiness == 0f)
                } finally {
                    if (popup.parent != null) popup.close()
                }
            }
        } finally {
            stats.finalStatList.remove(source)
            stats.baseStatTree.children.remove(source)
            stats.happinessList.remove(source)
        }
    }

    fun compactLoadingScreen(onReady: (BaseScreen) -> Unit): BaseScreen = object : BaseScreen() {
        private var started = false
        override fun show() {
            if (started) return
            started = true
            com.unciv.utils.concurrency.Concurrency.run("Compact city golden setup") {
                launchOnGLThread {
                    val world = createWorld()
                    game.loadGame(world)
                    val city = world.currentPlayerCiv.cities.single()
                    addVisitingHero(city)
                    val previousSize = game.settings.screenSize
                    val screen = try {
                        game.settings.screenSize = com.unciv.models.metadata.ScreenSize.Tiny
                        CityScreen(
                            city,
                            initSelectedConstruction = world.ruleSet.buildings.getValue("Granary")
                        )
                    } finally {
                        game.settings.screenSize = previousSize
                    }
                    game.pushScreen(screen)
                    check(screen.stage.width < 900f)
                    screen.stage.root.addAction(object : com.badlogic.gdx.scenes.scene2d.Action() {
                        override fun act(delta: Float): Boolean {
                            if (!compactCityReady(screen, city)) return false
                            verifyCompactTileSelection(screen, city)
                            verifyCompactImprovementSelection(screen, city)
                            onReady(screen)
                            return true
                        }
                    })
                }
            }
        }
    }

    /** Проверяет страницы целого компактного экрана после асинхронной загрузки построек. */
    private fun compactCityReady(screen: CityScreen, city: CityInfo): Boolean {
        val root =
                checkNotNull(screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityCompactRoot"))
        val pager = checkNotNull(root.findActor<com.unciv.ui.utils.TabbedPager>("cityCompactPager"))
        pager.selectPage("Construction")
        root.validate()
        val actors = ArrayList<com.badlogic.gdx.scenes.scene2d.Actor>()
        fun collect(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            actors.add(actor)
            if (actor is com.badlogic.gdx.scenes.scene2d.Group) actor.children.toList()
                .forEach { collect(it) }
            if (actor is com.unciv.ui.utils.ExpanderTab && !actor.isOpen) collect(actor.innerTable)
        }
        collect(root)
        if (actors.none { it.name?.startsWith("cityConstruction:") == true }) return false
        val gold = city.civInfo.gold
        val food = city.population.foodStored
        val worked = city.workedTiles.toSet()
        val queue = city.cityConstructions.constructionQueue.toList()
        val garrison = city.garrisonInfo.getAllTroops()
            .map { it?.let { troop -> troop.unitName to troop.amount } }
        val visitor = city.getVisitingHero()?.army?.getAllTroops()
            ?.map { it?.let { troop -> troop.unitName to troop.amount } }
        check(pager.pageCount() == 5)
        fun assertBounds(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            val bottom = actor.localToStageCoordinates(Vector2.Zero.cpy())
            val top = actor.localToStageCoordinates(Vector2(actor.width, actor.height))
            check(bottom.x >= -1f && bottom.y >= -1f && top.x <= screen.stage.width + 1f && top.y <= screen.stage.height + 1f) {
                "Compact city actor ${actor.name}: $bottom .. $top outside ${screen.stage.width}x${screen.stage.height}"
            }
        }
        for (caption in listOf("Map", "Construction", "Stats", "Army", "Details", "Map")) {
            val index = pager.getPageIndex(caption)
            val button = pager.getPageButton(index)
            var ancestor = button.parent
            while (ancestor != null && ancestor !is com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
                ancestor = ancestor.parent
            val headerScroll =
                    checkNotNull(ancestor as? com.badlogic.gdx.scenes.scene2d.ui.ScrollPane)
            headerScroll.validate()
            headerScroll.scrollX = (button.x + button.width / 2f - headerScroll.scrollWidth / 2f)
                .coerceIn(0f, headerScroll.maxX)
            headerScroll.updateVisualScroll()
            headerScroll.layout()
            val click =
                    button.localToStageCoordinates(Vector2(button.width / 2f, button.height / 2f))
            screen.stage.stageToScreenCoordinates(click)
            screen.stage.touchDown(
                click.x.toInt(),
                click.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            screen.stage.touchUp(
                click.x.toInt(),
                click.y.toInt(),
                0,
                com.badlogic.gdx.Input.Buttons.LEFT
            )
            root.validate()
            pager.validate()
            check(pager.activePage == index) { "Click did not activate compact city page $caption" }
            assertBounds(pager)
            val exit =
                    root.children.filterIsInstance<com.badlogic.gdx.scenes.scene2d.ui.TextButton>()
                        .single()
            assertBounds(exit)
            check(exit.top <= pager.y + 1f)
            check(pager.height > 100f)
            if (caption == "Map") {
                val mapPage =
                        checkNotNull(pager.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityCompactMap"))
                val map = mapPage.children.single()
                assertBounds(map)
                check(map.width > 100f && map.height > 100f)
            }
            if (caption == "Army") {
                actors.clear()
                collect(pager)
                val armies = actors.filterIsInstance<com.unciv.ui.army.ArmyView>()
                check(armies.size == 2)
                val first = armies[0].localToStageCoordinates(Vector2.Zero.cpy())
                val secondTop = armies[1].localToStageCoordinates(Vector2(0f, armies[1].height))
                check(first.y >= secondTop.y - 1f) { "Compact city armies overlap" }
            }
        }
        check(city.civInfo.gold == gold && city.population.foodStored == food)
        check(city.workedTiles.toSet() == worked)
        check(city.cityConstructions.constructionQueue.toList() == queue)
        check(
            city.garrisonInfo.getAllTroops()
                .map { it?.let { troop -> troop.unitName to troop.amount } } == garrison)
        check(
            city.getVisitingHero()?.army?.getAllTroops()
                ?.map { it?.let { troop -> troop.unitName to troop.amount } } == visitor)
        pager.selectPage("Construction")
        root.validate()
        return true
    }

    /** Нажатие доступной клетки карты открывает сведения без изменения хозяйства города. */
    private fun verifyCompactTileSelection(screen: CityScreen, city: CityInfo) {
        val root =
                checkNotNull(screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityCompactRoot"))
        val pager = checkNotNull(root.findActor<com.unciv.ui.utils.TabbedPager>("cityCompactPager"))
        pager.selectPage("Map")
        root.validate()
        val mapPage =
                checkNotNull(pager.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityCompactMap"))
        val map =
                mapPage.children.filterIsInstance<com.unciv.ui.utils.ZoomableScrollPane>().single()
        map.validate()
        map.updateVisualScroll()
        map.layout()
        val tiles = ArrayList<com.unciv.ui.cityscreen.CityTileGroup>()
        fun collect(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
            if (actor is com.unciv.ui.cityscreen.CityTileGroup) tiles.add(actor)
            if (actor is com.badlogic.gdx.scenes.scene2d.Group) actor.children.toList()
                .forEach { collect(it) }
        }
        collect(map)
        val tile = tiles.firstOrNull { candidate ->
            if (candidate.isWorkable) false
            else {
                val point = candidate.localToStageCoordinates(
                    Vector2(
                        candidate.width / 2f,
                        candidate.height / 2f
                    )
                )
                val hit = screen.stage.hit(point.x, point.y, true)
                hit != null && (hit === candidate || hit.isDescendantOf(candidate))
            }
        } ?: error("Compact map has no visible non-workable tile for selection")
        val gold = city.civInfo.gold
        val food = city.population.foodStored
        val worked = city.workedTiles.toSet()
        val locked = city.lockedTiles.toSet()
        val queue = city.cityConstructions.constructionQueue.toList()
        val click = tile.localToStageCoordinates(Vector2(tile.width / 2f, tile.height / 2f))
        screen.stage.stageToScreenCoordinates(click)
        screen.stage.touchDown(
            click.x.toInt(),
            click.y.toInt(),
            0,
            com.badlogic.gdx.Input.Buttons.LEFT
        )
        screen.stage.touchUp(
            click.x.toInt(),
            click.y.toInt(),
            0,
            com.badlogic.gdx.Input.Buttons.LEFT
        )
        root.validate()
        check(screen.selectedTile === tile.tileInfo) { "Compact map click did not select the tile" }
        check(pager.activePage == pager.getPageIndex("Details"))
        fun findCard(actor: com.badlogic.gdx.scenes.scene2d.Actor): com.unciv.ui.cityscreen.CityScreenTileTable? {
            if (actor is com.unciv.ui.cityscreen.CityScreenTileTable) return actor
            if (actor is com.badlogic.gdx.scenes.scene2d.Group)
                for (child in actor.children) findCard(child)?.let { return it }
            return null
        }

        val card = checkNotNull(findCard(pager))
        check(card.isVisible && card.width > 0f && card.height > 0f)
        check(city.civInfo.gold == gold && city.population.foodStored == food)
        check(city.workedTiles.toSet() == worked && city.lockedTiles.toSet() == locked)
        check(city.cityConstructions.constructionQueue.toList() == queue)
    }

    /** Вход и отмена выбора места улучшения сохраняют данные и возвращают страницу карты. */
    private fun verifyCompactImprovementSelection(screen: CityScreen, city: CityInfo) {
        val improvement = city.getRuleset().tileImprovements.getValue("Farm")
        val building = com.unciv.models.ruleset.Building().apply {
            name = "Royal improvement navigation check"
            uniques.add("Creates a [${improvement.name}] improvement on a specific tile")
        }
        check(building.getImprovementToCreate(city.getRuleset()) === improvement)
        val root =
                checkNotNull(screen.stage.root.findActor<com.badlogic.gdx.scenes.scene2d.ui.Table>("cityCompactRoot"))
        val pager = checkNotNull(root.findActor<com.unciv.ui.utils.TabbedPager>("cityCompactPager"))
        val gold = city.civInfo.gold
        val queue = city.cityConstructions.constructionQueue.toList()
        val improvements = city.tilesInRange.associate { it.position.cpy() to it.improvement }
        val existingToasts =
                screen.stage.actors.filterIsInstance<com.unciv.ui.popup.ToastPopup>().toSet()
        try {
            for (buying in listOf(false, true)) {
                pager.selectPage("Details")
                screen.startPickTileForCreatesOneImprovement(
                    building,
                    com.unciv.models.stats.Stat.Gold,
                    buying
                )
                root.validate()
                check(pager.activePage == pager.getPageIndex("Map"))
                val selection = checkNotNull(screen.pickTileData)
                check(selection.building === building && selection.improvement === improvement)
                check(selection.isBuying == buying)
                screen.stopPickTileForCreatesOneImprovement()
                check(screen.pickTileData == null)
                check(city.civInfo.gold == gold)
                check(city.cityConstructions.constructionQueue.toList() == queue)
                check(city.tilesInRange.associate { it.position.cpy() to it.improvement } == improvements)
                screen.stage.actors.filterIsInstance<com.unciv.ui.popup.ToastPopup>()
                    .filter { it !in existingToasts }.forEach { it.close() }
            }
        } finally {
            screen.stopPickTileForCreatesOneImprovement()
            screen.stage.actors.filterIsInstance<com.unciv.ui.popup.ToastPopup>()
                .filter { it !in existingToasts }.forEach { it.close() }
        }
    }

    fun overviewLoadingScreen(onReady: (BaseScreen) -> Unit): BaseScreen = object : BaseScreen() {
        private var started = false
        override fun show() {
            if (started) return
            started = true
            com.unciv.utils.concurrency.Concurrency.run("Overview golden setup") {
                launchOnGLThread {
                    val world = createWorld()
                    val option = com.unciv.models.ruleset.ModOptionsConstants.convertGoldToScience
                    val options = world.ruleSet.modOptions.uniques
                    val added = option !in options
                    if (added) options.add(option)
                    try {
                        game.loadGame(world)
                        val civ = world.currentPlayerCiv
                        addVisitingHero(civ.cities.single())
                        val originalResources = civ.detailedCivResources
                        val supply = com.unciv.models.ruleset.tile.ResourceSupplyList().apply {
                            val iron = world.ruleSet.tileResources.getValue("Iron")
                            add(iron, "Royal supply", 7)
                            add(iron, "Royal consumption", -3)
                        }
                        try {
                            civ.detailedCivResources = supply
                            val screen = com.unciv.ui.overviewscreen.EmpireOverviewScreen(
                                civ,
                                defaultPage = "Stats"
                            )
                            game.pushScreen(screen)
                            OverviewStatsGoldenChecks.verify(screen, civ)
                            OverviewStatsGoldenChecks.verifyInput(screen, civ)
                            OverviewStatsGoldenChecks.verifyCities(screen, civ)
                            OverviewStatsGoldenChecks.verifyResources(screen, supply)
                            OverviewStatsGoldenChecks.verifyTrades(screen, civ)
                            OverviewStatsGoldenChecks.verifyNotifications(screen, civ)
                            OverviewStatsGoldenChecks.verifyUnits(screen, civ)
                            OverviewStatsGoldenChecks.verifyPolitics(screen, civ)
                            OverviewStatsGoldenChecks.verifyReligion(screen, civ)
                            onReady(screen)
                        } finally {
                            civ.detailedCivResources = originalResources
                        }
                    } finally {
                        if (added) options.remove(option)
                    }
                }
            }
        }
    }
}
