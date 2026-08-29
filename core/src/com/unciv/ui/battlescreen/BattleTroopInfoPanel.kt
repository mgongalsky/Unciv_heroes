package com.unciv.ui.battlescreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.Align
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.extensions.toLabel
import kotlin.math.max

class BattleTroopInfoPanel(
    private val troop: Troop,
    private val attacker: Boolean
) : Table(BaseScreen.skin) {

    companion object {
        private const val EDGE_MARGIN = 12f
        private const val ANCHOR_GAP = 18f
    }

    init {
        name = "battleTroopInfoPanel"
        background = BaseScreen.skin.get("fantasy_background", NinePatchDrawable::class.java)
        touchable = Touchable.disabled
        pad(18f)
        defaults().pad(3f)
        buildContent()
        pack()
    }

    private fun buildContent() {
        val title =
            troop.unitName.toLabel(fontColor = Color.valueOf("F5D77AFF"), fontSize = 22).apply {
                setAlignment(Align.center)
            }
        add(title).colspan(2).expandX().fillX().padBottom(8f).row()

        val portrait = ImageGetter.getUnitIcon(troop.unitName, Color.WHITE)
        add(portrait).size(72f).top().padRight(14f)

        val stats = Table().apply {
            defaults().pad(2f, 4f, 2f, 4f)
            statRow("Side", if (attacker) "Attacker" else "Defender")
            statRow("Amount", troop.currentAmount.toString())
            statRow("Health", "${troop.currentHealth} / ${troop.maxHealth}")
            val totalHealth =
                max(0, troop.currentAmount - 1) * troop.maxHealth + troop.currentHealth
            statRow("Total health", totalHealth.toString())
            statRow("Damage", troop.damage.toString())
            if (troop.isRanged) statRow("Ranged", troop.rangedStrength.toString())
            statRow("Speed", troop.speed.toString())
            statRow("Formation", "${troop.formation.current} / ${troop.formation.maximum}")
        }
        add(stats).top().left()
    }

    private fun Table.statRow(name: String, value: String) {
        add(name.toLabel(fontColor = Color.LIGHT_GRAY, fontSize = 15)).left().padRight(12f)
        add(value.toLabel(fontColor = Color.WHITE, fontSize = 15)).right().row()
    }

    fun showNextTo(stage: Stage, anchor: Actor) {
        remove()
        stage.addActor(this)
        pack()

        val bottomLeft = anchor.localToStageCoordinates(Vector2(0f, 0f))
        val topRight = anchor.localToStageCoordinates(Vector2(anchor.width, anchor.height))
        val anchorLeft = minOf(bottomLeft.x, topRight.x)
        val anchorRight = maxOf(bottomLeft.x, topRight.x)
        val anchorBottom = minOf(bottomLeft.y, topRight.y)
        val anchorTop = maxOf(bottomLeft.y, topRight.y)

        val rightX = anchorRight + ANCHOR_GAP
        val leftX = anchorLeft - width - ANCHOR_GAP
        val targetX = if (rightX + width <= stage.width - EDGE_MARGIN) rightX else leftX
        val centeredY = (anchorBottom + anchorTop - height) / 2f

        setPosition(
            targetX.coerceIn(EDGE_MARGIN, max(EDGE_MARGIN, stage.width - width - EDGE_MARGIN)),
            centeredY.coerceIn(EDGE_MARGIN, max(EDGE_MARGIN, stage.height - height - EDGE_MARGIN))
        )
        toFront()
    }
}
