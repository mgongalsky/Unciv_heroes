package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.pure.application.city.CityBuildingsSnapshot
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.disable
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import java.text.DecimalFormat

/** Карточки построек получают только снимок и действия по идентификаторам. */
internal class CityBuildingsTable(
    private val onInspect: (String) -> Unit,
    private val onSell: (String) -> Unit
) : Table() {
    private val ink = Color.valueOf("453925")
    private val blue = Color.valueOf("355A80")
    private val muted = Color.valueOf("81775F")
    private val rule = Color.valueOf("C9B98E")
    private val numberFormat = DecimalFormat("0.#")

    /** Перестраивает содержимое; прокруткой владеет страница городского экрана. */
    fun update(snapshot: CityBuildingsSnapshot, availableWidth: Float) {
        clearChildren()
        name = "cityBuildingsTable"
        top().left()
        val contentWidth = availableWidth.coerceAtLeast(1f)
        defaults().left()
        fun line(text: String, size: Int = 14, color: Color = ink) {
            add(text.toLabel(fontSize = size, fontColor = color).apply { setWrap(true) })
                .width(contentWidth).padBottom(5f).row()
        }
        line("City buildings", 22, blue)
        line(
            "{Buildings}: ${snapshot.buildings.size} · {Maintenance cost}: ${
                numberFormat.format(
                    snapshot.effectiveMaintenance
                )
            } {Gold}", 14, muted
        )
        line("{Base values}: ${snapshot.baseMaintenance} {Gold}", 12, muted)
        if (snapshot.buildings.isEmpty()) {
            line("No buildings", 16, muted)
            return
        }
        for (building in snapshot.buildings) {
            addSeparator(color = rule)
            val title = Table().apply { left() }
            val portrait = ImageGetter.getPortraitImage(building.name, 36f)
            if (building.civilopediaLink.isNotEmpty()) portrait.onClick { onInspect(building.civilopediaLink) }
            title.add(portrait).size(36f).padRight(8f)
            val nameLabel =
                    building.name.toLabel(fontSize = 18, fontColor = blue).apply { setWrap(true) }
            if (building.civilopediaLink.isNotEmpty()) nameLabel.onClick { onInspect(building.civilopediaLink) }
            title.add(nameLabel).width((contentWidth - 44f).coerceAtLeast(1f)).left()
            add(title).width(contentWidth).padTop(7f).padBottom(4f).row()
            if (building.isFree) line("Free", 12, muted)
            if (building.description.isNotBlank()) {
                // Описание уже локализовано адаптером, повторный перевод не нужен.
                val description = "".toLabel(fontSize = 14, fontColor = ink).apply {
                    setText(building.description)
                    setWrap(true)
                }
                add(description).width(contentWidth).padBottom(6f).row()
            }
            line(
                "{Base values} · {Maintenance cost}: ${building.baseMaintenance} {Gold}",
                12,
                muted
            )
            if (building.saleGold != null) {
                val sell =
                        ("Sell [${building.saleGold}] " + com.unciv.ui.utils.Fonts.gold).toTextButton()
                if (!building.canSell) sell.disable()
                HeroicCityStyle.applyCompactStyle(sell)
                sell.onClick { if (building.canSell) onSell(building.name) }
                add(sell).height(32f).padBottom(8f).right().row()
            }
        }
        invalidateHierarchy()
    }
}
