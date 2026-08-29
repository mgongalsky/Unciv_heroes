package com.unciv.ui.battlescreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.unciv.pure.domain.troop.Troop
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen

internal class BattleTurnBar(
    private val queueProvider: () -> List<Troop>,
    private val isAttacker: (Troop) -> Boolean,
    private val onSkip: () -> Unit,
    private val onExit: () -> Unit,
    private val onHover: (Troop?) -> Unit
) : Table() {

    companion object {
        const val HEIGHT = 92f
        private const val SLOT_SIZE = 58f
        private val ATTACKER_COLOR = Color.valueOf("F2C94CFF")
        private val DEFENDER_COLOR = Color.valueOf("4B8BFFFF")
        private val PANEL_COLOR = Color.valueOf("101723F2")
        private val SLOT_COLOR = Color.valueOf("202B3AFF")
    }

    init {
        background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(PANEL_COLOR)
        left()
        rebuild()
    }

    fun refresh() {
        if (stage == null) {
            rebuild()
        } else {
            Gdx.app.postRunnable {
                if (stage != null) rebuild()
            }
        }
    }

    private fun rebuild() {
        clearChildren()
        pad(10f, 14f, 10f, 14f)

        add(actionButton("Skip", onSkip)).width(92f).height(54f).padRight(8f)
        add(actionButton("Exit", onExit)).width(92f).height(54f).padRight(18f)

        queueProvider().forEachIndexed { index, troop ->
            add(createQueueSlot(troop, index == 0))
                .size(SLOT_SIZE)
                .padRight(7f)
        }
    }

    private fun actionButton(text: String, action: () -> Unit) =
            TextButton(text, BaseScreen.skin).apply {
                label.setFontScale(0.75f)
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) = action()
                })
            }

    private fun createQueueSlot(troop: Troop, current: Boolean): Actor {
        val sideColor = if (isAttacker(troop)) ATTACKER_COLOR else DEFENDER_COLOR
        val outer = Table().apply {
            background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(sideColor)
        }
        val inner = Table().apply {
            background = ImageGetter.getDrawable("OtherIcons/whiteDot").tint(
                if (current) SLOT_COLOR.cpy().lerp(Color.WHITE, 0.14f) else SLOT_COLOR
            )
        }
        val icon = ImageGetter.getUnitIcon(troop.unitName, Color.WHITE).apply {
            setSize(38f, 38f)
            setPosition(6f, 6f)
        }
        val amount = Label(troop.currentAmount.toString(), BaseScreen.skin).apply {
            setAlignment(Align.center)
            color = Color.WHITE
            setFontScale(0.32f)
            background =
                    ImageGetter.getDrawable("OtherIcons/whiteDot").tint(Color.valueOf("05080DCC"))
            setSize(16f, 14f)
            setPosition(31f, 1f)
        }
        val content = Group().apply {
            setSize(48f, 48f)
            addActor(icon)
            addActor(amount)
        }
        inner.add(content).size(48f).pad(2f)
        outer.add(inner).expand().fill().pad(if (current) 3f else 2f)
        if (!current) {
            outer.addListener(object : ClickListener() {
                override fun enter(
                    event: InputEvent?, x: Float, y: Float, pointer: Int, fromActor: Actor?
                ) {
                    onHover(troop)
                    super.enter(event, x, y, pointer, fromActor)
                }

                override fun exit(
                    event: InputEvent?, x: Float, y: Float, pointer: Int, toActor: Actor?
                ) {
                    onHover(null)
                    super.exit(event, x, y, pointer, toActor)
                }
            })
        }
        return outer
    }
}
