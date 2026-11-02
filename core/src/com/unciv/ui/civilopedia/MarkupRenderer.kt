package com.unciv.ui.civilopedia

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.addSeparator
import com.unciv.ui.utils.extensions.onClick

/** Makes formatted text rendering available outside ICivilopediaText. */
object MarkupRenderer {
    private const val emptyLineHeight = 10f
    private const val defaultPadding = 2.5f
    private const val separatorTopPadding = 10f
    private const val separatorBottomPadding = 10f

    /** Preserves the existing default colors and positional/trailing-lambda API. */
    fun render(
        lines: Collection<FormattedLine>,
        labelWidth: Float = 0f,
        padding: Float = defaultPadding,
        iconDisplay: FormattedLine.IconDisplay = FormattedLine.IconDisplay.All,
        linkAction: ((id: String) -> Unit)? = null
    ): Table = renderWithTextColor(lines, null, labelWidth, padding, iconDisplay, linkAction)

    /** Overrides ordinary text only; explicit colors, icons and separators retain their colors. */
    fun renderWithTextColor(
        lines: Collection<FormattedLine>,
        defaultTextColor: Color?,
        labelWidth: Float = 0f,
        padding: Float = defaultPadding,
        iconDisplay: FormattedLine.IconDisplay = FormattedLine.IconDisplay.All,
        linkAction: ((id: String) -> Unit)? = null
    ): Table {
        val table = Table(BaseScreen.skin).apply { defaults().pad(padding).align(Align.left) }
        for (line in lines) {
            if (line.isEmpty()) {
                table.add().padTop(emptyLineHeight).row()
                continue
            }
            if (line.separator) {
                table.addSeparator(line.displayColor, 1, if (line.size == Int.MIN_VALUE) 2f else line.size.toFloat())
                    .pad(separatorTopPadding, 0f, separatorBottomPadding, 0f)
                continue
            }
            val actor = line.render(labelWidth, iconDisplay, defaultTextColor)
            if (line.linkType == FormattedLine.LinkType.Internal && linkAction != null)
                actor.onClick { linkAction(line.link) }
            else if (line.linkType == FormattedLine.LinkType.External)
                actor.onClick { Gdx.net.openURI(line.link) }
            if (labelWidth == 0f)
                table.add(actor).align(line.align).row()
            else
                table.add(actor).width(labelWidth).align(line.align).row()
        }
        return table.apply { pack() }
    }
}
