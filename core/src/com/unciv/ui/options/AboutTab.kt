package com.unciv.ui.options

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.UncivGame
import com.unciv.ui.civilopedia.FormattedLine
import com.unciv.ui.civilopedia.MarkupRenderer

fun aboutTab(parchmentWidth: Float? = null): Table {
    val versionAnchor = UncivGame.VERSION.text.replace(".", "")
    val textWidth = parchmentWidth?.let { (it - 45f).coerceAtLeast(1f) }
    val lines = sequence {
        yield(
            FormattedLine(
                extraImage = "banner",
                imageSize = textWidth?.coerceAtMost(240f) ?: 240f,
                centered = true
            )
        )
        yield(FormattedLine())
        yield(
            FormattedLine(
                "{Version}: ${UncivGame.VERSION.toNiceString()}",
                link = "https://github.com/yairm210/Unciv/blob/master/changelog.md#$versionAnchor"
            )
        )
        yield(
            FormattedLine(
                "See online Readme",
                link = "https://github.com/yairm210/Unciv/blob/master/README.md#unciv---foss-civ-v-for-androiddesktop"
            )
        )
        yield(FormattedLine("Visit repository", link = "https://github.com/yairm210/Unciv"))
    }.toList()
    return if (textWidth == null) MarkupRenderer.render(lines).pad(20f)
    else MarkupRenderer.renderWithTextColor(
        lines, com.badlogic.gdx.graphics.Color.valueOf("453925"), textWidth
    ).pad(20f)
}
