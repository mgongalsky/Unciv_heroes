package com.unciv.ui.heroscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.pure.application.hero.HeroOverviewSnapshot
import com.unciv.ui.utils.Fonts
import com.unciv.ui.utils.extensions.toLabel

/** Общие подписи и значения героя; оформление контейнера задаёт принимающий экран. */
class HeroDetailsTable(private val parchment: Boolean) : Table() {
    private val ink = if (parchment) Color.valueOf("453925") else Color.WHITE
    private val headingColor = if (parchment) Color.valueOf("355A80") else Color.WHITE

    /** Перестраивает только представление снимка, не выполняя игровых команд. */
    fun update(snapshot: HeroOverviewSnapshot, contentWidth: Float) {
        clear()
        top().left()
        val available = contentWidth.coerceAtLeast(1f)
        fun label(text: String, heading: Boolean = false): Label =
            text.toLabel(
                fontColor = if (heading) headingColor else ink,
                fontSize = if (heading) 22 else 18
            ).apply {
                setWrap(true)
                setAlignment(Align.left)
            }
        fun heading(text: String) {
            add(label(text, true)).colspan(2).width(available)
                .left().padTop(10f).padBottom(10f).row()
        }
        fun stat(text: String, value: String) {
            add(label(text)).width((available * 0.7f - 10f).coerceAtLeast(1f))
                .left().pad(5f)
            add(label(value).apply { setAlignment(Align.right) })
                .width((available * 0.3f - 10f).coerceAtLeast(1f))
                .right().pad(5f).row()
        }
        fun paragraph(text: String) {
            add(label(text)).colspan(2).width((available - 10f).coerceAtLeast(1f))
                .left().pad(5f).row()
        }

        heading(snapshot.name)
        heading("Hero Skills")
        stat("Attack Skill:", snapshot.attack.toString())
        stat("Defense Skill:", snapshot.defense.toString())
        stat("Morale:", snapshot.morale.toString())
        stat("Luck:", snapshot.luck.toString())
        if (snapshot.supplyEnabled) {
            heading("Food supply")
            paragraph(
                "Now ${snapshot.food.toInt()}${Fonts.food} of max ${snapshot.overviewCapacity.toInt()}${Fonts.food}. Per turn when outside city: %.1f${Fonts.food}."
                    .format(snapshot.outsideMaintenance)
            )
            paragraph(
                if (snapshot.foodDuration == Int.MAX_VALUE) "Food will last indefinitely."
                else "Food will last for ${snapshot.foodDuration}${Fonts.turn}."
            )
        }
        heading("City Settlement")
        paragraph("Can settle city with ${snapshot.settlementPopulation}${Fonts.population}")
        invalidateHierarchy()
    }
}
