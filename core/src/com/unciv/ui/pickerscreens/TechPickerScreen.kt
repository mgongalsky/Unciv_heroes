package com.unciv.ui.pickerscreens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.logic.civilization.CivilizationInfo
import com.unciv.logic.civilization.TechManager
import com.unciv.models.UncivSound
import com.unciv.models.ruleset.tech.Technology
import com.unciv.models.ruleset.unique.UniqueType
import com.unciv.models.translations.tr
import com.unciv.ui.civilopedia.CivilopediaCategories
import com.unciv.ui.civilopedia.CivilopediaScreen
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.ToastPopup
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.colorFromRGB
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.utils.concurrency.Concurrency
import kotlin.math.abs


class TechPickerScreen(
    internal val civInfo: CivilizationInfo,
    centerOnTech: Technology? = null,
    private val freeTechPick: Boolean = false
) : PickerScreen() {

    private var techNameToButton = HashMap<String, TechButton>()
    private var selectedTech: Technology? = null
    private var civTech: TechManager = civInfo.tech
    private var tempTechsToResearch: ArrayList<String>
    private var lines = ArrayList<Image>()
    private var eraLabels = ArrayList<Label>()

    /** We need this to be a separate table, and NOT the topTable, because *inhales*
     * When call setConnectingLines we need to pack() the table so that the lines will align correctly, BUT
     *  this causes the table to be SMALLER THAN THE SCREEN for small tech trees from mods,
     *  meaning the tech tree is in a crumpled heap at the lower-left corner of the screen
     * Having this be a separate table allows us to leave the TopTable as is (that is: auto-width to fit the scrollPane)
     *  leaving us the juicy small tech tree right in the center.
     */
    private val techTable = Table()

    // All these are to counter performance problems when updating buttons for all techs.
    private var researchableTechs = civInfo.gameInfo.ruleSet.technologies.keys
            .filter { civTech.canBeResearched(it) }.toHashSet()

    private val currentTechColor = colorFromRGB(72, 147, 175)
    private val researchedTechColor = colorFromRGB(255, 215, 0)
    private val researchableTechColor = colorFromRGB(28, 170, 0)
    private val queuedTechColor = colorFromRGB(7*2, 46*2, 43*2)


    private val turnsToTech = civInfo.gameInfo.ruleSet.technologies.values.associateBy({ it.name }, { civTech.turnsToTech(it.name) })

    init {
        setDefaultCloseAction()
        scrollPane.setOverscroll(false, false)
        descriptionLabel.onClick {
            if (selectedTech != null)
                game.pushScreen(
                    CivilopediaScreen(
                        civInfo.gameInfo.ruleSet,
                        CivilopediaCategories.Technology, selectedTech!!.name
                    )
                )
        }
        tempTechsToResearch = ArrayList(civTech.techsToResearch)
        createTechTable()
        setButtonsInfo()
        topTable.add(techTable)
        val royal = game.settings.skin == "Minimal"
        val panelColor = if (royal) Color.valueOf("EADFBD") else skinStrings.skinConfig.clearColor
        techTable.background =
                skinStrings.getUiBackground("TechPickerScreen/Background", tintColor = panelColor)
        pickerPane.bottomTable.background =
                skinStrings.getUiBackground("TechPickerScreen/BottomTable", tintColor = panelColor)
        if (royal) {
            pickerPane.enableAdaptiveActions()
            topTable.background = skinStrings.getUiBackground(
                "TechPickerScreen/Background",
                tintColor = panelColor
            )
            // Стиль копируется, чтобы цвет описания не менял остальные Label приложения.
            descriptionLabel.style = Label.LabelStyle(descriptionLabel.style).apply {
                fontColor = Color.valueOf("453925")
            }
            descriptionLabel.color = Color.WHITE
        }
        rightSideButton.setText("Pick a tech".tr())
        rightSideButton.onClick(UncivSound.Paper) {
            if (freeTechPick) {
                val freeTech = selectedTech!!.name
                // Повторное быстрое нажатие не должно выдавать уже недоступную технологию.
                if (!researchableTechs.contains(freeTech)) return@onClick
                civTech.getFreeTechnology(selectedTech!!.name)
            } else civTech.techsToResearch = tempTechsToResearch
            game.settings.addCompletedTutorialTask("Pick technology")
            game.popScreen()
        }
        val tech = centerOnTech ?: civInfo.tech.currentTechnology()
        if (tech != null) {
            if (civInfo.tech.isResearched(tech.name) || civInfo.tech.techsToResearch.size <= 1)
                selectTechnology(tech, true)
            else centerOnTechnology(tech)
        } else {
            val firstAvailable = researchableTechs.firstOrNull()
            val firstAvailableTech = civInfo.gameInfo.ruleSet.technologies[firstAvailable]
            if (firstAvailableTech != null) centerOnTechnology(firstAvailableTech)
        }
    }

    private fun createTechTable() {
        val royal = game.settings.skin == "Minimal"
        for (label in eraLabels) label.remove()
        eraLabels.clear()

        val allTechs = civInfo.gameInfo.ruleSet.technologies.values
        if (allTechs.isEmpty()) return
        val columns = allTechs.maxOf { it.column!!.columnNumber } + 1
        val rows = allTechs.maxOf { it.row } + 1
        val techMatrix = Array<Array<Technology?>>(columns) { arrayOfNulls(rows) }
        for (technology in allTechs)
            techMatrix[technology.column!!.columnNumber][technology.row - 1] = technology

        val erasNamesToColumns = LinkedHashMap<String, ArrayList<Int>>()
        for (tech in allTechs) {
            val era = tech.era()
            if (!erasNamesToColumns.containsKey(era)) erasNamesToColumns[era] = ArrayList()
            val columnNumber = tech.column!!.columnNumber
            if (!erasNamesToColumns[era]!!.contains(columnNumber))
                erasNamesToColumns[era]!!.add(columnNumber)
        }
        for ((era, eraColumns) in erasNamesToColumns) {
            val pastEra = civInfo.gameInfo.ruleSet.eras[era]!!.eraNumber < civTech.era.eraNumber
            val color = if (royal) when {
                civTech.era.name == era -> Color.valueOf("B8C997")
                pastEra -> Color.valueOf("D8BD72")
                else -> Color.valueOf("EADFBD")
            } else when {
                civTech.era.name == era -> queuedTechColor
                pastEra -> colorFromRGB(255, 175, 0)
                else -> Color.BLACK
            }
            val border = Table().pad(1f)
            val heading = Table()
            border.background = skinStrings.getUiBackground(
                "General/Border",
                tintColor = if (royal) Color.valueOf("8D7546") else Color.WHITE
            )
            heading.background = skinStrings.getUiBackground("General/Border", tintColor = color)
            val label = era.toLabel().apply {
                setAlignment(Align.center)
                if (royal) this.color = Color.valueOf("453925")
                else if (pastEra) this.color = colorFromRGB(120, 46, 16)
            }
            eraLabels.add(label)
            heading.add(label).growX()
            border.add(heading).growX()
            techTable.add(border).fill().colspan(eraColumns.size)
        }

        for (rowIndex in 0 until rows) {
            techTable.row()
            for (columnIndex in techMatrix.indices) {
                val tech = techMatrix[columnIndex][rowIndex]
                val table = Table().pad(2f).padRight(60f).padLeft(20f)
                if (rowIndex == 0) table.padTop(7f)
                if (erasNamesToColumns[civTech.era.name]!!.contains(columnIndex))
                    table.background = skinStrings.getUiBackground(
                        "TechPickerScreen/Background",
                        tintColor = if (royal) Color.valueOf("D5D7B5") else queuedTechColor.darken(
                            0.5f
                        )
                    )
                if (tech == null) {
                    techTable.add(table).fill()
                } else {
                    val techButton = TechButton(tech.name, civTech, false)
                    table.add(techButton)
                    techNameToButton[tech.name] = techButton
                    techButton.onClick { selectTechnology(tech, false) }
                    techTable.add(table).fillX()
                }
            }
        }
    }

    private fun setButtonsInfo() {
        val royal = game.settings.skin == "Minimal"
        for (techName in techNameToButton.keys) {
            val techButton = techNameToButton[techName]!!
            val researched = civTech.isResearched(techName) && techName != Constants.futureTech
            if (royal) {
                // Цвет принадлежит фону карточки: значки и дочерние подписи не тонируются целиком.
                val backgroundColor = when {
                    researched -> Color.valueOf("D8BD72")
                    tempTechsToResearch.firstOrNull() == techName && !freeTechPick -> Color.valueOf(
                        "9BC6D1"
                    )

                    researchableTechs.contains(techName) -> Color.valueOf("B8C997")
                    tempTechsToResearch.contains(techName) -> Color.valueOf("A6BDB2")
                    else -> Color.valueOf("C9C1AC")
                }
                techButton.color = Color.WHITE
                techButton.text.color = Color.valueOf("453925")
                techButton.turns.color = Color.valueOf("453925")
                techButton.setButtonColor(
                    if (techName == selectedTech?.name && civTech.isResearched(techName))
                        Color.valueOf("F0DCA0")
                    else backgroundColor
                )
            } else {
                techButton.setButtonColor(
                    when {
                        researched -> researchedTechColor
                        tempTechsToResearch.firstOrNull() == techName && !freeTechPick -> currentTechColor
                        researchableTechs.contains(techName) -> researchableTechColor
                        tempTechsToResearch.contains(techName) -> queuedTechColor
                        else -> Color.BLACK
                    }
                )
                if (researched) {
                    techButton.text.color = colorFromRGB(154, 98, 16)
                    techButton.color = researchedTechColor.cpy().darken(0.5f)
                }
                if (techName == selectedTech?.name && civTech.isResearched(techName)) {
                    techButton.setButtonColor(colorFromRGB(230, 220, 114))
                }
            }

            techButton.orderIndicator?.remove()
            if (tempTechsToResearch.contains(techName) && tempTechsToResearch.size > 1) {
                techButton.addOrderIndicator(tempTechsToResearch.indexOf(techName) + 1)
            }
            if (!researched) {
                techButton.turns.setText(turnsToTech[techName] + "${Fonts.turn}".tr())
            } else if (royal) {
                techButton.turns.setText("")
            }
            techButton.text.setText(techName.tr())
        }
        addConnectingLines()
    }

    private fun addConnectingLines() {
        val royal = game.settings.skin == "Minimal"
        val normalLineColor = if (royal) Color.valueOf("796747") else Color.WHITE
        val availableLineColor = if (royal) Color.valueOf("47633A") else researchableTechColor
        val queuedLineColor = if (royal) Color.valueOf("356575") else currentTechColor
        techTable.pack()
        scrollPane.updateVisualScroll()
        for (line in lines) line.remove()
        lines.clear()

        for (eraLabel in eraLabels) {
            val coords = Vector2(0f, 0f)
            eraLabel.localToStageCoordinates(coords)
            techTable.stageToLocalCoordinates(coords)
            val line = ImageGetter.getLine(
                coords.x - 1f,
                coords.y,
                coords.x - 1f,
                coords.y - 1000f,
                1f
            )
            line.color =
                    (if (royal) Color.valueOf("8D7546") else Color.GRAY.cpy()).apply { a = 0.6f }
            line.toBack()
            techTable.addActor(line)
            lines.add(line)
        }

        for (tech in civInfo.gameInfo.ruleSet.technologies.values) {
            if (!techNameToButton.containsKey(tech.name)) {
                ToastPopup(
                    "Tech ${tech.name} appears to be missing - perhaps two techs have the same row & column",
                    this
                )
                continue
            }
            val techButton = techNameToButton[tech.name]!!
            for (prerequisite in tech.prerequisites) {
                if (!techNameToButton.containsKey(prerequisite)) {
                    ToastPopup(
                        "Tech $prerequisite. prerequisite of ${tech.name}, appears to be missing - perhaps two techs have the same row & column",
                        this
                    )
                    continue
                }
                val prerequisiteButton = techNameToButton[prerequisite]!!
                val techButtonCoords = Vector2(0f, techButton.height / 2)
                techButton.localToStageCoordinates(techButtonCoords)
                techTable.stageToLocalCoordinates(techButtonCoords)
                val prerequisiteCoords =
                        Vector2(prerequisiteButton.width, prerequisiteButton.height / 2)
                prerequisiteButton.localToStageCoordinates(prerequisiteCoords)
                techTable.stageToLocalCoordinates(prerequisiteCoords)
                val lineColor = when {
                    civTech.isResearched(tech.name) && !tech.isContinuallyResearchable() -> normalLineColor
                    civTech.isResearched(prerequisite) -> availableLineColor
                    tempTechsToResearch.contains(tech.name) -> queuedLineColor
                    else -> normalLineColor
                }
                val lineSize = when {
                    tempTechsToResearch.contains(tech.name) && !civTech.isResearched(prerequisite) -> 4f
                    else -> 2f
                }
                if (techButtonCoords.y != prerequisiteCoords.y) {
                    val r = 6f
                    val deltaX = techButtonCoords.x - prerequisiteCoords.x
                    val deltaY = techButtonCoords.y - prerequisiteCoords.y
                    val halfLength = deltaX / 2f
                    val line = ImageGetter.getWhiteDot().apply {
                        width = halfLength - r
                        height = lineSize
                        x = prerequisiteCoords.x
                        y = prerequisiteCoords.y - height / 2
                    }
                    val line1 = ImageGetter.getWhiteDot().apply {
                        width = halfLength - r
                        height = lineSize
                        x = techButtonCoords.x - halfLength + r
                        y = techButtonCoords.y - height / 2
                    }
                    val line2 = ImageGetter.getWhiteDot().apply {
                        width = lineSize
                        height = abs(deltaY) - 2 * r
                        x = techButtonCoords.x - halfLength - width / 2
                        y = techButtonCoords.y + (if (deltaY > 0f) -height - r else r + width / 2)
                    }
                    val line3: Image
                    val line4: Image
                    if (deltaY < 0) {
                        line3 = ImageGetter.getLine(
                            line2.x, line2.y + line2.height,
                            line.x + line.width, line.y + lineSize / 2, lineSize
                        )
                        line4 = ImageGetter.getLine(
                            line2.x,
                            line2.y,
                            line1.x,
                            line1.y + lineSize / 2,
                            lineSize
                        )
                    } else {
                        line3 = ImageGetter.getLine(
                            line2.x + lineSize / 2, line2.y,
                            line.x + line.width, line.y, lineSize
                        )
                        line4 = ImageGetter.getLine(
                            line2.x, line2.y + line2.height - lineSize / 2,
                            line1.x, line1.y + lineSize / 2, lineSize
                        )
                    }
                    for (segment in listOf(line, line1, line2, line3, line4)) {
                        segment.color = lineColor
                        techTable.addActor(segment)
                        segment.toFront()
                        lines.add(segment)
                    }
                } else {
                    val line = ImageGetter.getWhiteDot().apply {
                        width = techButtonCoords.x - prerequisiteCoords.x
                        height = lineSize
                        x = prerequisiteCoords.x
                        y = prerequisiteCoords.y - height / 2
                    }
                    line.color = lineColor
                    techTable.addActor(line)
                    line.zIndex = if (tempTechsToResearch.contains(tech.name)) 200000 else 500
                    lines.add(line)
                }
            }
        }
        for (line in lines) {
            if (line.color == queuedLineColor) line.toFront()
        }
    }

    private fun selectTechnology(tech: Technology?, center: Boolean = false, switchFromWorldScreen: Boolean = true) {

        val previousSelectedTech = selectedTech
        selectedTech = tech
        descriptionLabel.setText(tech?.getDescription(civInfo.gameInfo.ruleSet))

        if (!switchFromWorldScreen)
            return

        if (tech == null)
            return

        // center on technology
        if (center) centerOnTechnology(tech)

        if (freeTechPick) {
            selectTechnologyForFreeTech(tech)
            setButtonsInfo()
            return
        }

        if (civInfo.gameInfo.gameParameters.godMode && !civInfo.tech.isResearched(tech.name)
                && selectedTech == previousSelectedTech) {
            civInfo.tech.addTechnology(tech.name)
        }

        if (civTech.isResearched(tech.name) && !tech.isContinuallyResearchable()) {
            rightSideButton.setText("Pick a tech".tr())
            rightSideButton.disable()
            setButtonsInfo()
            return
        }

        if (!UncivGame.Current.worldScreen!!.canChangeState) {
            rightSideButton.disable()
            return
        }

        val pathToTech = civTech.getRequiredTechsToDestination(tech)
        for (requiredTech in pathToTech) {
            for (unique in requiredTech.uniqueObjects
                .filter { it.type == UniqueType.OnlyAvailableWhen && !it.conditionalsApply(civInfo) }) {
                rightSideButton.setText(unique.text.tr())
                rightSideButton.disable()
                return
            }
        }

        tempTechsToResearch.clear()
        tempTechsToResearch.addAll(pathToTech.map { it.name })

        val label = "Research [${tempTechsToResearch[0]}]".tr()
        val techProgression = getTechProgressLabel(tempTechsToResearch)

        pick("${label}\n${techProgression}")
        setButtonsInfo()
    }

    private fun getTechProgressLabel(techs: List<String>): String {
        val progress = techs.sumOf { tech -> civTech.researchOfTech(tech) }
        val techCost = techs.sumOf { tech -> civInfo.tech.costOfTech(tech) }
        return "(${progress}/${techCost})"
    }

    private fun centerOnTechnology(tech: Technology) {
        Concurrency.runOnGLThread {
            techNameToButton[tech.name]?.parent?.let {
                scrollPane.scrollTo(it.x, it.y, it.width, it.height, true, true)
                scrollPane.updateVisualScroll()
            }
        }
    }

    private fun selectTechnologyForFreeTech(tech: Technology) {
        if (researchableTechs.contains(tech.name)) {
            val label = "Pick [${tech.name}] as free tech".tr()
            val techProgression = getTechProgressLabel(listOf(tech.name))
            pick("${label}\n${techProgression}")
        } else {
            rightSideButton.setText("Pick a free tech".tr())
            rightSideButton.disable()
        }
    }

}
