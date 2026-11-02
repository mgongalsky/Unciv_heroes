package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.city.CityFlags
import com.unciv.logic.city.CityFocus
import com.unciv.logic.city.CityInfo
import com.unciv.models.UncivSound
import com.unciv.models.ruleset.Building
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.ruleset.unit.BaseUnit
import com.unciv.models.stats.Stat
import com.unciv.models.translations.tr
import com.unciv.pure.domain.supply.SupplyMechanic
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.ImageGetter.ruleset
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.center
import com.unciv.ui.utils.extensions.colorFromRGB
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toGroup
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toPercent
import com.unciv.ui.utils.extensions.toTextButton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import com.unciv.ui.utils.AutoScrollPane as ScrollPane

class CityStatsTable(val cityScreen: CityScreen): Table(), KoinComponent {
    private val supplyMechanic: SupplyMechanic by inject()
    private val innerTable = Table() // table within this Table. Slightly smaller creates border
    private val upperTable = Table() // fixed position table
    private val lowerTable = Table() // table that will be in the ScrollPane
    private val lowerPane: ScrollPane
    private val cityInfo = cityScreen.city
    private val lowerCell: Cell<ScrollPane>

    private val detailedStatsButton = "Stats".toTextButton().apply {
        labelCell.pad(10f)
        onActivation {
            DetailedStatsPopup(cityScreen).open()
        }
    }

    private val testFantasyButton = "Fantasy".toTextButton().apply {
        labelCell.pad(10f)
        onActivation {
            TestFantasyPopup(cityScreen).open()
        }
    }

    init {
        pad(2f)
        background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityStatsTable/Background",
            tintColor = colorFromRGB(194, 180, 131)
        )

