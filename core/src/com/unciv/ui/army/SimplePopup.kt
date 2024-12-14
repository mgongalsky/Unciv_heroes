package com.unciv.ui.popup

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.utils.BaseScreen
import kotlin.math.roundToInt

/**
 * Test class for popups. Actual splitting class is SplitTroopPopup
 * A popup for splitting troops into two parts.
 * @param screen The screen from which the popup is opened.
 * @param troopView The TroopArmyView representing the troop.
 */
class SimplePopup(
    screen: BaseScreen,
    troopView: TroopArmyView
) : Popup(screen) {

    init {
        this.skin = BaseScreen.skin // Устанавливаем скин для попапа

        // Проверяем, что troopInfo не null
        if (troopView.troopInfo == null){
            println("Error: Troop info is null!")
            closePopup()
        } else {

            val totalValue = troopView.troopInfo.amount // Общее количество юнитов в отряде
            var leftValue = totalValue // Изначально все юниты слева
            var rightValue = 0 // Изначально справа ничего

            // Обертка для элементов
            val wrapper = Table(BaseScreen.skin)
            wrapper.defaults().pad(10f) // Устанавливаем отступы для элементов

            // Добавляем текст
            wrapper.add("Adjust troop split").row()

            // Лейблы для числовых значений
            val leftLabel = Label(leftValue.toString(), BaseScreen.skin)
            val rightLabel = Label(rightValue.toString(), BaseScreen.skin)

            // Ползунок
            val troopSlider = Slider(0f, totalValue.toFloat(), 1f, false, BaseScreen.skin)
            troopSlider.value = leftValue.toFloat()

            troopSlider.addListener { _ ->
                leftValue = troopSlider.value.roundToInt()
                rightValue = totalValue - leftValue
                leftLabel.setText(leftValue.toString())
                rightLabel.setText(rightValue.toString())
                false
            }

            // Таблица для ползунка и лейблов
            val sliderTable = Table()
            sliderTable.add(leftLabel).padRight(10f) // Левое числовое поле
            sliderTable.add(troopSlider).growX().pad(5f) // Ползунок
            sliderTable.add(rightLabel).padLeft(10f) // Правое числовое поле
            wrapper.add(sliderTable).growX().row()

            // Добавляем кнопку закрытия
            val closeButton = TextButton("Close", BaseScreen.skin)
            closeButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    println("Close button clicked") // Отладочное сообщение
                    closePopup()
                }
            })
            wrapper.add(closeButton).padTop(20f)

            // Добавляем обертку в попап
            add(wrapper).expand().fill().center()
        }
    }

    /**
     * Закрывает попап.
     */
    private fun closePopup() {
        println("Closing popup") // Логирование при закрытии
        this.remove() // Удаляем попап
    }
}
