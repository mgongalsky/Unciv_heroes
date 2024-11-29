package com.unciv.ui.popup

import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.unciv.ui.army.TroopArmyView
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.roundToInt

/**
 * Popup for splitting a troop into two parts.
 * @param screen The screen from which the popup is opened.
 * @param troopView The TroopArmyView representing the troop.
 * @param onSplit Lambda that gets the split result: (leftTroopCount, rightTroopCount).
 */
class SplitTroopPopup(
    screen: BaseScreen,
    troopView: TroopArmyView,
    onSplit: (Int, Int) -> Unit
) : Popup(screen) {

    init {

        // Проверяем, что troopInfo не null
        if (troopView.troopInfo == null) {
            closePopup() // Закрываем попап, если слот пустой
        } else {
            val troopCount = troopView.troopInfo.currentAmount

            // Устанавливаем минимальные размеры попапа
            this.setSize(400f, 300f) // Можно настроить размеры на ваше усмотрение

            // Wrapper для иконки и текста
            val wrapper = Table()
            wrapper.defaults().pad(10f) // Задает отступы для всех элементов внутри wrapper

            // Добавляем аватар из TroopArmyView
            troopView.drawAvatar()
            wrapper.add(troopView).size(80f).padBottom(10f).row()

            // Текст "Adjust troop split"
            val label = "Adjust troop split".toLabel(fontSize = 20)
            wrapper.add(label).expandX().left().padBottom(10f).row()

            // Лейблы для левой и правой частей
            val leftCountLabel = Label("0", BaseScreen.skin)
            val rightCountLabel = Label(troopCount.toString(), BaseScreen.skin)

            // Ползунок
            val troopSlider = Slider(0f, troopCount.toFloat(), 1f, false, BaseScreen.skin)
            troopSlider.value = 0f
            troopSlider.addListener { _ ->
                val leftCount = troopSlider.value.roundToInt()
                val rightCount = troopCount - leftCount
                leftCountLabel.setText(leftCount.toString())
                rightCountLabel.setText(rightCount.toString())
                false
            }

            // Таблица для ползунка и лейблов
            val sliderTable = Table()
            sliderTable.defaults().pad(5f) // Общие отступы для всех элементов в sliderTable
            sliderTable.add(leftCountLabel).padRight(10f)
            sliderTable.add(troopSlider).growX().pad(5f)
            sliderTable.add(rightCountLabel).padLeft(10f)
            wrapper.add(sliderTable).growX().row()

            add(wrapper).expand().fill().row() // Добавляем wrapper в попап

            // Добавляем кнопки OK и Cancel
            addCloseButton()
            addOKButton {
                val leftCount = troopSlider.value.roundToInt()
                val rightCount = troopCount - leftCount
                onSplit(leftCount, rightCount)
            }
            equalizeLastTwoButtonWidths()
        }
    }

    /**
     * Закрывает попап, если слот пустой.
     */
    private fun closePopup() {
        this.remove() // Удаляем попап со сцены
    }
}

