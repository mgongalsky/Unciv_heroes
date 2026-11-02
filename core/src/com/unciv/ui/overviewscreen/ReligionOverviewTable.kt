package com.unciv.ui.overviewscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.models.Religion
import com.unciv.models.ruleset.Belief
import com.unciv.models.translations.fillPlaceholders
import com.unciv.models.translations.tr
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.civilopedia.MarkupRenderer
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.setFontSize
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.max

class ReligionOverviewTab(
    viewingPlayer: CivilizationInfo,
    overviewScreen: EmpireOverviewScreen,
    persistedData: EmpireOverviewTabPersistableData? = null
) : EmpireOverviewTab(viewingPlayer, overviewScreen) {
    class ReligionTabPersistableData(
        var selectedReligion: String? = null
    ) : EmpireOverviewTabPersistableData() {
        override fun isEmpty() = selectedReligion == null
    }
    override val persistableData = (persistedData as? ReligionTabPersistableData) ?: ReligionTabPersistableData()

    private val royal = overviewScreen.game.settings.skin == "Minimal"
    private val civStatsTable = Table()
    private val religionButtons = Table()
    private val religionButtonLabel = "Click an icon to see the stats of this religion".toLabel()
    private val statsTable = Table()
    private val beliefsTable = Table()

    /** Оставляет место для отступов вкладки и карточек в узком обзоре. */
    private fun contentWidth() = minOf(560f, overviewScreen.stage.width - 100f).coerceAtLeast(96f)

    /** Ограничивает обе колонки; копия стиля исключает изменение общих подписей skin. */
    private fun styleStats(table: Table, backgroundPath: String) {
        if (!royal) return
        table.background = BaseScreen.skinStrings.getUiBackground(
            backgroundPath, tintColor = Color.valueOf("EADFBD")
        )
        val columnsWidth = (contentWidth() - table.padLeft - table.padRight - 20f).coerceAtLeast(48f)
        for ((index, cell) in table.cells.withIndex()) {
            val label = cell.actor as? Label ?: continue
            label.style = Label.LabelStyle(label.style).apply { fontColor = Color.WHITE.cpy() }
            label.color = Color.valueOf("453925")
            label.setWrap(true)
            cell.width(columnsWidth * if (index % 2 == 0) 0.6f else 0.4f)
        }
    }

    override fun getFixedContent() = Table().apply {
        defaults().pad(5f)
        align(Align.top)
        civStatsTable.clear()
        civStatsTable.defaults().left().pad(5f)
        civStatsTable.addCivSpecificStats()
        styleStats(civStatsTable, "OverviewScreen/ReligionOverviewTab/CivStats")
        add(civStatsTable).row()
        add(religionButtons).row()
        if (royal) {
            val heading = Table().apply {
                background = BaseScreen.skinStrings.getUiBackground(
                    "OverviewScreen/ReligionOverviewTab/Heading", tintColor = Color.valueOf("EADFBD")
                )
                religionButtonLabel.color = Color.valueOf("453925")
                religionButtonLabel.setWrap(true)
                religionButtonLabel.setAlignment(Align.center)
                val textWidth = (contentWidth() - padLeft - padRight - 10f).coerceAtLeast(48f)
                add(religionButtonLabel).width(textWidth).pad(5f)
            }
            add(heading)
        } else add(religionButtonLabel)
        addSeparator()
    }

    init {
        defaults().pad(5f)
        align(Align.top)
        loadReligionButtons()
        statsTable.defaults().left().pad(5f)
        beliefsTable.defaults().padBottom(20f)
        loadReligion(persistableData.selectedReligion)
        add(statsTable).row()
        add(beliefsTable).pad(20f)
    }

    private fun Table.addCivSpecificStats() {
        if (viewingPlayer.religionManager.canGenerateProphet()) {
            add("Minimal Faith required for\nthe next [great prophet equivalent]:"
                .fillPlaceholders(viewingPlayer.religionManager.getGreatProphetEquivalent()!!).toLabel())
            add((viewingPlayer.religionManager.faithForNextGreatProphet() + 1).toLabel()).right().row()
        }
        add("Religions to be founded:".toLabel())
        add(viewingPlayer.religionManager.remainingFoundableReligions().toLabel()).right().row()
        add("Religious status:".toLabel()).left()
        add(viewingPlayer.religionManager.religionState.toString().toLabel()).right().row()
    }

    private fun loadReligionButtons() {
        religionButtons.clear()
        val availableWidth = (overviewScreen.stage.width - 40f).coerceAtLeast(1f)
        var rowWidth = 0f
        val existingReligions = gameInfo.civilizations.mapNotNull { it.religionManager.religion }
        for (religion in existingReligions) {
            val image = if (religion.isPantheon()) {
                if (viewingPlayer.knows(religion.foundingCivName) || viewingPlayer.civName == religion.foundingCivName)
                    ImageGetter.getNationIndicator(religion.getFounder().nation, 60f)
                else ImageGetter.getRandomNationIndicator(60f)
            } else ImageGetter.getCircledReligionIcon(religion.getIconName(), 60f)
            val button = Button(image, BaseScreen.skin)
            button.onClick {
                persistableData.selectedReligion = religion.name
                loadReligionButtons()
                loadReligion(religion)
            }
            if (persistableData.selectedReligion == religion.name) button.disable()
            val buttonWidth = button.prefWidth + 10f
            if (royal && rowWidth > 0f && rowWidth + buttonWidth > availableWidth) {
                religionButtons.row()
                rowWidth = 0f
            }
            religionButtons.add(button).pad(5f)
            rowWidth += buttonWidth
        }
    }

    override fun select(selection: String) {
        persistableData.selectedReligion = selection
        loadReligionButtons()
        loadReligion(selection)
    }

    private fun loadReligion(religionName: String?) {
        if (religionName == null) return
        val religion = gameInfo.religions[religionName] ?: return
        loadReligion(religion)
    }

    private fun loadReligion(religion: Religion) {
        statsTable.clear()
        beliefsTable.clear()
        religionButtonLabel.setText(religion.getReligionDisplayName().tr())
        religionButtonLabel.setFontSize(Constants.headingFontSize)
        religionButtonLabel.color = if (royal) Color.valueOf("453925") else Color.CORAL
        for (belief in religion.getAllBeliefsOrdered()) {
            beliefsTable.add(createBeliefDescription(belief)).row()
        }
        statsTable.add((if (religion.isPantheon()) "Pantheon Name:" else "Religion Name:").toLabel())
        statsTable.add(religion.getReligionDisplayName().toLabel()).right().row()
        statsTable.add("Founding Civ:".toLabel())
        val foundingCivName =
            if (viewingPlayer.knows(religion.foundingCivName) || viewingPlayer.civName == religion.foundingCivName)
                religion.foundingCivName
            else Constants.unknownNationName
        statsTable.add(foundingCivName.toLabel()).right().row()
        if (religion.isMajorReligion()) {
            val holyCity = gameInfo.getCities().firstOrNull { it.isHolyCityOf(religion.name) }
            if (holyCity != null) {
                statsTable.add("Holy City:".toLabel())
                val cityName = if (viewingPlayer.hasExplored(holyCity.location)) holyCity.name
                    else Constants.unknownNationName
                statsTable.add(cityName.toLabel()).right().row()
            }
        }
        val manager = religion.getFounder().religionManager
        statsTable.add("Cities following this religion:".toLabel())
        statsTable.add(manager.numberOfCitiesFollowingThisReligion().toLabel()).right().row()
        statsTable.add("Followers of this religion:".toLabel())
        statsTable.add(manager.numberOfFollowersFollowingThisReligion("in all cities").toLabel()).right().row()

        if (royal) {
            styleStats(statsTable, "OverviewScreen/ReligionOverviewTab/Stats")
        } else {
            val minWidth = max(statsTable.minWidth, beliefsTable.minWidth) + 5
            statsTable.width = minWidth
            for (cell in beliefsTable.cells) cell.minWidth(minWidth)
        }
        // После выбора пересчитываем размеры, сохраняя прежнее центрирование широкой таблицы.
        invalidateHierarchy()
        (parent as? ScrollPane)?.apply {
            layout()
            scrollX = maxX / 2
            updateVisualScroll()
        }
    }

    private fun createBeliefDescription(belief: Belief): Table {
        return MarkupRenderer.renderWithTextColor(
            belief.getCivilopediaTextLines(withHeader = true),
            defaultTextColor = if (royal) Color.valueOf("453925") else null,
            labelWidth = if (royal) contentWidth() else 0f
        ) {
            UncivGame.Current.pushScreen(CivilopediaScreen(gameInfo.ruleSet, link = it))
        }.apply {
            background = BaseScreen.skinStrings.getUiBackground(
                "OverviewScreen/ReligionOverviewTab/BeliefDescription",
                tintColor = if (royal) Color.valueOf("EADFBD") else BaseScreen.skinStrings.skinConfig.baseColor
            )
        }
    }
}
