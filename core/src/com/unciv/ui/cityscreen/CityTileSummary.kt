package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.city.CityInfo
import com.unciv.logic.map.TileInfo
import com.unciv.models.stats.Stat
import com.unciv.models.translations.tr
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.tilegroups.TileGroup
import com.unciv.ui.tilegroups.TileSetStrings
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.roundToInt

/** Сводка выбранной клетки: читает готовые доходы, не меняя население и состояние карты. */
internal class CityTileSummary(city: CityInfo, tile: TileInfo, contentWidth: Float) : Table() {
    init {
        name = "cityTileSummary"
        val ink = Color.valueOf("453925")
        val blue = Color.valueOf("355A80")
        val muted = Color.valueOf("81775F")
        top().left()
        defaults().left()
        add("Selected tile".toLabel(fontSize = 13, fontColor = muted)).row()

        // Превью имеет собственное дерево Actors; игровой гекс остаётся на карте.
        val preview = TileGroup(tile, TileSetStrings(), 76f).apply {
            update(city.civInfo, showResourcesAndImprovements = true, showTileYields = false)
            unitLayerGroup.isVisible = false
            unitImageLayerGroup.isVisible = false
            pixelMilitaryUnitGroup.isVisible = false
            pixelCivilianUnitGroup.isVisible = false
            cityButtonLayerGroup.isVisible = false
            borderLayerGroup.isVisible = false
            touchable = Touchable.disabled
        }
        val heading = Table().apply {
            defaults().left()
            val textWidth = (contentWidth - 100f).coerceAtLeast(1f)
            val title = (tile.naturalWonder ?: tile.terrainFeatures.lastOrNull() ?: tile.baseTerrain)
                .toLabel(fontSize = 26, fontColor = blue).apply { setWrap(true) }
            add(title).width(textWidth).row()
            add(tile.baseTerrain.toLabel(fontSize = 16, fontColor = muted).apply { setWrap(true) })
                .width(textWidth).padTop(4f).row()
            val ownership = tile.owningCity?.let { "Owned by [${it.name}]" } ?: "Unowned"
            add(ownership.toLabel(fontSize = 15, fontColor = ink).apply { setWrap(true) })
                .width(textWidth).padTop(5f).row()
            val status = when {
                city.isWorked(tile) -> "Worked by [${city.name}]"
                tile.getWorkingCity() != null -> "Worked by [${tile.getWorkingCity()!!.name}]"
                tile.owningCity == city -> "Within city lands"
                else -> "Outside city lands"
            }
            add(status.toLabel(fontSize = 13, fontColor = muted).apply { setWrap(true) })
                .width(textWidth).padTop(5f)
        }
        add(Table().apply {
            add(preview).size(76f).padRight(20f).top()
            add(heading).growX().top()
        }).width(contentWidth).padTop(12f).padBottom(18f).row()
        add(ImageGetter.getWhiteDot().apply { color = Color.valueOf("BEAD86") })
            .width(contentWidth).height(1f).row()
        add("Potential yield".toLabel(fontSize = 21, fontColor = blue)).padTop(12f).row()
        add("per turn".toLabel(fontSize = 13, fontColor = muted)).padBottom(8f).row()

        val stats = tile.getTileStats(city, city.civInfo)
        val displayedStats = mutableListOf(Stat.Food, Stat.Production, Stat.Gold,
            Stat.Science, Stat.Culture, Stat.Faith)
        if (stats[Stat.Happiness] != 0f) displayedStats.add(Stat.Happiness)
        val columns = if (contentWidth >= 360f) 3 else 2
        val cellWidth = contentWidth / columns
        val yields = Table().apply {
            defaults().left().top()
            for ((index, stat) in displayedStats.withIndex()) {
                val value = stats[stat]
                val rounded = value.roundToInt()
                val text = if (rounded > 0) "+$rounded" else rounded.toString()
                val entry = Table().apply {
                    defaults().left()
                    add(ImageGetter.getStatIcon(stat.name)).size(25f).padRight(5f)
                    add(text.toLabel(fontSize = 24, fontColor = if (value == 0f) muted else ink))
                        .expandX().left().row()
                    add(stat.name.tr().toLabel(fontSize = 14, fontColor = muted).apply { setWrap(true) })
                        .colspan(2).width((cellWidth - 12f).coerceAtLeast(1f)).padTop(3f)
                }
                add(entry).width(cellWidth).padBottom(14f)
                if ((index + 1) % columns == 0) row()
            }
        }
        add(yields).width(contentWidth).row()
        add(ImageGetter.getWhiteDot().apply { color = Color.valueOf("BEAD86") })
            .width(contentWidth).height(1f).padBottom(12f).row()
    }
}
