package com.unciv.ui.newgamescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.MapParameters
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.UncivTextField
import com.unciv.ui.utils.extensions.onChange
import com.unciv.ui.utils.extensions.toCheckBox
import com.unciv.ui.utils.extensions.toLabel

/** Поля широт редактора; ошибочный черновик не подменяется молча допустимым значением. */
class MapLatitudeControls(private val parameters: MapParameters) : Table(BaseScreen.skin) {
    private var refreshing = false
    private val enabled = "Use latitude range".toCheckBox(parameters.useLatitudeRange) {
        if (!refreshing) {
            parameters.useLatitudeRange = it
            updateState()
        }
    }.apply { name = "map-latitude-enabled" }
    private val top = UncivTextField.create("Top latitude", parameters.topLatitude.toString())
        .apply { name = "map-top-latitude" }
    private val bottom = UncivTextField.create("Bottom latitude", parameters.bottomLatitude.toString())
        .apply { name = "map-bottom-latitude" }
    private val error = "".toLabel(Color.RED).apply {
        name = "map-latitude-error"
        wrap = true
    }

    init {
        defaults().pad(5f)
        add(enabled).colspan(2).left().row()
        add("Top latitude (degrees)".toLabel()).left()
        add(top).width(160f).row()
        add("Bottom latitude (degrees)".toLabel()).left()
        add(bottom).width(160f).row()
        add("North +90; equator 0; south -90".toLabel()).colspan(2).left().row()
        add(error).colspan(2).width(360f).left().row()
        top.onChange { readFields() }
        bottom.onChange { readFields() }
        updateState()
    }

    /** Обновляет поля после Reset без промежуточной записи устаревшего черновика. */
    fun refresh() {
        refreshing = true
        try {
            enabled.isChecked = parameters.useLatitudeRange
            top.text = parameters.topLatitude.toString()
            bottom.text = parameters.bottomLatitude.toString()
        } finally {
            refreshing = false
        }
        updateState()
    }

    private fun readFields() {
        if (refreshing) return
        parameters.topLatitude = top.text.trim().toDoubleOrNull() ?: Double.NaN
        parameters.bottomLatitude = bottom.text.trim().toDoubleOrNull() ?: Double.NaN
        updateState()
    }

    /** Вызывается также при смене формы: Flat Earth нельзя совместить с диапазоном. */
    fun updateState() {
        top.isDisabled = !parameters.useLatitudeRange
        bottom.isDisabled = !parameters.useLatitudeRange
        val message = try {
            parameters.validatedLatitudeRange()
            ""
        } catch (invalid: IllegalArgumentException) {
            invalid.message.orEmpty()
        }
        error.setText(message)
        invalidateHierarchy()
    }
}
