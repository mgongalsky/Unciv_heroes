package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.logic.city.StatTreeNode
import com.unciv.models.stats.Stat
import com.unciv.models.stats.Stats
import com.unciv.models.translations.tr
import com.unciv.ui.images.IconCircleGroup
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.KeyCharAndCode
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.brighten
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toLabel
import java.text.DecimalFormat

class DetailedStatsPopup(val cityScreen: CityScreen, stageToShowOn: Stage) : Popup(
    stageToShowOn = stageToShowOn,
    scrollable = false) {

    constructor(screen: CityScreen) : this(screen, screen.stage)

    private val totalTable = Table()

    private var sourceHighlighted: String? = null
    private var onlyWithStat: Stat? = null
    private var isDetailed: Boolean = false

    private val colorTotal: Color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("D7C89F") else Color.BLUE.brighten(0.5f)
    private val colorSelector: Color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("C6C394") else Color.GREEN.darken(0.5f)

    init {
        val royal = cityScreen.game.settings.skin == "Minimal"
        if (royal) {
            innerTable.background = com.unciv.ui.utils.RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(52f).padTop(58f)
        }
        val scrollPane = AutoScrollPane(totalTable)
        scrollPane.name = "detailedStatsScroll"
        scrollPane.setOverscroll(false, false)
        val scrollPaneCell = add(scrollPane)
        row()
        val closeCell = addCloseButton(
            "Cancel", KeyCharAndCode('n'),
            if (royal) null else BaseScreen.skin.get("fantasy", TextButton.TextButtonStyle::class.java)
        )
        closeCell.actor.name = "detailedStatsClose"
        closeCell.actor.label.setAlignment(Align.center)
        closeCell.actor.pad(10f)
        if (royal) {
            closeCell.actor.label.setWrap(true)
            val contentWidth = (stageToShowOn.width - 24f - innerTable.padLeft -
                innerTable.padRight - scrollPaneCell.padLeft - scrollPaneCell.padRight).coerceAtLeast(1f)
            closeCell.width(minOf(240f, contentWidth)).minHeight(48f)
            closeCell.actor.width = minOf(240f, contentWidth)
            closeCell.actor.validate()
            val closeHeight = maxOf(48f, closeCell.actor.prefHeight)
            val contentHeight = (stageToShowOn.height - 24f - innerTable.padTop -
                innerTable.padBottom - scrollPaneCell.padTop - scrollPaneCell.padBottom -
                closeCell.padTop - closeCell.padBottom - closeHeight).coerceAtLeast(1f)
            scrollPaneCell.width(contentWidth).height(contentHeight)
        } else {
            scrollPaneCell.maxHeight(cityScreen.stage.height * 3 / 4)
            closeCell.actor.label.setFontScale(0.5f, 0.5f)
        }
        update()
    }
    private fun update() {

        totalTable.clear()

        val cityStats = cityScreen.city.cityStats
        val showFaith = cityScreen.city.civInfo.gameInfo.isReligionEnabled()

        val stats = when {
            onlyWithStat != null -> listOfNotNull(onlyWithStat)
            !showFaith -> Stat.values().filter { it != Stat.Faith }
            else -> Stat.values().toList()
        }

        totalTable.defaults().pad(3f).padLeft(0f).padRight(0f)

        totalTable.add(getToggleButton(isDetailed).onClick {
            isDetailed = !isDetailed
            update() }).minWidth(150f).grow()

        for (stat in stats) {
            val label = stat.name.toStatsLabel()
            label.onClick {
                onlyWithStat = if (onlyWithStat == null) stat else null
                update()
            }
            totalTable.add(wrapInTable(label, if (onlyWithStat == stat) colorSelector else null))
                .minWidth(if (onlyWithStat == stat) 150f else 110f).grow()
        }
        totalTable.row()

        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padBottom(2f)
        totalTable.add("Base values".toStatsLabel().apply { setAlignment(Align.center) })
            .colspan(totalTable.columns).padLeft(0f).padRight(0f).growX().row()
        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padTop(2f)
        traverseTree(totalTable, stats, cityStats.baseStatTree, mergeHappiness = true, percentage = false)

        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padBottom(2f)
        totalTable.add("Bonuses".toStatsLabel().apply { setAlignment(Align.center) })
            .colspan(totalTable.columns).padLeft(0f).padRight(0f).growX().row()
        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padTop(2f)
        traverseTree(totalTable, stats, cityStats.statPercentBonusTree, percentage = true)

        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padBottom(2f)
        totalTable.add("Final".toStatsLabel().apply { setAlignment(Align.center) })
            .colspan(totalTable.columns).padLeft(0f).padRight(0f).growX().row()
        totalTable.addSeparator(color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("A18B5A") else Color.WHITE).padTop(2f)

        val final = LinkedHashMap<Stat, Float>()
        val map = cityStats.finalStatList.mapValues { it.value.clone() }.toSortedMap()

        for ((key, value) in cityScreen.city.cityStats.happinessList) {
            if (!map.containsKey(key)) {
                map[key] = Stats()
                map[key]!![Stat.Happiness] = value
            } else if (map[key]!![Stat.Happiness] == 0f) {
                map[key]!![Stat.Happiness] = value
            }
        }

        for ((source, finalStats) in map) {

            if (finalStats.all { it.value == 0f })
                continue

            if (onlyWithStat != null && finalStats[onlyWithStat!!] == 0f)
                continue

            val label = source.toStatsLabel().apply {
                setAlignment(Align.left)
                onClick {
                    sourceHighlighted = if (sourceHighlighted == source) null else source
                    update()
                }
            }

            var color: Color? = null

            if (sourceHighlighted == source)
                color = colorSelector

            totalTable.add(wrapInTable(label, color, Align.left)).grow()

            for (stat in stats) {
                val value = finalStats[stat]
                val cell = when (value) {
                    0f -> "-".toStatsLabel()
                    else -> value.toOneDecimalLabel()
                }

                totalTable.add(wrapInTable(cell, color)).grow()

                var f = final[stat]
                if (f == null)
                    f = 0f
                f += value
                final[stat] = f

            }
            totalTable.row()
        }

        totalTable.add(wrapInTable("Total".toStatsLabel(), colorTotal)).grow()
        for (stat in stats) {
            totalTable.add(wrapInTable(final[stat]?.toOneDecimalLabel(), colorTotal)).grow()
        }
        totalTable.row()
    }

    private fun getToggleButton(showDetails: Boolean): IconCircleGroup {
        val label = (if (showDetails) "-" else "+").toStatsLabel()
        label.setAlignment(Align.center)
        return label
            .surroundWithCircle(25f, color = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("D7C89F") else BaseScreen.skinStrings.skinConfig.baseColor)
            .surroundWithCircle(27f, false)
    }

    private fun traverseTree(
        table: Table,
        stats: List<Stat>,
        statTreeNode: StatTreeNode,
        mergeHappiness: Boolean = false,
        percentage: Boolean = false,
        indentation: Int = 0
    ) {

        val total = LinkedHashMap<Stat, Float>()
        val map = (if (mergeHappiness) statTreeNode.children.mapValues { it.value.clone() } else statTreeNode.children).toSortedMap()

        if (mergeHappiness) {
            for ((key, value) in cityScreen.city.cityStats.happinessList) {
                if (!map.containsKey(key)) {
                    map[key] = StatTreeNode()
                    map[key]?.setInnerStat(Stat.Happiness, value)
                } else if (map[key]!!.totalStats.happiness == 0f) {
                    map[key]?.setInnerStat(Stat.Happiness, value)
                }
            }
        }

        for ((name, child) in map) {

            val text = "- ".repeat(indentation) + name.tr()

            if (child.totalStats.all { it.value == 0f }) {
                table.row()
                continue
            }

            if (onlyWithStat != null && child.totalStats[onlyWithStat!!] == 0f) {
                table.row()
                continue
            }

            val label = text.toStatsLabel().apply {
                setAlignment(Align.left)
                onClick {
                    sourceHighlighted = if (sourceHighlighted == text) null else text
                    update()
                }
            }

            var color: Color? = null

            if (sourceHighlighted == text)
                color = colorSelector

            table.add(wrapInTable(label, color, Align.left)).fill().left()

            for (stat in stats) {
                val value = child.totalStats[stat]
                val cell = when {
                    value == 0f -> "-".toStatsLabel()
                    percentage ->  value.toPercentLabel()
                    else -> value.toOneDecimalLabel()
                }

                table.add(wrapInTable(cell, color)).grow()

                if (indentation == 0) {
                    var current = total[stat]
                    if (current == null)
                        current = 0f
                    total[stat] = current + value
                }
            }

            table.row()
            if (isDetailed)
                traverseTree(table, stats, child, percentage = percentage, indentation = indentation + 1)

        }

        if (indentation == 0) {
            table.add(wrapInTable("Total".toStatsLabel(), colorTotal)).grow()
            for (stat in stats) {
                if (percentage)
                    table.add(wrapInTable(total[stat]?.toPercentLabel(), colorTotal)).grow()
                else
                    table.add(wrapInTable(total[stat]?.toOneDecimalLabel(), colorTotal)).grow()
            }
            table.row()
        }

    }

    private fun wrapInTable(label: Label?, color: Color? = null, align: Int = Align.center) : Table {
        val tbl = Table()
        label?.setAlignment(align)
        if (cityScreen.game.settings.skin == "Minimal") label?.color = Color.valueOf("453925")
        if (color != null)
            tbl.background = BaseScreen.skinStrings.getUiBackground("General/Border", tintColor = color)
        tbl.add(label).growX()
        return tbl
    }

    private fun String.toStatsLabel(): Label = toLabel(
        fontColor = if (cityScreen.game.settings.skin == "Minimal") Color.valueOf("453925") else Color.WHITE
    )
    companion object {
        private fun Float.toPercentLabel() =
                "${if (this>0f) "+" else ""}${DecimalFormat("0.#").format(this)}%".toLabel()
        private fun Float.toOneDecimalLabel() =
                DecimalFormat("0.#").format(this).toLabel()
    }
}

