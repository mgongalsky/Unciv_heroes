package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
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
import com.unciv.ui.utils.extensions.onActivation
import com.unciv.ui.utils.extensions.toTextButton
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor.SystemCursor
import com.unciv.ui.utils.extensions.toLabel

class BattleResultPopup(
    screen: BaseScreen,
    private val report: BattleReport,
    private val onContinue: () -> Unit
) : Popup(screen.stage, scrollable = false) {

    init {
        Gdx.graphics.setSystemCursor(SystemCursor.Arrow)
        defaults().pad(8f)
        add(fantasyLabel(resultTitle(), scale = 0.4f))
            .colspan(2).padTop(24f).padBottom(8f)
        row()
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
        add(resultText.toLabel(fontColor = Color.GOLD, fontSize = 24).apply {
            setAlignment(Align.center)
            setWrap(true)
        }).colspan(2).width(740f).row()
        if (score != null && report.winner != null) {
            add(
                "Army value remaining: ${score.remainingValue} / ${score.initialValue} coins"
                    .toLabel(fontSize = 18).apply {
                        setAlignment(Align.center)
                        setWrap(true)
                    }).colspan(2).width(740f).padBottom(8f).row()
        }
        add(
            sideTable(
                "Attacker", report.attackerLosses,
                report.winner == BattleSide.ATTACKER, true
            )
        ).top().width(390f)
        add(
            sideTable(
                "Defender", report.defenderLosses,
                report.winner == BattleSide.DEFENDER, false
            )
        ).top().width(390f)
        row()
        add(fantasyContinueButton()).colspan(2).width(190f).height(58f).padTop(18f)
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
        defaults().pad(4f)
        add(
            fantasyLabel(
                text = title,
                scale = 0.32f,
                color = if (isWinner) Color.GOLD else Color.LIGHT_GRAY
            )
        )
        row()
        add(fantasyLabel("Losses", scale = 0.26f)).padBottom(6f)
        row()
        add(lossStrip(losses, isAttacker)).center()
    }

    private fun lossStrip(losses: List<BattleLoss>, isAttacker: Boolean): Table =
            Table(BaseScreen.skin).apply {
                defaults().pad(4f)
                if (losses.isEmpty()) {
                    add(fantasyLabel("No losses", scale = 0.24f, color = Color.LIGHT_GRAY))
                } else {
                    losses.forEach { loss ->
                        add(lossCard(loss, isAttacker)).width(78f).top()
                    }
                }
            }

    private fun lossCard(loss: BattleLoss, isAttacker: Boolean): Table =
            Table(BaseScreen.skin).apply {
                add(battleSprite(loss.unitName, isAttacker)).size(64f)
                row()
                add(fantasyLabel("-${loss.amount}", scale = 0.25f)).padTop(2f)
            }

    private fun fantasyLabel(
        text: String,
        scale: Float,
        color: Color = Color.WHITE
    ) = Label(text, BaseScreen.skin, "fantasyLabel").apply {
        setFontScale(scale)
        setAlignment(Align.center)
        this.color = color
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
                    label.setFontScale(0.4f)
                    label.setAlignment(Align.center)
                    pad(5f, 10f, 5f, 10f)
                    onActivation {
                        close()
                        onContinue()
                    }
                }
}
