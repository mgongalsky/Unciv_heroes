package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.Constants
import com.unciv.logic.city.CityFocus
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ExpanderTab
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel

class CitizenManagementTable(val cityScreen: CityScreen) : Table(BaseScreen.skin) {
    val city = cityScreen.city

    fun update() {
        clear()
        val royal = cityScreen.game.settings.skin == "Minimal"
        val colorSelected = if (royal) Color.valueOf("C6C394")
        else BaseScreen.skin.get("selection", Color::class.java)
        val colorButton = if (royal) Color.valueOf("EADFBD")
        else BaseScreen.skin.get("color", Color::class.java)
        val textColor = if (royal) Color.valueOf("453925") else Color.WHITE
        val contentWidth = ((cityScreen.stage.width / 3f).coerceAtLeast(300f)
            .coerceAtMost(cityScreen.stage.width - 24f) - 20f).coerceAtLeast(1f)

        fun control(
            text: String,
            path: String,
            selected: Boolean,
            fullRow: Boolean,
            action: () -> Unit
        ): Table {
            val label = text.toLabel(fontColor = textColor)
            val button = Table()
            val labelCell = button.add(label).pad(5f)
            if (royal) {
                label.setWrap(true)
                label.setAlignment(com.badlogic.gdx.utils.Align.center)
                val width = if (fullRow) contentWidth - 6f else (contentWidth - 12f) / 2f
                labelCell.width((width - 10f).coerceAtLeast(1f)).minHeight(38f)
            }
            if (cityScreen.canCityBeChanged()) {
                button.touchable = Touchable.enabled
                button.onClick { action() }
            }
            button.background = BaseScreen.skinStrings.getUiBackground(
                path, tintColor = if (selected) colorSelected else colorButton
            )
            return button
        }

        add(control("Reset Citizens", "CityScreen/CitizenManagementTable/ResetCell", false, true) {
            city.reassignPopulation(true)
            cityScreen.update()
        }).colspan(2).growX().pad(3f).row()
        add(
            control(
                "Avoid Growth",
                "CityScreen/CitizenManagementTable/AvoidCell",
                city.avoidGrowth,
                true
            ) {
                city.avoidGrowth = !city.avoidGrowth
                city.reassignPopulation()
                cityScreen.update()
            }).colspan(2).growX().pad(3f).row()

        var newRow = false
        for (focus in CityFocus.values()) {
            if (!focus.tableEnabled) continue
            if (focus == CityFocus.FaithFocus && !city.civInfo.gameInfo.isReligionEnabled()) continue
            add(
                control(
                    focus.label, "CityScreen/CitizenManagementTable/FocusCell",
                    city.cityAIFocus == focus, false
                ) {
                    city.cityAIFocus = focus
                    city.reassignPopulation()
                    cityScreen.update()
                }).growX().pad(3f)
            if (newRow) row()
            newRow = !newRow
        }
        pack()
    }

    fun asExpander(onChange: (() -> Unit)?): ExpanderTab {
        val expander = ExpanderTab(
            title = "{Citizen Management}",
            fontSize = Constants.defaultFontSize,
            persistenceID = "CityStatsTable.CitizenManagement",
            startsOutOpened = false,
            onChange = onChange
        ) {
            it.add(this)
            update()
        }
        if (cityScreen.game.settings.skin == "Minimal") {
            expander.innerTable.background = BaseScreen.skinStrings.getUiBackground(
                "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("EADFBD")
            )
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
