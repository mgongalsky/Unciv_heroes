package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.unciv.models.translations.tr
import com.unciv.pure.application.city.CityHeroSupplySnapshot
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

/** Панель снабжения получает только значения и callbacks; игровая модель остаётся в адаптере. */
internal class CityHeroSupplyTable(
    private val onTransfer: (Long, Float) -> CityHeroSupplySnapshot,
    private val onMaximum: (Long) -> CityHeroSupplySnapshot,
    private val onToggle: (Long) -> CityHeroSupplySnapshot,
    private val onChanged: () -> Unit
) : Table() {
    private var slider: Slider? = null
    private val ink = Color.valueOf("453925")
    private val blue = Color.valueOf("355A80")

    /** Округляет только подпись; игровые запасы и расход сохраняют исходную точность. */
    private fun number(value: Float): String =
            java.math.BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()

    /** Перестройка не прерывает жест и не посылает команду от программной установки ползунка. */
    fun update(snapshot: CityHeroSupplySnapshot, contentWidth: Float) {
        if (slider?.isDragging == true) return
        clear()
        slider = null
        name = "cityHeroSupplyTable"
        top().left()
        val available = contentWidth.coerceAtLeast(1f)
        fun line(text: String, size: Int = 16, color: Color = ink): Label {
            val label = text.toLabel(fontColor = color, fontSize = size)
            label.setWrap(true)
            add(label).width(available).left().padBottom(6f).row()
            return label
        }
        line("Hero Supply", 22, blue)
        if (!snapshot.enabled) {
            line("Supply is disabled")
            return
        }
        if (!snapshot.hasHero) {
            line("No visiting hero")
            return
        }
        val stocks = Table()
        val cityLabel = "".toLabel(fontColor = ink, fontSize = 18).apply { setWrap(true) }
        val heroLabel = "".toLabel(fontColor = ink, fontSize = 18).apply { setWrap(true) }
        val half = ((available - 12f) / 2f).coerceAtLeast(1f)
        stocks.add(cityLabel).width(half).left().padRight(12f)
        stocks.add(heroLabel).width(half).left()
        add(stocks).width(available).padBottom(8f).row()
        fun showStocks(amount: Float) {
            cityLabel.setText("City stores".tr() + ": ${number(snapshot.cityFood - amount)} ${Fonts.food}")
            heroLabel.setText("Hero's provisions".tr() + ": ${number(snapshot.heroFood + amount)} / ${number(snapshot.manualCapacity)} ${Fonts.food}")
        }
        showStocks(0f)
        val duration = if (snapshot.maintenance > 0f)
            "${(snapshot.heroFood / snapshot.maintenance).toInt().coerceAtLeast(0)} ${Fonts.turn}"
        else "Indefinitely".tr()
        line("${number(snapshot.maintenance)} ${Fonts.food} / " + "turn".tr() +
            "   ·   $duration   ·   " + "Morale".tr() + ": ${snapshot.morale}")
        if (snapshot.manualCapacity != snapshot.automaticCapacity) {
            line("Auto-Supply capacity".tr() + ": ${number(snapshot.automaticCapacity)} ${Fonts.food}", 14)
        }
        val foodSlider = Slider(snapshot.minimumTransfer, snapshot.maximumTransfer, 1f, false, BaseScreen.skin)
        foodSlider.name = "cityHeroSupplySlider"
        foodSlider.setProgrammaticChangeEvents(false)
        foodSlider.value = 0f
        foodSlider.isDisabled = !snapshot.canChange || snapshot.minimumTransfer == snapshot.maximumTransfer
        slider = foodSlider
        foodSlider.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!snapshot.canChange) return
                if (foodSlider.isDragging) {
                    showStocks(foodSlider.value)
                    return
                }
                // Slider завершает жест ChangeEvent даже при неизменном последнем значении.
                val result = onTransfer(snapshot.revision, foodSlider.value)
                update(result, available)
                onChanged()
            }
        })
        add(foodSlider).width(available).height(32f).padBottom(8f).row()
        val buttons = Table()
        val maximum = "Max".toTextButton().apply {
            name = "cityHeroSupplyMax"
            isDisabled = !snapshot.canChange || snapshot.maximumTransfer <= 0f
            onClick {
                if (!isDisabled) {
                    update(onMaximum(snapshot.revision), available)
                    onChanged()
                }
            }
        }
        val automatic = (if (snapshot.autoSupply) "Auto-Supply: ON" else "Auto-Supply: OFF").toTextButton().apply {
            name = "cityHeroSupplyAuto"
            isDisabled = !snapshot.canChange
            onClick {
                if (!isDisabled) {
                    update(onToggle(snapshot.revision), available)
                    onChanged()
                }
            }
        }
        maximum.label.setWrap(true)
        automatic.label.setWrap(true)
        buttons.add(maximum).width((available - 8f) * 0.3f).height(44f).padRight(8f)
        buttons.add(automatic).width((available - 8f) * 0.7f).height(44f)
        add(buttons).width(available).padBottom(6f).row()
        line("Slide to share food between city and hero.", 14)
        invalidateHierarchy()
    }
}
