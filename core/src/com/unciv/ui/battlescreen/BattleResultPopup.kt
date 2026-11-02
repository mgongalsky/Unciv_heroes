package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle
import com.badlogic.gdx.utils.Align
import com.unciv.pure.domain.battle.BattleLoss
import com.unciv.pure.domain.battle.BattleReport
import com.unciv.pure.domain.battle.BattleSide
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

class BattleResultPopup(
    screen: BaseScreen,
    private val report: BattleReport,
    private val onContinue: () -> Unit
) : Popup(screen.stage, scrollable = true) {
    private val royal = screen.game.settings.skin == "Minimal"
    private val textColor = if (royal) Color.valueOf("453925") else Color.WHITE
    private val secondaryColor = if (royal) Color.valueOf("655438") else Color.LIGHT_GRAY
    private val accentColor = if (royal) Color.valueOf("8a632e") else Color.GOLD
    private val framePadding = if (royal) 52f else 20f
    private val contentWidth = minOf(780f, screen.stage.width - 2f * framePadding - 48f)
        .coerceAtLeast(96f)
    private val sideColumns = if (contentWidth >= 560f) 2 else 1
    private val sideWidth = if (sideColumns == 2) (contentWidth - 16f) / 2f else contentWidth

    init {
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        if (royal) {
            innerTable.background = RoyalPanelDrawable(BaseScreen.skin)
            innerTable.pad(framePadding).padTop(58f)
        }
        defaults().pad(8f)
        add(fantasyLabel(resultTitle(), scale = 0.4f).apply {
            name = "battleResultTitle"
            setWrap(true)
        }).colspan(sideColumns).width(contentWidth)
            .padTop(if (royal) 4f else 24f).padBottom(8f).row()
        val score = report.winnerScore
        val resultText = when {
            report.winner == null -> "Result: 0%"
            score == null -> "Result unavailable: recruitment prices are missing."
            else -> "Winner result: ${
                String.format(
                    java.util.Locale.getDefault(),
                    "%.1f",
                    score.percent
                )
            }%"
        }
        add(resultText.toLabel(fontColor = accentColor, fontSize = 24).apply {
            name = "battleResultScore"
            setAlignment(Align.center)
            setWrap(true)
        }).colspan(sideColumns).width(contentWidth).row()
        if (score != null && report.winner != null) {
            add(
                "Army value remaining: ${score.remainingValue} / ${score.initialValue} coins"
                    .toLabel(fontColor = textColor, fontSize = 18).apply {
                        setAlignment(Align.center)
                        setWrap(true)
                    }).colspan(sideColumns).width(contentWidth).padBottom(8f).row()
        }
        add(
            sideTable(
                "Attacker", report.attackerLosses,
                report.winner == BattleSide.ATTACKER, true
            )
        ).top().width(sideWidth)
        if (sideColumns == 1) row()
        add(
            sideTable(
                "Defender", report.defenderLosses,
                report.winner == BattleSide.DEFENDER, false
            )
        ).top().width(sideWidth)
        row()
        add(fantasyContinueButton()).colspan(sideColumns)
            .width(minOf(190f, contentWidth)).height(58f).padTop(18f)
    }

    private fun resultTitle(): String = when (report.winner) {
        BattleSide.ATTACKER -> "Attacker victory"
        BattleSide.DEFENDER -> "Defender victory"
        null -> "Mutual defeat"
    }

    private fun sideTable(
        title: String,
        losses: List<BattleLoss>,
        isWinner: Boolean,
        isAttacker: Boolean
    ): Table = Table(BaseScreen.skin).apply {
        name = if (isAttacker) "battleResultAttacker" else "battleResultDefender"
        defaults().pad(4f)
        add(
            fantasyLabel(
                text = title,
                scale = 0.32f,
                color = if (isWinner) accentColor else secondaryColor
            ).apply { setWrap(true) }
        ).width(sideWidth - 8f).row()
        add(fantasyLabel("Losses", scale = 0.26f).apply { setWrap(true) })
            .width(sideWidth - 8f).padBottom(6f).row()
        add(lossStrip(losses, isAttacker)).center()
    }

    private fun lossStrip(losses: List<BattleLoss>, isAttacker: Boolean): Table =
        Table(BaseScreen.skin).apply {
            defaults().pad(4f)
            if (losses.isEmpty()) {
                add(fantasyLabel("No losses", scale = 0.24f, color = secondaryColor).apply {
                    setWrap(true)
                }).width(sideWidth - 16f)
            } else {
                val columns = ((sideWidth - 8f) / 86f).toInt().coerceAtLeast(1)
                losses.forEachIndexed { index, loss ->
                    if (index > 0 && index % columns == 0) row()
                    add(lossCard(loss, isAttacker)).width(78f).top()
                }
            }
        }

    private fun lossCard(loss: BattleLoss, isAttacker: Boolean): Table =
        Table(BaseScreen.skin).apply {
            add(battleSprite(loss.unitName, isAttacker)).size(64f)
            row()
            add(fantasyLabel("-${loss.amount}", scale = 0.25f).apply { setWrap(true) })
                .width(78f).padTop(2f)
        }

    private fun fantasyLabel(
        text: String,
        scale: Float,
        color: Color = textColor
    ) = Label(text, Label.LabelStyle(BaseScreen.skin.get("fantasyLabel", Label.LabelStyle::class.java)).apply {
        fontColor = color.cpy()
    }).apply {
        setFontScale(scale)
        setAlignment(Align.center)
    }

    private fun battleSprite(unitName: String, isAttacker: Boolean): Group {
        val size = 64f
        return Group().apply {
            setSize(size, size)
            ImageGetter.getLayeredImageColored(
                "TileSets/AbsoluteUnits/Units/$unitName",
                null,
                null,
                null
            ).forEach { layer ->
                layer.setSize(size, size)
                layer.setOrigin(Align.center)
                if (isAttacker) layer.setScale(-1f, 1f)
                addActor(layer)
            }
        }
    }

    private fun fantasyContinueButton() =
        "Continue".toTextButton(BaseScreen.skin.get("fantasy", TextButtonStyle::class.java))
            .apply {
                name = "battleResultContinue"
                label.setFontScale(0.4f)
                label.setAlignment(Align.center)
                pad(5f, 10f, 5f, 10f)
                onActivation {
                    close()
                    onContinue()
                }
            }
}
