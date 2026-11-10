package com.unciv.ui.cityscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.UncivGame
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.extensions.addBorder
import com.unciv.ui.utils.extensions.addSeparatorVertical
import com.unciv.ui.utils.extensions.darken
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.surroundWithCircle
import com.unciv.ui.utils.extensions.toGroup
import com.unciv.ui.utils.extensions.toLabel

class SpecialistAllocationTable(val cityScreen: CityScreen) : Table(BaseScreen.skin) {
    val cityInfo = cityScreen.city

    fun update() {
        clear()
        val royal = cityScreen.game.settings.skin == "Minimal"
        val columns = if (royal) 3 else 5
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 36f).coerceAtLeast(1f)
        if (royal) {
            background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
            pad(8f)
        }
        if (cityScreen.canCityBeChanged()) {
            val label =
                (if (cityInfo.manualSpecialists) "Manual Specialists" else "Auto Specialists")
                    .toLabel(fontColor = if (royal) Color.valueOf("453925") else Color.WHITE)
            val toggle = if (royal) {
                label.setWrap(true)
                label.setAlignment(Align.center)
                Table().apply {
                    background = BaseScreen.skinStrings.getUiBackground(
                        "CityScreen/CitizenManagementTable/FocusCell",
                        tintColor = Color.valueOf(if (cityInfo.manualSpecialists) "C6C394" else "D7C89F")
                    )
                    add(label).width((contentWidth - 10f).coerceAtLeast(1f)).pad(5f).minHeight(38f)
                }
            } else label.addBorder(5f, BaseScreen.skin.get("color", Color::class.java))
            if (cityInfo.manualSpecialists) {
                toggle.onClick {
                    cityInfo.manualSpecialists = false
                    cityInfo.reassignPopulation()
                    cityScreen.update()
                }
            } else {
                toggle.onClick { cityInfo.manualSpecialists = true; update() }
            }
            add(toggle).colspan(columns).row()
        }
        for ((specialistName, maxSpecialists) in cityInfo.population.getMaxSpecialists()) {
            if (!cityInfo.getRuleset().specialists.containsKey(specialistName)) continue
            val assigned = cityInfo.population.getNewSpecialists()[specialistName]!!
            if (cityScreen.canChangeState) add(getUnassignButton(assigned, specialistName))
            else if (royal) add()
            add(getAllocationTable(assigned, maxSpecialists, specialistName)).pad(10f)
            if (cityScreen.canChangeState) add(
                getAssignButton(
                    assigned,
                    maxSpecialists,
                    specialistName
                )
            )
            else if (royal) add()
            if (royal) {
                row()
                add(getSpecialistStatsTable(specialistName)).colspan(columns).padBottom(10f).row()
            } else {
                addSeparatorVertical().pad(10f)
                add(getSpecialistStatsTable(specialistName)).row()
            }
        }
        pack()
    }


    fun getAllocationTable(
        assignedSpecialists: Int,
        maxSpecialists: Int,
        specialistName: String
    ): Table {
        val table = Table()
        val specialist = cityInfo.getRuleset().specialists[specialistName]!!
        val royal = cityScreen.game.settings.skin == "Minimal"
        val availableWidth = (cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 116f
        val columns = (availableWidth / 30f).toInt().coerceAtLeast(1)
        for (index in 1..maxSpecialists) {
            val color = if (index <= assignedSpecialists) specialist.colorObject else Color.GRAY
            table.add(ImageGetter.getSpecialistIcon(color)).size(30f)
            if (royal && index % columns == 0) table.row()
        }
        return table
    }

    private fun getAssignButton(
        assignedSpecialists: Int,
        maxSpecialists: Int,
        specialistName: String
    ): Actor {
        if (assignedSpecialists >= maxSpecialists || cityInfo.isPuppet) return Table()
        val assignButton = "+".toLabel(Color.BLACK, Constants.headingFontSize)
            .apply { this.setAlignment(Align.center) }
            .surroundWithCircle(30f).apply { circle.color = Color.GREEN.darken(0.2f) }
        assignButton.onClick {
            cityInfo.population.specialistAllocations.add(specialistName, 1)
            cityInfo.manualSpecialists = true
            cityInfo.cityStats.update()
            cityScreen.update()
        }
        if (cityInfo.population.getFreePopulation() == 0 || !cityScreen.isPlayersTurn)
            assignButton.clear()
        return assignButton
    }

    private fun getUnassignButton(assignedSpecialists: Int, specialistName: String): Actor {
        val unassignButton = "-".toLabel(Color.BLACK, Constants.headingFontSize)
            .apply { this.setAlignment(Align.center) }
            .surroundWithCircle(30f).apply { circle.color = Color.RED.darken(0.1f) }
        unassignButton.onClick {
            cityInfo.population.specialistAllocations.add(specialistName, -1)
            cityInfo.manualSpecialists = true
            cityInfo.cityStats.update()
            cityScreen.update()
        }
        if (assignedSpecialists <= 0 || cityInfo.isPuppet) unassignButton.isVisible = false
        if (!cityScreen.isPlayersTurn) unassignButton.clear()
        return unassignButton
    }


    private fun getSpecialistStatsTable(specialistName: String): Table {
        val royal = cityScreen.game.settings.skin == "Minimal"
        val ink = if (royal) Color.valueOf("453925") else Color.WHITE
        val table = Table().apply { defaults().padBottom(5f).padTop(5f) }
        for ((key, value) in cityInfo.cityStats.getStatsOfSpecialist(specialistName)) {
            if (value == 0f) continue
            table.add(value.toInt().toString().toLabel(fontColor = ink))
            table.add(ImageGetter.getStatIcon(key.name)).size(20f).padRight(10f)
            if (royal) table.row()
        }
        val specialist = cityInfo.getRuleset().specialists[specialistName]!!
        for (entry in specialist.greatPersonPoints) {
            table.add(entry.value.toString().toLabel(fontColor = ink))
            table.add(ImageGetter.getUnitIcon(entry.key, Color.GOLD).toGroup(20f)).padRight(10f)
            if (royal) table.row()
        }
        return table
    }


    fun asExpander(onChange: (() -> Unit)?): ExpanderTab {
        val expander = ExpanderTab(
            title = "{Specialists}:",
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityStatsTable.Specialists",
            startsOutOpened = true,
            onChange = onChange
        ) {
            it.add(this)
            update()
        }
        if (cityScreen.game.settings.skin == "Minimal") {
            expander.setHeaderAppearance(
                BaseScreen.skinStrings.getUiBackground(
                    "General/ExpanderTab",
                    tintColor = Color.valueOf("D7C89F")
                ),
                Color.valueOf("453925"),
                prefWidth + 20f
            )
        }
        return expander
    }

}
