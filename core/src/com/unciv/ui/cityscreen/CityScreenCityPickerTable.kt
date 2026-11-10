package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.AskTextPopup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel

/** Widget for the City Screen -
 *  the panel at bottom center showing the city name and offering arrows to cycle through the cities. */
class CityScreenCityPickerTable(private val cityScreen: CityScreen) : Table() {

    fun update() {
        val city = cityScreen.city
        val civInfo = city.civInfo
        val royal = cityScreen.game.settings.skin == "Minimal"
        val hasNeighbours = civInfo.cities.size > 1
        val ink = if (royal) Color.valueOf("453925") else civInfo.nation.getInnerColor()
        clear()
        pad(if (royal) 8f else 0f)
        background = BaseScreen.skinStrings.getUiBackground(
            "CityScreen/CityPickerTable",
            BaseScreen.skinStrings.roundedEdgeRectangleShape,
            if (royal) Color.valueOf("EADFBD") else civInfo.nation.getOuterColor()
        )
        fun addArrow(direction: Int) {
            if (!hasNeighbours) {
                add()
                return
            }
            val button = Table()
            val image = ImageGetter.getImage("OtherIcons/BackArrow")
            image.color = ink
            if (direction > 0) {
                image.setSize(25f, 25f)
                image.setOrigin(Align.center)
                image.rotation = 180f
            }
            button.add(image).size(25f).pad(10f)
            button.touchable = Touchable.enabled
            button.onClick { cityScreen.page(direction) }
            add(button).pad(if (royal) 5f else 10f)
        }
        addArrow(-1)
        val cityNameTable = Table()
        val statusTable = if (royal) Table() else cityNameTable
        if (city.isBeingRazed)
            statusTable.add(ImageGetter.getImage("OtherIcons/Fire")).size(20f).padRight(5f)
        if (city.isCapital())
            statusTable.add(ImageGetter.getImage("OtherIcons/Star").apply {
                color = if (royal) Color.valueOf("8A692B") else Color.LIGHT_GRAY
            }).size(20f).padRight(5f)
        if (city.isPuppet)
            statusTable.add(ImageGetter.getImage("OtherIcons/Puppet").apply {
                color = if (royal) ink else Color.LIGHT_GRAY
            }).size(20f).padRight(5f)
        if (city.isInResistance())
            statusTable.add(ImageGetter.getImage("StatIcons/Resistance")).size(20f).padRight(5f)
        val label = city.name.toLabel(fontSize = 30, fontColor = ink)
        if (cityScreen.canChangeState) label.onClick {
            AskTextPopup(
                cityScreen,
                label = "Please enter a new name for your city",
                defaultText = city.name,
                validate = { it != "" },
                actionOnOk = { text ->
                    city.name = text
                    cityScreen.game.replaceCurrentScreen(cityScreen.createCityScreen())
                }
            ).open()
        }
        if (royal) {
            // Ширина учитывает обе области нажатия стрелок и внешние отступы.
            val panelWidth = (cityScreen.stage.width / 3f).coerceAtLeast(300f)
                .coerceAtMost(cityScreen.stage.width - 24f)
            val nameWidth = (panelWidth - 16f - if (hasNeighbours) 110f else 0f).coerceAtLeast(1f)
            label.setWrap(true)
            label.setAlignment(Align.center)
            cityNameTable.add(label).width(nameWidth).fillX().row()
            if (statusTable.hasChildren()) cityNameTable.add(statusTable).padTop(4f).row()
            add(cityNameTable).width(nameWidth)
        } else {
            cityNameTable.add(label)
            add(cityNameTable).width(cityScreen.stage.width / 4f)
        }
        addArrow(1)
        pack()
    }

}
