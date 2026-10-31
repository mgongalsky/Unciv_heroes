package com.unciv.ui.mapeditor

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.FloatAction
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.utils.BaseScreen
import kotlin.math.abs

class MapEditorToolsDrawer(tabs: MapEditorMainTabs, initStage: Stage,
                           private val mapHolder: EditorMapHolder) : Table(BaseScreen.skin) {
    private companion object {
        const val arrowImage = "OtherIcons/BackArrow"
        const val animationDuration = 0.333f
        const val clickEpsilon = 0.001f
    }
    private val handleWidth = if (Gdx.app.type == Application.ApplicationType.Desktop) 10f else 25f
    private val arrowSize = if (Gdx.app.type == Application.ApplicationType.Desktop) 10f else 20f
    var splitAmount = 1f
        set(value) { field = value; reposition() }
    private val arrowIcon = ImageGetter.getImage(arrowImage)
    init {
        touchable = Touchable.childrenOnly
        arrowIcon.setSize(arrowSize, arrowSize)
        arrowIcon.setOrigin(Align.center)
        arrowIcon.rotation = 180f
        val arrowWrapper = Container(arrowIcon).apply {
            align(Align.center)
            setSize(arrowSize, arrowSize)
            setOrigin(Align.center)
        }
        add(arrowWrapper).align(Align.center).width(handleWidth).fillY().apply {
            background = BaseScreen.skinStrings.getUiBackground(
                "MapEditor/MapEditorToolsDrawer/Handle", tintColor = BaseScreen.skin.get("color", Color::class.java))
        }
        // Все инструменты слоёв принадлежат странице Generate, общей панели над вкладками нет.
        add(Container(tabs).fill().minSize(0f, 0f))
            .width(requireNotNull(mapHolder.editorScreen).getToolsWidth()).height(initStage.height)
        pack()
        setPosition(initStage.width, 0f, Align.bottomRight)
        initStage.addActor(this)
        initStage.addCaptureListener(getListener(this))
    }
    private class SplitAmountAction(private val drawer: MapEditorToolsDrawer, endAmount: Float) :
        FloatAction(drawer.splitAmount, endAmount, animationDuration) {
        override fun act(delta: Float): Boolean {
            val result = super.act(delta)
            drawer.splitAmount = value
            return result
        }
    }

    private fun getListener(drawer: MapEditorToolsDrawer) = object : InputListener() {
        private var draggingPointer = -1
        private var oldSplitAmount = -1f
        private var lastX = 0f
        private var handleX = 0f
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            if (draggingPointer != -1) return false
            if (pointer == 0 && button != 0) return false
            // Stage capture видит также диалоги: совпадение X с ручкой не должно закрывать drawer.
            if (generateSequence(event.target) { it.parent }.any { it is com.unciv.ui.popup.Popup }) return false
            if (x !in drawer.x..(drawer.x + handleWidth)) return false
            editorHolder().killListeners()
            draggingPointer = pointer
            lastX = x
            handleX = drawer.x
            oldSplitAmount = splitAmount
            return true
        }

        override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
            if (pointer != draggingPointer) return
            editorHolder().resurrectListeners()
            draggingPointer = -1
            if (oldSplitAmount < 0f) return
            addAction(SplitAmountAction(drawer, if (splitAmount > 0.5f) 0f else 1f))
        }

        override fun touchDragged(event: InputEvent, x: Float, y: Float, pointer: Int) {
            if (pointer != draggingPointer) return
            val delta = x - lastX
            lastX = x
            handleX += delta
            splitAmount = drawer.xToSplitAmount(handleX)
            if (oldSplitAmount >= 0f && abs(oldSplitAmount - splitAmount) >= clickEpsilon) oldSplitAmount =
                    -1f
        }
    }
    private fun editorHolder() = mapHolder.editorScreen?.mapHolder ?: mapHolder
    private fun splitAmountToX() = stage.width - width + (1f - splitAmount) * (width - handleWidth)
    private fun xToSplitAmount(x: Float) =
        (1f - (x + width - stage.width) / (width - handleWidth)).coerceIn(0f, 1f)
    fun reposition() {
        if (stage == null) return
        when (splitAmount) {
            0f -> { arrowIcon.rotation = 0f; arrowIcon.isVisible = true }
            1f -> { arrowIcon.rotation = 180f; arrowIcon.isVisible = true }
            else -> arrowIcon.isVisible = false
        }
        setPosition(splitAmountToX(), 0f, Align.bottomLeft)
    }
}
