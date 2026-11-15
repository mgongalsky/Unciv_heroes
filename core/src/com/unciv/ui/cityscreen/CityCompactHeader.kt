package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.unciv.logic.city.CityFocus
import com.unciv.logic.city.INonPerpetualConstruction
import com.unciv.models.stats.Stat
import com.unciv.models.translations.tr
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.AskTextPopup
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.roundToInt

/** Компактная сводка общего городского экрана; игровое состояние остаётся в CityInfo. */
internal class CityCompactHeader(
    private val screen: CityScreen,
    private val exitButton: TextButton,
    private val contentWidth: Float
) : Table() {
    private val ink = Color.valueOf("453925")
    private val incomes = Table()
    private val incomeScroll = AutoScrollPane(incomes).apply {
        setScrollingDisabled(false, true)
        setOverscroll(false, false)
    }

    /** Перечитывает доходы, население и прогресс после существующих игровых команд. */
    fun update() {
        val city = screen.city
        val savedScroll = incomeScroll.scrollX
        clearChildren()
        top().left()
        val title = Table().apply { left() }
        fun arrow(direction: Int) {
            if (city.civInfo.cities.size <= 1) return
            val button = Table()
            val image = ImageGetter.getImage("OtherIcons/BackArrow").apply {
                color = ink
                setSize(18f, 18f)
                setOrigin(Align.center)
                if (direction > 0) rotation = 180f
            }
            button.add(image).size(18f).pad(5f)
            button.touchable = Touchable.enabled
            button.onClick { screen.page(direction) }
            title.add(button).size(28f)
        }
        arrow(-1)
        val name = city.name.toLabel(fontSize = 22, fontColor = ink).apply {
            setEllipsis(true)
            setAlignment(Align.left)
        }
        if (screen.canChangeState) name.onClick {
            AskTextPopup(screen,
                label = "Please enter a new name for your city",
                defaultText = city.name,
                validate = { it.isNotBlank() },
                actionOnOk = {
                    city.name = it
                    screen.game.replaceCurrentScreen(screen.createCityScreen())
                }
            ).open()
        }
        title.add(name).minWidth(0f).growX()
        title.add(ImageGetter.getStatIcon("Population")).size(18f).padLeft(5f)
        title.add(city.population.population.toString().toLabel(fontSize = 18, fontColor = ink)).padRight(5f)
        fun status(path: String) { title.add(ImageGetter.getImage(path)).size(16f).padRight(3f) }
        if (city.isCapital()) status("OtherIcons/Star")
        if (city.isBeingRazed) status("OtherIcons/Fire")
        if (city.isPuppet) status("OtherIcons/Puppet")
        if (city.isInResistance()) status("StatIcons/Resistance")
        arrow(1)
        HeroicCityStyle.applyCompactStyle(exitButton)
        title.add(exitButton).width(104f).height(32f).padLeft(6f)
        add(title).width(contentWidth).height(44f).row()

        incomes.clearChildren()
        incomes.left()
        fun focusCell(caption: String, stat: Stat?, value: String?) {
            val focus = if (stat == null) CityFocus.NoFocus else city.cityAIFocus.safeValueOf(stat)
            val cell = Table().apply {
                background = BaseScreen.skinStrings.getUiBackground("CityScreen/CityStatsTable/InnerTable",
                    tintColor = Color.valueOf(if (city.cityAIFocus == focus) "BCCCDD" else "EADFBD"))
                pad(3f)
                if (stat == null) add(caption.toLabel(fontSize = 16, fontColor = ink))
                else {
                    add(ImageGetter.getStatIcon(stat.name)).size(20f).padRight(3f)
                    val number = value!!.toLabel(fontSize = 16, fontColor = ink)
                    add(number).minWidth(40f).left()
                }
            }
            if (screen.canCityBeChanged()) {
                cell.touchable = Touchable.enabled
                cell.onClick {
                    city.cityAIFocus = if (city.cityAIFocus == focus) CityFocus.NoFocus else focus
                    city.reassignPopulation()
                    screen.update()
                }
            }
            incomes.add(cell).height(30f).padRight(3f)
        }
        focusCell("Default Focus", null, null)
        for ((stat, amount) in city.cityStats.currentCityStats) {
            if (stat == Stat.Faith && !city.civInfo.gameInfo.isReligionEnabled()) continue
            val value = if (stat == Stat.Happiness) city.cityStats.happinessList.values.sum() else amount
            focusCell(stat.name, stat, value.roundToInt().toString())
        }
        add(incomeScroll).width(contentWidth).height(36f).row()
        incomeScroll.validate()
        incomeScroll.scrollX = savedScroll
        incomeScroll.updateVisualScroll()

        val construction = city.cityConstructions
        val currentName = construction.currentConstructionFromQueue
        val project = Table().apply { left() }
        if (currentName.isEmpty()) {
            project.add("Pick a construction".toLabel(fontSize = 17, fontColor = ink)).left()
        } else {
            project.add(ImageGetter.getPortraitImage(currentName, 32f)).size(32f).padRight(6f)
            val details = Table().apply { left() }
            val label = (currentName.tr() + construction.getTurnsToConstructionString(currentName, true))
                .toLabel(fontSize = 17, fontColor = ink).apply { setWrap(true) }
            details.add(label).width((contentWidth - 152f).coerceAtLeast(1f)).left().row()
            val item = construction.getConstruction(currentName)
            if (item is INonPerpetualConstruction) {
                val cost = item.getProductionCost(city.civInfo)
                val work = construction.getWorkDone(currentName)
                val progress = ImageGetter.ProgressBar((contentWidth - 152f).coerceAtLeast(1f), 12f, false)
                progress.setBackground(Color.valueOf("C9BD9B"))
                progress.setProgress(Color.valueOf("52789B"), if (cost > 0) (work.toFloat() / cost).coerceIn(0f, 1f) else 0f)
                progress.setLabel(ink, "$work/$cost", fontSize = 12)
                details.add(progress).left()
            }
            project.add(details).growX()
            project.add(screen.constructionsTable.currentProjectActions()).width(104f).right()
        }
        add(project).width(contentWidth).minHeight(44f).padTop(3f).padBottom(4f).row()
        invalidateHierarchy()
    }
}
