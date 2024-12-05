package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.utils.BaseScreen
import com.unciv.logic.army.TroopInfo
import com.unciv.ui.army.ArmyView

/**
 * Represents the view of a troop in battles.
 */
class TroopBattleView(
    private val troopInfo: TroopInfo,
    private val battleScreen: NewBattleScreen
    ) {
    private val troopGroup = Group()
    private var troopImages: ArrayList<Image>

    /** Initialize the troop's battle appearance. */
    // Deprecated. To be removed
    fun initialize(civColor: Color) {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, civColor, civColor)
    }

    init {
        val unitImagePath = "TileSets/AbsoluteUnits/Units/${troopInfo.unitName}"
        troopImages = ImageGetter.getLayeredImageColored(unitImagePath, null, null, null)
        troopGroup.name = "troopGroup"


    }

    fun getBattlefieldPosition(): Vector2 {
        return troopInfo.position
    }

    fun getTroopInfo(): TroopInfo{
        return troopInfo
    }

    /** Returns the current group of the troop for rendering. */
    fun getCurrentGroup(): Group {
        return troopGroup
    }


    // Глобальный флаг для управления логами (можно сделать локальным, если логика более узкая)
    private val DEBUG_LOGS_ENABLED = true


    /**
     * Updates the position of the troop in the view.
     * This moves the visual representation of the troop to the new position on the battlefield.
     *
     * @param tileGroups The list of tile groups for finding the target tile.
     */
    fun updatePosition(targetTileGroup: TileGroup?) {
        if (targetTileGroup == null) {
            if (DEBUG_LOGS_ENABLED) println("Error: Target tile group is null!")
            return
        }

        // Удаляем текущую привязку troopGroup к старому тайлу
        troopGroup.remove()
        if (DEBUG_LOGS_ENABLED) println("Troop removed from previous tile.")

        // Перемещаем troopGroup в новый тайл
        //troopGroup.setPosition(targetTileGroup.x, targetTileGroup.y)
        if (DEBUG_LOGS_ENABLED) println("Troop moved to new tile at position: (${targetTileGroup.x}, ${targetTileGroup.y})")

        // Добавляем troopGroup как дочерний элемент нового тайла
        targetTileGroup.addActor(troopGroup)
        targetTileGroup.update()
        if (DEBUG_LOGS_ENABLED) println("Troop added to target tile group.")


        // Обновляем логическую позицию troopInfo, чтобы соответствовать новому тайлу
        //troopInfo.position = targetTileGroup.tileInfo.position
        //if (DEBUG_LOGS_ENABLED) println("Troop logical position updated to: ${troopInfo.position}")
    }

    /** Draw the troop on the battle field. */
    fun draw(tileGroup: TileGroup, attacker: Boolean) {
        // Создаем или обновляем Label для отображения количества юнитов
        val amountLabel = Label(troopInfo.currentAmount.toString(), BaseScreen.skin).apply {
            name = "amountLabel" // Устанавливаем имя для последующего поиска
            setPosition(tileGroup.width * 0.5f, 0f) // Позиция внутри группы
        }

        // Устанавливаем изображение отряда
        for (troopImage in troopImages) {
            troopImage.setScale(if (attacker) -0.25f else 0.25f, 0.25f)
            troopImage.setPosition(
                if (attacker) tileGroup.width * 1.3f else tileGroup.width * -0.3f,
                tileGroup.height * 0.15f
            )
            troopImage.setOrigin(tileGroup.originX, tileGroup.originY)
            troopGroup.addActor(troopImage)
        }

        // Добавляем Label и всю группу отряда в текущий TileGroup
        troopGroup.addActor(amountLabel)
        tileGroup.addActor(troopGroup)
    }

    /** Show morale animation (e.g., after gaining morale). */
    fun showMoraleBird() {
        val moraleImage = ImageGetter.getExternalImage("MoraleBird.png").apply {
            setScale(0.075f)
            moveBy(troopGroup.parent.width * -0.035f, troopGroup.parent.height * 1.85f)
            touchable = Touchable.disabled
            color = Color.WHITE.cpy().apply { a = 0f }
        }

        moraleImage.addAction(
            Actions.sequence(
                Actions.alpha(1f, 0.5f),
                Actions.delay(0.3f),
                Actions.alpha(0f, 0.5f)
            )
        )

        troopGroup.addActor(moraleImage)
    }
    /** Обновляет визуальные параметры отряда (например, количество юнитов). */
    fun updateStats() {
        Gdx.app.postRunnable {
            // Проверяем, есть ли уже Label для отображения количества юнитов
            val amountLabel = troopGroup.findActor<Label>("amountLabel")
            if (amountLabel != null) {
                // Обновляем текст в существующем Label
                amountLabel.setText(troopInfo.currentAmount.toString())
            } else {
                // Если Label отсутствует, создаем его
                println("Amount Label not found")
                //val newAmountLabel = Label(troopInfo.currentAmount.toString(), BaseScreen.skin).apply {
                 //   name = "amountLabel" // Устанавливаем имя для поиска в будущем
                //    setPosition(troopGroup.width * 0.5f, 0f) // Позиция внутри группы
                //}
                //troopGroup.addActor(newAmountLabel) // Добавляем новый Label в группу
            }
            println("Troop stats updated: ${troopInfo.unitName}, Amount: ${troopInfo.currentAmount}")
        }
    }


    /** Remove the troop's group from the stage when it perishes. */
    fun perish() {
        troopGroup.remove()
    }
}
