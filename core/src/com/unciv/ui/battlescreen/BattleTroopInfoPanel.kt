package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.RoyalPanelDrawable
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.max

/** Troop statistics; a null side is used when inspecting an army outside battle. */
class BattleTroopInfoPanel(
    private val troop: Troop,
    private val attacker: Boolean?
) : Table(BaseScreen.skin) {
    companion object {
        private const val EDGE_MARGIN = 12f
        private const val ANCHOR_GAP = 18f
    }

    private val royal = UncivGame.Current.settings.skin == "Minimal"
    private val textColor = if (royal) Color.valueOf("453925") else Color.WHITE
    private val secondaryColor = if (royal) Color.valueOf("655438") else Color.LIGHT_GRAY
    private val titleColor = if (royal) Color.valueOf("453925") else Color.valueOf("F5D77AFF")
    private var retaliationBudget: Int? = null
    private var normalRetaliationBudget: Int = 0

    init {
        name = "battleTroopInfoPanel"
        background = if (royal) RoyalPanelDrawable(BaseScreen.skin)
            else BaseScreen.skin.get("fantasy_background", NinePatchDrawable::class.java)
        touchable = Touchable.disabled
        if (royal) pad(52f).padTop(58f) else pad(18f).padTop(42f)
        defaults().pad(3f)
        buildContent()
        pack()
    }

    private fun buildContent() {
        val title = troop.unitName.toLabel(fontColor = titleColor, fontSize = 22).apply {
            if (royal && BaseScreen.skin.has("royal-display", BitmapFont::class.java)) {
                style = Label.LabelStyle(style).apply {
                    font = BaseScreen.skin.get("royal-display", BitmapFont::class.java)
                }
            }
            setAlignment(Align.center)
        }
        add(title).colspan(2).expandX().fillX().padBottom(8f).row()
        if (attacker != null) {
            add(createTroopPortrait()).size(144f, 180f).top().padRight(14f)
        } else {
            add(ImageGetter.getUnitIcon(troop.unitName, Color.WHITE)).size(72f).top().padRight(14f)
        }
        val stats = Table().apply {
            defaults().pad(2f, 4f, 2f, 4f)
            attacker?.let { statRow("Side", if (it) "Attacker" else "Defender") }
            val amount = if (attacker == null) troop.amount else troop.currentAmount
            statRow("Amount", amount.toString())
            statRow("Health", "${troop.currentHealth} / ${troop.maxHealth}")
            val totalHealth = if (amount <= 0) 0L else
                max(0, amount - 1).toLong() * troop.maxHealth + troop.currentHealth
            statRow("Total health", totalHealth.toString())
            statRow("Damage", troop.damage.toString())
            if (troop.isRanged) statRow("Ranged", troop.rangedStrength.toString())
            statRow("Speed", troop.speed.toString())
            statRow("Formation", "${troop.formation.current} / ${troop.formation.maximum}")
            retaliationBudget?.let { statRow("Retaliation", "$it / $normalRetaliationBudget") }
        }
        add(stats).top().left()
    }

    private fun Table.statRow(name: String, value: String) {
        add(name.toLabel(fontColor = secondaryColor, fontSize = 15)).left().padRight(12f)
        add(value.toLabel(fontColor = textColor, fontSize = 15)).right().row()
    }

    fun showNextTo(stage: Stage, anchor: Actor) {
        remove()
        stage.addActor(this)
        setScale(1f)
        pack()
        // This transient, non-interactive panel cannot be scrolled while holding a troop.
        // Fit its complete contents when the logical stage is smaller than its preferred size.
        val marginX = minOf(EDGE_MARGIN, stage.width / 4f)
        val marginY = minOf(EDGE_MARGIN, stage.height / 4f)
        val availableWidth = (stage.width - 2f * marginX).coerceAtLeast(0f)
        val availableHeight = (stage.height - 2f * marginY).coerceAtLeast(0f)
        val fitScale = minOf(
            1f,
            availableWidth / width.coerceAtLeast(1f),
            availableHeight / height.coerceAtLeast(1f)
        )
        setTransform(true)
        setOrigin(0f, 0f)
        setScale(fitScale)
        val displayedWidth = width * fitScale
        val displayedHeight = height * fitScale
        val bottomLeft = anchor.localToStageCoordinates(Vector2(0f, 0f))
        val topRight = anchor.localToStageCoordinates(Vector2(anchor.width, anchor.height))
        val anchorLeft = minOf(bottomLeft.x, topRight.x)
        val anchorRight = maxOf(bottomLeft.x, topRight.x)
        val anchorBottom = minOf(bottomLeft.y, topRight.y)
        val anchorTop = maxOf(bottomLeft.y, topRight.y)
        val rightX = anchorRight + ANCHOR_GAP
        val leftX = anchorLeft - displayedWidth - ANCHOR_GAP
        val targetX = if (rightX + displayedWidth <= stage.width - marginX) rightX else leftX
        val centeredY = (anchorBottom + anchorTop - displayedHeight) / 2f
        setPosition(
            targetX.coerceIn(marginX, max(marginX, stage.width - displayedWidth - marginX)),
            centeredY.coerceIn(marginY, max(marginY, stage.height - displayedHeight - marginY))
        )
        toFront()
    }

    fun setAttackPreview(
        preview: com.unciv.pure.application.battle.BattleAttackPreview?,
        remainingRetaliation: Int,
        attackUnavailable: Boolean,
        normalRetaliation: Int
    ) {
        retaliationBudget = remainingRetaliation
        normalRetaliationBudget = normalRetaliation
        clearChildren()
        buildContent()
        row()
        if (preview != null) {
            add(buildBattleAttackPreviewTable(preview, parchment = royal))
                .colspan(2).left().padTop(10f)
        } else if (attackUnavailable) {
            add(
                "Attack unavailable from this position".toLabel(
                    fontColor = secondaryColor,
                    fontSize = 14
                )
            ).colspan(2).left().padTop(8f)
        }
        pack()
    }

    private fun createTroopPortrait(): com.badlogic.gdx.scenes.scene2d.Group {
        val portrait = com.badlogic.gdx.scenes.scene2d.Group().apply {
            name = "battleTroopPortrait"
            touchable = Touchable.disabled
            setSize(144f, 180f)
        }
        val layers = ImageGetter.getLayeredImageColored(
            "TileSets/AbsoluteUnits/Units/${troop.unitName}", null, null, null
        )
        val sourceWidth = layers.maxOfOrNull { it.width } ?: 0f
        val sourceHeight = layers.maxOfOrNull { it.height } ?: 0f
        if (sourceWidth <= 0f || sourceHeight <= 0f) return portrait
        val scale = minOf(portrait.width / sourceWidth, portrait.height / sourceHeight)
        val left = (portrait.width - sourceWidth * scale) / 2f
        val bottom = (portrait.height - sourceHeight * scale) / 2f
        layers.forEach { layer ->
            layer.name = "portraitSprite"
            layer.setOrigin(0f, 0f)
            layer.setScale(if (attacker == true) -scale else scale, scale)
            layer.setPosition(if (attacker == true) left + sourceWidth * scale else left, bottom)
            layer.touchable = Touchable.disabled
            portrait.addActor(layer)
        }
        return portrait
    }
}
