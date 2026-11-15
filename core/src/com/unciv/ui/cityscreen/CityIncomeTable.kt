package com.unciv.ui.cityscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.pure.application.city.CityStatSource
import com.unciv.pure.application.city.CityStatsSnapshot
import com.unciv.pure.application.city.CityStatValues
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import java.text.DecimalFormat

/** Отображение снимка доходов: не обращается к городу и не рассчитывает игровые показатели. */
internal class CityIncomeTable : Table() {
    private val ink = Color.valueOf("453925")
    private val blue = Color.valueOf("355A80")
    private val muted = Color.valueOf("81775F")
    private val negative = Color.valueOf("A44732")
    private val numberFormat = DecimalFormat("0.#")

    /** Компактная сводка; дополнительные источники сохраняют доступную прокрутку. */
    fun update(snapshot: CityStatsSnapshot, availableWidth: Float) {
        val savedPositions = children.filterIsInstance<AutoScrollPane>().map { it.scrollX }
        clearChildren()
        name = "cityIncomeTable"
        top().left()
        val contentWidth = availableWidth.coerceAtLeast(1f)
        defaults().left()
        add("Income & sources".toLabel(fontSize = 18, fontColor = blue)).padBottom(2f).row()
        val summary =
            "{Population}: ${snapshot.population} · {Food}: ${snapshot.foodStored} · {Gold}: ${snapshot.civilizationGold}"
        add(summary.toLabel(fontSize = 12, fontColor = muted).apply { setWrap(true) })
            .width(contentWidth).padBottom(4f).row()

        fun section(
            title: String,
            sources: List<CityStatSource>,
            percentage: Boolean,
            totals: CityStatValues?
        ) {
            add(title.toLabel(fontSize = 14, fontColor = blue)).padTop(4f).padBottom(3f).row()
            val visibleSources = sources.filter { source ->
                snapshot.resources.any { source.values[it] != 0f }
            }
            val matrix = Table().apply {
                top().left()
                if (totals != null) name = "cityIncomeFinalMatrix"
            }
            val resourceWidth = minOf(104f, contentWidth * 0.24f)
            val columnCount = visibleSources.size + if (totals != null) 1 else 0
            // Обычная сводка помещается целиком; множество источников модов не сжимаем до нечитаемого текста.
            val sourceWidth = if (columnCount == 0) 0f else
                kotlin.math.floor((contentWidth - resourceWidth) / columnCount).coerceIn(48f, 96f)
            fun cell(text: String, width: Float, color: Color = ink, header: Boolean = false) {
                val label = text.toLabel(fontSize = if (header) 11 else 14, fontColor = color)
                label.setWrap(true)
                label.setAlignment(Align.center)
                val holder = Table()
                if (header) holder.background = BaseScreen.skinStrings.getUiBackground(
                    "CityScreen/CityStatsTable/InnerTable", tintColor = Color.valueOf("DDD0AB")
                )
                holder.add(label).width((width - 6f).coerceAtLeast(1f)).pad(3f)
                matrix.add(holder).width(width).minHeight(if (header) 28f else 24f).fill()
            }
            cell("Resource", resourceWidth, muted, true)
            for (source in visibleSources) cell(source.name, sourceWidth, muted, true)
            if (totals != null) cell("Final", sourceWidth, blue, true)
            matrix.row()
            for (resource in snapshot.resources) {
                // Перевод уже добавляет значок ресурса: отдельное изображение дублировало его.
                val heading = resource.name.toLabel(fontSize = 13, fontColor = ink).apply {
                    setWrap(true)
                    setAlignment(Align.left)
                }
                val holder = Table()
                holder.add(heading).width((resourceWidth - 6f).coerceAtLeast(1f)).pad(3f)
                matrix.add(holder).width(resourceWidth).minHeight(24f).fill()
                fun value(values: CityStatValues, total: Boolean = false) {
                    val amount = values[resource]
                    val text = if (amount == 0f && !total) "—"
                        else numberFormat.format(amount) + if (percentage) "%" else ""
                    cell(text, sourceWidth, when {
                        amount < 0f -> negative
                        total -> blue
                        amount == 0f -> muted
                        else -> ink
                    })
                }
                for (source in visibleSources) value(source.values)
                if (totals != null) value(totals, true)
                matrix.row()
            }
            matrix.pack()
            val needsHorizontalScroll = matrix.prefWidth > contentWidth + 0.5f
            val scroll = AutoScrollPane(matrix).apply {
                setScrollingDisabled(!needsHorizontalScroll, true)
                setOverscroll(false, false)
                setFadeScrollBars(false)
            }
            // Не резервируем полосу прокрутки, когда все столбцы помещаются в панель.
            add(scroll).width(contentWidth)
                .height(matrix.prefHeight + if (needsHorizontalScroll) 16f else 0f)
                .padBottom(4f).row()
        }
        section("Final", snapshot.finalSources, false, snapshot.totals)
        section("Base values", snapshot.baseSources, false, null)
        section("Bonuses (%)", snapshot.percentageSources, true, null)

        // Родитель уже включает детей; путь сохраняет различимость одноимённых источников.
        fun details(
            sources: List<CityStatSource>,
            percentage: Boolean,
            parents: List<String> = emptyList()
        ) {
            for (source in sources) {
                val path = parents + source.name
                if (parents.isNotEmpty()) {
                    val values = snapshot.resources.filter { source.values[it] != 0f }
                    if (values.isNotEmpty()) {
                        val text = path.joinToString(" / ") { "{$it}" } + ": " +
                            values.joinToString(" · ") {
                                "{${it.name}} " + numberFormat.format(source.values[it]) +
                                    if (percentage) "%" else ""
                            }
                        add(text.toLabel(fontSize = 12, fontColor = muted).apply { setWrap(true) })
                            .width(contentWidth).padBottom(3f).row()
                    }
                }
                details(source.children, percentage, path)
            }
        }
        if (snapshot.baseSources.any { it.children.isNotEmpty() }) {
            add("Base values — Details".toLabel(fontSize = 14, fontColor = blue)).padTop(4f).row()
            details(snapshot.baseSources, false)
        }
        if (snapshot.percentageSources.any { it.children.isNotEmpty() }) {
            add("Bonuses (%) — Details".toLabel(fontSize = 14, fontColor = blue)).padTop(4f).row()
            details(snapshot.percentageSources, true)
        }
        children.filterIsInstance<AutoScrollPane>().forEachIndexed { index, scroll ->
            scroll.validate()
            scroll.scrollX = savedPositions.getOrElse(index) { 0f }.coerceIn(0f, scroll.maxX)
            scroll.updateVisualScroll()
        }
        invalidateHierarchy()
    }
}
