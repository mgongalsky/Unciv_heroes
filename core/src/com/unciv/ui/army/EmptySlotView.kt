package com.unciv.ui.army

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.unciv.ui.images.ImageGetter

/**
 * !! Obsolete!
 * A simple placeholder view for empty slots in the [ArmyView].
 */
class EmptySlotView : Group() {

    init {
        // Используем изображение для пустого слота
        // TODO: Create an image for EmptySlot
        val emptySlotImage = ImageGetter.getImage("OtherIcons/Wait") // Replace with your placeholder image path

        // Устанавливаем размер и добавляем изображение в группу
        emptySlotImage.setSize(64f, 64f) // Размер пустого слота
        addActor(emptySlotImage) // Добавляем изображение как дочерний элемент
    }

    fun updateView() {
        // Пустая реализация, если для пустого слота ничего обновлять не нужно
        println("Updating EmptySlotView")
    }

}