        innerTable.pad(5f)
        innerTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityStatsTable/InnerTable",
            tintColor = Color.BLACK.cpy().apply { a = 0.8f }
        )
        innerTable.add(upperTable).row()

        upperTable.defaults().pad(2f)
        lowerTable.defaults().pad(2f)
        lowerPane = ScrollPane(lowerTable)
        lowerPane.setOverscroll(false, false)
        lowerPane.setScrollingDisabled(true, false)
        lowerCell = innerTable.add(lowerPane)

        add(innerTable)
    }

    fun update(height: Float) {
        upperTable.clear()
        lowerTable.clear()
        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 20f).coerceAtLeast(1f)
        if (royal) {
            background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/Background", tintColor = Color.valueOf("A18B5A")
            )
            innerTable.background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
        }
        val miniStatsTable = Table()
        val selected = if (royal) Color.valueOf("C6C394")
        else BaseScreen.skin.get("selection", Color::class.java)
        var displayedStats = 0
        for ((stat, amount) in cityInfo.cityStats.currentCityStats) {
            if (stat == Stat.Faith && !cityInfo.civInfo.gameInfo.isReligionEnabled()) continue
            val icon = Table()
            if (cityInfo.cityAIFocus.stat == stat) {
                icon.add(
                    ImageGetter.getStatIcon(stat.name)
                        .surroundWithCircle(27f, false, color = selected)
                )
                if (cityScreen.canCityBeChanged()) {
                    icon.onClick {
                        cityInfo.cityAIFocus = CityFocus.NoFocus
                        cityInfo.reassignPopulation(); cityScreen.update()
                    }
                }
            } else {
                icon.add(
                    ImageGetter.getStatIcon(stat.name)
                        .surroundWithCircle(27f, false, color = Color.CLEAR)
                )
                if (cityScreen.canCityBeChanged()) {
                    icon.onClick {
                        cityInfo.cityAIFocus = cityInfo.cityAIFocus.safeValueOf(stat)
                        cityInfo.reassignPopulation(); cityScreen.update()
                    }
                }
            }
            miniStatsTable.add(icon).size(27f).padRight(3f)
            val valueToDisplay =
                    if (stat == Stat.Happiness) cityInfo.cityStats.happinessList.values.sum() else amount
            val label = round(valueToDisplay).toInt().toString().toLabel(
                fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
            )
            val cell = miniStatsTable.add(label).padRight(5f)
            if (royal) {
                label.setWrap(true)
                cell.width((contentWidth / 2f - 35f).coerceAtLeast(1f))
                displayedStats++
                if (displayedStats % 2 == 0) miniStatsTable.row()
            }
        }
        upperTable.add(miniStatsTable)
        upperTable.addSeparator(color = if (royal) Color.valueOf("A18B5A") else Color.WHITE)
        for (button in listOf(detailedStatsButton, testFantasyButton)) {
            val cell = upperTable.add(button)
            if (royal) {
                button.label.setWrap(true)
                cell.width(contentWidth).minHeight(48f).fillX()
            }
            cell.row()
        }
        addText()
        addCitizenManagement()
        if (cityScreen.visitingHero != null) addHeroSupplyInfo()
        addGreatPersonPointInfo(cityInfo)
        if (!cityInfo.population.getMaxSpecialists().isEmpty()) addSpecialistInfo()
        if (cityInfo.religion.getNumberOfFollowers()
                    .isNotEmpty() && cityInfo.civInfo.gameInfo.isReligionEnabled()
        )
            addReligionInfo()
        addBuildingsInfo()
        addPreparedCityEvents()

        upperTable.pack()
        lowerTable.pack()
        // Ограничение не должно становиться отрицательным при высокой сводке.
        lowerCell.maxHeight((height - upperTable.height - 8f).coerceAtLeast(0f))
        innerTable.pack()
        pack()
        lowerPane.validate()
        lowerPane.updateVisualScroll()
    }

    private fun onContentResize() {
        if (cityScreen.game.settings.skin == "Minimal")
            constrainPanelHeight(availablePanelHeight)
        else pack()
        val currentStage = stage ?: return
        // Внутри страницы положение задаёт родительская таблица.
        if (parent !== currentStage.root) {
            invalidateHierarchy()
            return
        }
        setPosition(
            currentStage.width - CityScreen.posFromEdge,
            currentStage.height - CityScreen.posFromEdge,
            Align.topRight
        )
    }

    private fun updateTurnsToPopString(): String {
        var turnsToPopString =
                when {
                    cityInfo.isStarving() -> "[${cityInfo.getNumTurnsToStarvation()}] turns to lose population"
                    cityInfo.getRuleset().units[cityInfo.cityConstructions.currentConstructionFromQueue]
                        .let { it != null && it.hasUnique(UniqueType.ConvertFoodToProductionWhenConstructed) }
                    -> "Food converts to production"
                    cityInfo.isGrowing() -> "[${cityInfo.getNumTurnsToNewPopulation()}] turns to new population"
                    else -> "Stopped population growth"
                }.tr()
        turnsToPopString += " (${cityInfo.population.foodStored}${Fonts.food}/${cityInfo.population.getFoodToNextPopulation()}${Fonts.food})"
        return turnsToPopString

    }

    private fun addText() {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 36f).coerceAtLeast(1f)
        val target = if (royal) Table().apply {
            background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
            pad(8f)
        } else upperTable
        val ink = if (royal) Color.valueOf("453925") else Color.WHITE
        fun addLine(text: String): Label {
            val label = text.toLabel(fontColor = ink)
            val cell = target.add(label)
            if (royal) {
                label.setWrap(true)
                cell.width(contentWidth)
            }
            cell.row()
            return label
        }

        val unassigned = "{Unassigned population}: ".tr() +
                cityInfo.population.getFreePopulation()
                    .toString() + "/" + cityInfo.population.population
        val unassignedLabel = addLine(unassigned)
        if (cityScreen.canChangeState)
            unassignedLabel.onClick { cityInfo.reassignPopulation(); cityScreen.update() }

        var expansion =
                if (cityInfo.cityStats.currentCityStats.culture > 0 && cityInfo.expansion.getChoosableTiles()
                            .any()
                ) {
                    val remaining =
                            cityInfo.expansion.getCultureToNextTile() - cityInfo.expansion.cultureStored
                    val turns =
                            ceil(remaining / cityInfo.cityStats.currentCityStats.culture).toInt()
                                .coerceAtLeast(1)
                    "[$turns] turns to expansion".tr()
                } else "Stopped expansion".tr()
        if (cityInfo.expansion.getChoosableTiles().any())
            expansion += " (${cityInfo.expansion.cultureStored}${Fonts.culture}/${cityInfo.expansion.getCultureToNextTile()}${Fonts.culture})"
        addLine(expansion)
        addLine(updateTurnsToPopString())
        addLine(
            "Garrison consumes ${
                ceil(
                    cityScreen.city.garrisonInfo.calculateFoodMaintenance(
                        isInCity = true
                    )
                ).toInt()
            }${Fonts.food} each turn."
        )

        val statuses = Table()
        statuses.defaults().pad(2f)
        fun addStatus(icon: Actor, label: Label) {
            statuses.add(icon).size(20f).padRight(5f)
            val cell = statuses.add(label)
            if (royal) {
                label.setWrap(true)
                cell.width((contentWidth - 33f).coerceAtLeast(1f))
            }
            cell.row()
        }
        if (cityInfo.isInResistance()) {
            addStatus(
                ImageGetter.getImage("StatIcons/Resistance"),
                "In resistance for another [${cityInfo.getFlag(CityFlags.Resistance)}] turns".toLabel(
                    fontColor = if (royal) Color.valueOf("8C2923") else Color.WHITE
                )
            )
        }
        val (wltkIcon: Actor?, wltkLabel: Label?) = when {
            cityInfo.isWeLoveTheKingDayActive() -> ImageGetter.getStatIcon("Food") to
                    "We Love The King Day for another [${cityInfo.getFlag(CityFlags.WeLoveTheKing)}] turns".toLabel(
                        if (royal) Color.valueOf("49612D") else Color.LIME
                    )

            cityInfo.demandedResource.isNotEmpty() -> ImageGetter.getResourceImage(
                cityInfo.demandedResource,
                20f
            ) to
                    "Demanding [${cityInfo.demandedResource}]".toLabel(
                        if (royal) Color.valueOf("8C4623") else Color.CORAL
                    )

            else -> null to null
        }
        if (wltkLabel != null) {
            wltkLabel.onClick {
                UncivGame.Current.pushScreen(
                    CivilopediaScreen(
                        cityInfo.getRuleset(),
                        link = "We Love The King Day"
                    )
                )
            }
            addStatus(wltkIcon!!, wltkLabel)
        }
        target.add(statuses).row()
        if (royal) upperTable.add(target).row()
    }

    private fun addCitizenManagement() {
        val expanderTab = CitizenManagementTable(cityScreen).asExpander { onContentResize() }
        lowerTable.add(expanderTab).growX().row()
    }

    private fun addHeroSupplyInfo() {
        if (!supplyMechanic.isEnabled) return
        val expanderTab = HeroSupplyTable(cityScreen).asExpander { onContentResize() }
        lowerTable.add(expanderTab).growX().row()
    }

    private fun addSpecialistInfo() {
        val expanderTab = SpecialistAllocationTable(cityScreen).asExpander { onContentResize() }
        lowerTable.add(expanderTab).growX().row()
    }

    private fun addReligionInfo() {
        val parchmentWidth = if (cityScreen.game.settings.skin == "Minimal")
            ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(cityScreen.stage.width - 24f) - 20f).coerceAtLeast(1f)
        else 0f
        val expanderTab = CityReligionInfoTable(cityInfo.religion, parchmentWidth = parchmentWidth)
            .asExpander { onContentResize() }
        lowerTable.add(expanderTab).growX().row()
    }

    private fun addBuildingsInfo() {
        val wonders = mutableListOf<Building>()
        val specialistBuildings = mutableListOf<Building>()
        val otherBuildings = mutableListOf<Building>()
        for (building in cityInfo.cityConstructions.getBuiltBuildings()) {
            when {
                building.isAnyWonder() -> wonders.add(building)
                !building.newSpecialists().isEmpty() -> specialistBuildings.add(building)
                else -> otherBuildings.add(building)
            }
        }
        wonders.sortBy { it.name }
        specialistBuildings.sortBy { it.name }
        otherBuildings.sortBy { it.name }

        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 48f).coerceAtLeast(1f)
        val separatorColor = if (royal) Color.valueOf("A18B5A") else Color.LIGHT_GRAY
        val totalTable = Table()
        lowerTable.addCategory("Buildings", totalTable, false)

        fun addGroup(title: String, buildings: List<Building>) {
            if (buildings.isEmpty()) return
            totalTable.addSeparator(color = separatorColor)
            val label = title.toLabel(
                fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
            ).apply { setAlignment(Align.center) }
            val cell = totalTable.add(label).growX()
            if (royal) {
                label.setWrap(true)
                cell.width(contentWidth)
            }
            totalTable.addSeparator(color = separatorColor)
            val entries = Table()
            for (building in buildings) addBuildingButton(building, entries)
            totalTable.add(entries).growX().right().row()
        }
        if (specialistBuildings.isNotEmpty()) totalTable.add().row()
        addGroup("Specialist Buildings", specialistBuildings)
        addGroup("Wonders", wonders)
        addGroup("Other", otherBuildings)
    }

    private fun addBuildingButton(building: Building, destinationTable: Table) {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val ink = if (royal) Color.valueOf("453925") else Color.WHITE
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 48f).coerceAtLeast(1f)
        val infoWidth = (contentWidth - 60f).coerceAtLeast(1f)
        val button = Table()
        if (royal) button.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
        )
        val info = Table()
        val statsAndSpecialists = Table()
        val icon = ImageGetter.getPortraitImage(building.name, 50f)
        val isFree = building.name in cityScreen.city.civInfo.civConstructions.getFreeBuildings(
            cityScreen.city.id
        )
        val displayName = if (isFree) "{${building.name}} ({Free})" else building.name
        val nameLabel = displayName.toLabel(fontColor = ink, fontSize = Constants.defaultFontSize)
        val nameCell = info.add(nameLabel).padBottom(5f).right()
        if (royal) {
            nameLabel.setWrap(true)
            nameLabel.setAlignment(Align.right)
            nameCell.width(infoWidth)
        }
        nameCell.row()
        val stats = building.getStats(cityInfo).joinToString(separator = " ") {
            "" + it.value.toInt() + it.key.character
        }
        val statsLabel = stats.toLabel(fontColor = ink, fontSize = Constants.defaultFontSize)
        val statsCell = statsAndSpecialists.add(statsLabel).right()
        if (royal) {
            statsLabel.setWrap(true)
            statsLabel.setAlignment(Align.right)
            statsCell.width(infoWidth).row()
        }
        val assignedSpec = cityInfo.population.getNewSpecialists().clone()
        val specialistIcons = Table()
        val iconColumns = (infoWidth / 20f).toInt().coerceAtLeast(1)
        var iconCount = 0
        for ((specialistName, amount) in building.newSpecialists()) {
            val specialist = cityInfo.getRuleset().specialists[specialistName] ?: continue
            for (i in 0 until amount) {
                val color = if (assignedSpec[specialistName]!! > 0) {
                    assignedSpec.add(specialistName, -1)
                    specialist.colorObject
                } else Color.GRAY
                specialistIcons.add(ImageGetter.getSpecialistIcon(color)).size(20f)
                iconCount++
                if (royal && iconCount % iconColumns == 0) specialistIcons.row()
            }
        }
        statsAndSpecialists.add(specialistIcons).right()
        info.add(statsAndSpecialists).right()
        button.add(info).right().top().padRight(10f).padTop(5f)
        button.add(icon).right()
        button.onClick {
            cityScreen.selectConstruction(building)
            cityScreen.update()
        }
        destinationTable.add(button).pad(1f).padBottom(2f).padTop(2f).expandX().right().row()
    }

    private fun addPreparedCityEvents() {
        val preparedEvents = cityInfo.cityConstructions.getPreparedEvents()
        if (preparedEvents.none()) return

        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 48f).coerceAtLeast(1f)
        val eventsTable = Table()
        for (event in preparedEvents) {
            val eventRow = Table()
            val eventInfo = Table()
            val label = event.name.toLabel(
                fontColor = if (royal) Color.valueOf("453925") else Color.WHITE,
                fontSize = Constants.defaultFontSize
            )
            val labelCell = eventInfo.add(label)
            if (royal) {
                label.setWrap(true)
                // Резервируем место действия и для недоступного события, чтобы подписи не прыгали.
                labelCell.width((contentWidth - 100f).coerceAtLeast(1f))
            }
            labelCell.row()
            eventRow.add(ImageGetter.getPortraitImage(event.name, 50f)).size(50f).padRight(10f)
            eventRow.add(eventInfo).growX()
            if (event.isUsable(cityInfo)) {
                val useCityEventButton = ImageGetter.getImage("OtherIcons/New")
                    .apply { color = Color.BLACK }.surroundWithCircle(40f)
                useCityEventButton.onClick(UncivSound.Silent) {
                    event.use(cityInfo)
                    cityInfo.cityConstructions.removeCityEvent(event.name)
                    cityScreen.update()
                }
                eventRow.add(useCityEventButton)
            } else if (royal) {
                eventRow.add().width(40f)
            }
            eventsTable.add(eventRow).growX().padBottom(5f).row()
        }
        lowerTable.addCategory("Prepared Events", eventsTable, startsOpened = true)
    }


    private fun addRecruitmentInfo() {
        val units = mutableListOf<BaseUnit>()

        units.add(ruleset.units["Archer"]!!)
        units.add(ruleset.units["Warrior"]!!)


        val wonders = mutableListOf<Building>()
        val specialistBuildings = mutableListOf<Building>()
        val otherBuildings = mutableListOf<Building>()

        for (building in cityInfo.cityConstructions.getBuiltBuildings()) {
            when {
                building.isAnyWonder() -> wonders.add(building)
                !building.newSpecialists().isEmpty() -> specialistBuildings.add(building)
                else -> otherBuildings.add(building)
            }
        }

        // Buildings sorted alphabetically
        wonders.sortBy { it.name }
        specialistBuildings.sortBy { it.name }
        otherBuildings.sortBy { it.name }

        val totalTable = Table()
        lowerTable.addCategory("Recruit", totalTable, false)

        val unitsTable = Table()
        totalTable.add().row()
        //totalTable.addSeparator(color = Color.LIGHT_GRAY)
        totalTable.add("Available units:".toLabel().apply { setAlignment(Align.center) }).growX().colspan(4)
        totalTable.addSeparator(color = Color.LIGHT_GRAY)
        for (unit in units) {
            val amount2Buy = Slider(0f,100f, 10f, false, BaseScreen.skin)
            totalTable.add(amount2Buy)
            totalTable.add("Max".toTextButton().apply { onActivation { amount2Buy.value = 100f } }).pad(5f)
            totalTable.add("Buy".toTextButton())
          //  totalTable.add(unit.getIconName().)
        //    totalTable.row().height(25f)
            totalTable.add(unit.name.toLabel().apply { setAlignment(Align.right) }).growX().right().row()
          //  totalTable.add(unit.getDescription(cityScreen.city).toLabel()).row()
          //  totalTable.row().height(25f)

        }
/*
        private val detailedStatsButton = "Stats".toTextButton().apply {
            labelCell.pad(10f)
            onActivation {
                DetailedStatsPopup(cityScreen).open()
            }
        }


 */
/*
        if (specialistBuildings.isNotEmpty()) {
            val specialistBuildingsTable = Table()
            totalTable.add().row()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            totalTable.add("Specialist Buildings".toLabel().apply { setAlignment(Align.center) }).growX()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            for (building in specialistBuildings) addBuildingButton(building, specialistBuildingsTable)
            totalTable.add(specialistBuildingsTable).growX().right().row()
        }

        if (wonders.isNotEmpty()) {
            val wondersTable = Table()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            totalTable.add("Wonders".toLabel().apply { setAlignment(Align.center) }).growX()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            for (building in wonders) addBuildingButton(building, wondersTable)
            totalTable.add(wondersTable).growX().right().row()
        }

        if (otherBuildings.isNotEmpty()) {
            val regularBuildingsTable = Table()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            totalTable.add("Other".toLabel().apply { setAlignment(Align.center) }).growX()
            totalTable.addSeparator(color = Color.LIGHT_GRAY)
            for (building in otherBuildings) addBuildingButton(building, regularBuildingsTable)
            totalTable.add(regularBuildingsTable).growX().right().row()
        }

 */
    }


    private fun Table.addCategory(
        category: String,
        showHideTable: Table,
        startsOpened: Boolean = true,
        innerPadding: Float = 10f
    ): ExpanderTab {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val panelWidth = (cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f)
        val expanderTab = ExpanderTab(
            title = category,
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityInfo.$category",
            startsOutOpened = startsOpened,
            defaultPad = innerPadding,
            onChange = { onContentResize() }
        ) {
            it.add(showHideTable).fillX().right()
        }
        if (royal) {
            showHideTable.background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
            expanderTab.setHeaderAppearance(
                BaseScreen.skinStrings.getUiBackground(
                    "General/ExpanderTab", tintColor = Color.valueOf("D7C89F")
                ),
                Color.valueOf("453925"),
                (panelWidth - 20f).coerceAtLeast(1f)
            )
        }
        add(expanderTab).growX().row()
        return expanderTab
    }

    private fun addGreatPersonPointInfo(cityInfo: CityInfo) {
        val greatPersonPoints = cityInfo.getGreatPersonPointsForNextTurn()
        val allGreatPersonNames =
                greatPersonPoints.asSequence().flatMap { it.value.keys }.distinct()
        if (allGreatPersonNames.none()) return
        val royal = cityScreen.game.settings.skin == "Minimal"
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 48f).coerceAtLeast(1f)
        val greatPeopleTable = Table()
        if (royal) greatPeopleTable.background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
        )
        for (greatPersonName in allGreatPersonNames) {
            var gppPerTurn = 0
            for ((_, counter) in greatPersonPoints) {
                val points = counter[greatPersonName]!!
                if (points != 0) gppPerTurn += points
            }
            val info = Table()
            if (royal)
                info.add(ImageGetter.getPortraitImage(greatPersonName, 50f)).colspan(2)
                    .padBottom(5f).row()
            info.add(ImageGetter.getUnitIcon(greatPersonName, Color.GOLD).toGroup(20f))
                .left().padBottom(4f).padRight(5f)
            val label = "{$greatPersonName} (+$gppPerTurn)".toLabel(
                fontColor = if (royal) Color.valueOf("453925") else Color.WHITE
            )
            val labelCell = info.add(label).left().padBottom(4f).expandX()
            if (royal) {
                label.setWrap(true)
                labelCell.width((contentWidth - 30f).coerceAtLeast(1f))
            }
            labelCell.row()
            val gppCurrent = cityInfo.civInfo.greatPeople.greatPersonPointsCounter[greatPersonName]
            val gppNeeded = cityInfo.civInfo.greatPeople.pointsForNextGreatPerson
            val percent = gppCurrent!! / gppNeeded.toFloat()
            val progressBar =
                    ImageGetter.ProgressBar(if (royal) contentWidth - 5f else 300f, 25f, false)
            progressBar.setBackground(
                if (royal) Color.valueOf("453925") else Color.BLACK.cpy().apply { a = 0.8f })
            progressBar.setProgress(if (royal) Color.valueOf("8A692B") else Color.ORANGE, percent)
            progressBar.apply {
                val border = ImageGetter.getWhiteDot()
                border.color = if (royal) Color.valueOf("A18B5A") else Color.GRAY
                border.setSize(width + 5f, height + 5f)
                border.center(this)
                addActor(border)
                border.toBack()
            }
            progressBar.setLabel(Color.WHITE, "$gppCurrent/$gppNeeded", fontSize = 14)
            info.add(progressBar).colspan(2).left().expandX().row()
            greatPeopleTable.add(info).growX().top().padBottom(10f)
            if (!royal) greatPeopleTable.add(ImageGetter.getPortraitImage(greatPersonName, 50f))
            greatPeopleTable.row()
        }
        lowerTable.addCategory("Great People", greatPeopleTable)
    }

    private var availablePanelHeight = 0f
    private val royalStatsScroll by lazy {
        // Одна область прокрутки сохраняет доступ к разделам даже при высокой сводке.
        val content = Table().apply {
            add(upperTable).growX().row()
            add(lowerTable).growX().row()
        }
        ScrollPane(content).apply {
            name = "cityStatsScroll"
            setScrollingDisabled(true, false)
            setOverscroll(false, false)
        }
    }

    /** Ограничивает всю статистику доступным местом над выбранной карточкой. */
    fun constrainPanelHeight(height: Float) {
        if (cityScreen.game.settings.skin != "Minimal") return
        availablePanelHeight = height.coerceAtLeast(0f)
        val scroll = royalStatsScroll
        val savedScrollY = scroll.scrollY
        if (scroll.parent !== innerTable) {
            innerTable.clearChildren()
            innerTable.add(scroll).growX()
        }
        val content = scroll.actor as Table
        content.invalidateHierarchy()
        content.pack()
        val insets = padTop + padBottom + innerTable.padTop + innerTable.padBottom
        innerTable.getCell(scroll).width(content.prefWidth)
            .height(minOf(content.prefHeight, (availablePanelHeight - insets).coerceAtLeast(0f)))
        innerTable.pack()
        pack()
        scroll.validate()
        scroll.scrollY = savedScrollY.coerceIn(0f, scroll.maxY)
        scroll.updateVisualScroll()
    }
}
