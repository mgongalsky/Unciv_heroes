package com.unciv.ui.mapeditor

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.story.EditStoryDialogue
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.popup.Popup
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ZoomableScrollPane
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton
import com.unciv.ui.utils.extensions.setFontSize

/** Координирует независимый черновик, историю и жесты; карта изменяется только владельцем после Apply. */
class StoryDialogueGraphScreen(
    source: StoryDialogue,
    private val map: TileMap,
    private val ruleset: Ruleset,
    private val applyDialogue: (StoryDialogue) -> Unit
) : BaseScreen() {
    private var synchronizingText = false
    private val editorState = StoryDialogueGraphState(source)
    private val draft get() = editorState.dialogue
    private val selectedId get() = editorState.selectedId
    private var properties = source.deepCopy()
    private var editedNode: StoryDialogueNode? = null
    private var aiChoice = ""
    private var nodeForm: StoryEditorForm? = null
    private var metadataForm: StoryEditorForm? = null
    private var selectedOutput: StoryDialogueGraphCanvas.Output? = null
    private val error = "".toLabel(Color.SALMON).apply { wrap = true; name = "story-graph-error" }
    private val selection = "".toLabel(Color.SKY).apply { wrap = true; setFontScale(0.7f) }
    private val root = Table().apply { setFillParent(true) }
    private val sidebar = Table().apply { top(); defaults().pad(4f).growX() }
    private val graph = object : ZoomableScrollPane(minZoom = 0.35f, maxZoom = 1.6f) {
        fun refreshContentSize() { sizeChanged(); invalidate(); validate() }
    }
    private val graphViewport = object : Group() {
        override fun sizeChanged() {
            super.sizeChanged()
            graph.setSize(width / graph.scaleX, height / graph.scaleY)
            graph.refreshContentSize()
        }
    }.apply { addActor(graph) }
    private val sidebarScroll = AutoScrollPane(sidebar).apply {
        setScrollingDisabled(true, false); setFadeScrollBars(false)
    }
    private var closed = false
    private val canvas get() = graph.actor as? StoryDialogueGraphCanvas

    init {
        stage.addActor(root)
        graph.setOverscroll(false, false)
        val toolbar = Table().apply { defaults().pad(4f).minHeight(42f) }
        fun button(text: String, action: () -> Unit) {
            toolbar.add(text.toTextButton().onClick { attempt(action) })
        }
        button("Apply dialogue") { capture(); applyDialogue(draft.deepCopy()); close() }
        button("Cancel") { close() }
        button("Add node") {
            capture()
            var number = 1
            while (draft.nodes.any { it.id == "node$number" }) number++
            val node = StoryDialogueNode(id = "node$number", text = "New dialogue line")
            editorState.publish(EditStoryDialogue.addNode(draft, node), node.id)
            selectedOutput = null
            rebuild(true)
        }
        button("Set as start") {
            capture()
            editorState.publish(EditStoryDialogue.setStart(draft, selectedId), selectedId)
            rebuild(false)
        }
        button("Delete node") { deleteNode() }
        button("Disconnect output") { disconnect() }
        button("Undo") { undo() }
        button("Redo") { redo() }
        root.add(AutoScrollPane(toolbar).apply { setScrollingDisabled(false, true) })
            .colspan(2).growX().height(58f).row()
        val navigation = Table().apply { defaults().pad(3f) }
        navigation.add("−".toTextButton().onClick { graph.zoomOut() })
        navigation.add("+".toTextButton().onClick { graph.zoomIn() })
        navigation.add("Text −".toTextButton().onClick {
            attempt { capture(); editorState.changeTextScale(editorState.textScale - 0.1f); rebuild(false) }
        })
        navigation.add("Text +".toTextButton().onClick {
            attempt { capture(); editorState.changeTextScale(editorState.textScale + 0.1f); rebuild(false) }
        })
        navigation.add("Center selected node".toTextButton().onClick { centerSelected() })
        navigation.add("Auto layout".toTextButton().onClick {
            attempt { capture(); editorState.autoLayout(); rebuild(true) }
        })
        navigation.add("Controls".toTextButton().onClick {
            Popup(this).apply {
                addGoodSizedLabel("Click a card to edit. Drag its title to move it. Drag the background to pan. Drag an answer output to a blue input to connect. Drop outside an input to cancel. Select an output and use Disconnect output to end that branch.").row()
                addGoodSizedLabel("Delete: selected output or node. Ctrl+Z: undo. Ctrl+Shift+Z / Ctrl+Y: redo. Escape: cancel the current gesture, otherwise close without applying. Shortcuts are disabled while typing. Text − / Text + change card text independently of zoom.").row()
                addCloseButton()
            }.open()
        })
        root.add(AutoScrollPane(navigation).apply { setScrollingDisabled(false, true) })
            .colspan(2).growX().height(48f).row()
        root.add(graphViewport).grow()
        root.add(sidebarScroll).width(300f).growY()
        root.row()
        root.add(selection).colspan(2).growX().height(28f).padHorizontal(6f).row()
        root.add(error).colspan(2).growX().minHeight(32f).pad(6f).row()
        stage.addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
                    if (canvas?.cancelGesture() != true) close()
                    return true
                }
                if (stage.keyboardFocus is TextField) return false
                val ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
                val shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)
                when {
                    keycode == Input.Keys.FORWARD_DEL -> attempt { if (selectedOutput != null) disconnect() else deleteNode() }
                    ctrl && keycode == Input.Keys.Z -> attempt { if (shift) redo() else undo() }
                    ctrl && keycode == Input.Keys.Y -> attempt { redo() }
                    else -> return false
                }
                return true
            }
        })
        rebuild(true)
    }

    private fun attempt(action: () -> Unit) {
        if (closed) return
        try { action(); if (!closed) error.setText("") }
        catch (invalid: IllegalArgumentException) { error.setText(invalid.message.orEmpty()) }
    }

    /** Собирает проверенный кандидат; ввод одного поля объединяется в одно действие истории. */
    private fun capture(editGroup: Any? = null) {
        metadataForm?.capture()
        nodeForm?.capture()
        val node = editedNode ?: return
        val oldId = selectedId
        val updated =
                EditStoryDialogue.replaceNode(properties, oldId, node, aiChoice.ifBlank { null })
        editorState.publish(updated, node.id, oldId, editGroup)
        properties = draft.deepCopy()
    }

    private fun mutateNode(action: () -> Unit) = attempt {
        metadataForm?.capture()
        nodeForm?.capture()
        action()
        val node = requireNotNull(editedNode)
        editorState.publish(EditStoryDialogue.replaceNode(properties, selectedId, node, aiChoice.ifBlank { null }), node.id, selectedId)
        rebuild(false)
    }

    /** Поля могли переименовать узел или ответ после создания портов: разрешаем их актуальные ID перед командой. */
    private fun connect(output: StoryDialogueGraphCanvas.Output, target: String) {
        val previousId = selectedId
        val choiceIndex = draft.nodes.firstOrNull { it.id == output.nodeId }?.choices
            ?.indexOfFirst { it.id == output.choiceId } ?: -1
        capture()
        val sourceId = if (output.nodeId == previousId) selectedId else output.nodeId
        val targetId = if (target == previousId) selectedId else target
        val choiceId = if (output.choiceId == null) null else {
            requireNotNull(draft.nodes.firstOrNull { it.id == sourceId }?.choices?.getOrNull(choiceIndex)?.id) {
                "The selected answer no longer exists"
            }
        }
        editorState.publish(EditStoryDialogue.setTransition(draft, sourceId, choiceId, targetId), selectedId)
        selectedOutput = StoryDialogueGraphCanvas.Output(sourceId, choiceId)
        rebuild(false)
    }

    private fun disconnect() {
        val output = requireNotNull(selectedOutput) { "Select an output port first" }
        connect(output, "")
    }

    private fun deleteNode() {
        capture()
        editorState.publish(EditStoryDialogue.removeNode(draft, selectedId), draft.startNodeId)
        selectedOutput = null
        rebuild(true)
    }

    private fun undo() {
        canvas?.cancelGesture()
        capture()
        if (editorState.undo()) { selectedOutput = null; rebuild(false) }
    }

    private fun redo() {
        canvas?.cancelGesture()
        capture()
        if (editorState.redo()) { selectedOutput = null; rebuild(false) }
    }

    private fun rebuild(center: Boolean) {
        canvas?.finishInline()
        val previousX = graph.scrollX
        val previousY = graph.scrollY
        properties = draft.deepCopy()
        editedNode = draft.nodes.firstOrNull { it.id == selectedId }?.deepCopy()
        aiChoice = draft.aiChoices[selectedId].orEmpty()
        graph.actor = StoryDialogueGraphCanvas(
            editorState, StoryDialogueGraphCanvas.Callbacks(
                select = { target ->
                    attempt {
                        val oldId = selectedId
                        capture()
                        if (target != oldId) editorState.select(target)
                        stage.keyboardFocus = null
                        selectedOutput = null
                        rebuild(false)
                    }
                },
                move = { id, x, y ->
                    attempt {
                        val oldId = selectedId
                        capture()
                        editorState.move(if (id == oldId) selectedId else id, x, y)
                        rebuild(false)
                    }
                },
                connect = { output, target -> attempt { connect(output, target) } },
                selectOutput = { output ->
                    selectedOutput = output
                    selection.setText("Output: ${output.nodeId} / ${output.choiceId ?: "Continue"} • Disconnect output → End dialogue")
                }
            ))
        canvas?.installTextEditing(::openInline) { target -> attempt { addAnswer(target) } }
        // Кнопка остаётся вне прокрутки; размер задан в единицах интерфейса, а не долях исходного шрифта.
        if (sidebarScroll.parent === root) {
            @Suppress("UNCHECKED_CAST")
            val cell =
                    root.getCell(sidebarScroll) as com.badlogic.gdx.scenes.scene2d.ui.Cell<com.badlogic.gdx.scenes.scene2d.Actor>
            val host = Table()
            host.add("Add answer to selected".toTextButton().apply {
                name = "story-graph-add-answer"
                label.setFontSize(18)
            }.onClick { attempt { addAnswer() } }).growX().height(34f).pad(2f).row()
            host.add(sidebarScroll).grow()
            cell.setActor(host).width(280f)
        }
        sidebar.clear()
        sidebar.defaults().pad(1f).growX()
        val fieldWidth = 252f
        metadataForm = StoryEditorForm(fieldWidth).also { form ->
            form.text("Dialogue ID", draft.id) { properties.id = it.trim() }
            form.flag("Pause world", draft.pauseWorld) { properties.pauseWorld = it }
            form.position("Camera focus (x,y; optional)", draft.focus) { properties.focus = it }
            sidebar.add(form.compact()).growX().row()
        }
        editedNode?.let { editable ->
            nodeForm = StoryDialogueEditor.createNode(
                draft, editable, map, ruleset, fieldWidth, aiChoice,
                { aiChoice = it }, ::mutateNode, { attempt { addAnswer() } }).apply {
                compact()
                observeChanges(::liveText)
            }
            sidebar.add(nodeForm).growX().row()
            sidebar.add("Update node".toTextButton().apply { label.setFontSize(16) }
                .onClick { attempt { capture(); rebuild(false) } }).height(30f).row()
        }
        selection.setFontSize(16)
        error.setFontSize(16)
        selection.setText(selectedOutput?.let { "Output: ${it.nodeId} / ${it.choiceId ?: "Continue"} • Disconnect output → End dialogue" }
            ?: "Click text: edit • Drag title: move • Text: ${(editorState.textScale * 100).toInt()}%")
        root.invalidateHierarchy()
        root.validate()
        graph.refreshContentSize()
        if (center) centerSelected() else {
            graph.scrollX = previousX
            graph.scrollY = previousY
            graph.updateVisualScroll()
        }
    }

    private fun centerSelected() {
        val canvas = graph.actor as? Group ?: return
        val card = canvas.findActor<com.badlogic.gdx.scenes.scene2d.Actor>("story-graph-node:$selectedId") ?: return
        graph.refreshContentSize()
        graph.scrollTo(card.x + card.width / 2f, canvas.height - card.y - card.height / 2f, true)
    }

    private fun close() {
        if (closed || game.screen !== this) return
        closed = true
        game.popScreen()
    }

    /** Resize не пересоздаёт поля и сохраняет незавершённый ввод. */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        root.invalidateHierarchy(); root.validate()
    }

    /** Задаёт боковые отступы ячейки средствами используемой версии Scene2D. */
    private fun <T : com.badlogic.gdx.scenes.scene2d.Actor> com.badlogic.gdx.scenes.scene2d.ui.Cell<T>.padHorizontal(
        value: Float
    ): com.badlogic.gdx.scenes.scene2d.ui.Cell<T> =
            padLeft(value).padRight(value)

    /** Обновляет текст при вводе, не пересоздавая активное поле и не публикуя незавершённые ID. */
    private fun liveText(actor: com.badlogic.gdx.scenes.scene2d.Actor) {
        if (synchronizingText || actor !is TextField) return
        val caption = actor.name
        if (caption != "story-field:Text" && caption != "story-field:Speaker" && caption != "story-field:Answer text") return
        attempt {
            val node = draft.nodes.first { it.id == selectedId }.deepCopy()
            when (caption) {
                "story-field:Text" -> {
                    node.text = actor.text; editedNode?.text = actor.text
                }

                "story-field:Speaker" -> {
                    node.speaker = actor.text; editedNode?.speaker = actor.text
                }

                else -> {
                    val index = textFields(nodeForm).filter { it.name == caption }.indexOf(actor)
                    require(index in node.choices.indices) { "The selected answer no longer exists" }
                    node.choices[index].text = actor.text
                    editedNode?.choices?.getOrNull(index)?.text = actor.text
                }
            }
            editorState.publish(
                EditStoryDialogue.replaceNode(draft, selectedId, node),
                selectedId,
                editGroup = actor
            )
            // Метаданные и незавершённые ID остаются в существующих полях до capture.
            properties.nodes.firstOrNull { it.id == selectedId }?.let { current ->
                current.text = node.text
                current.speaker = node.speaker
                current.choices.zip(node.choices)
                    .forEach { (old, updated) -> old.text = updated.text }
            }
            canvas?.refreshText(node)
        }
    }
    private fun textFields(actor: com.badlogic.gdx.scenes.scene2d.Actor?): List<TextField> =
            when (actor) {
                is TextField -> listOf(actor)
                is Group -> actor.children.flatMap { textFields(it) }
                else -> emptyList()
            }

    /** Смена выбранного узла фиксируется до открытия поля; дальнейший ввод не перестраивает Canvas. */
    private fun openInline(nodeId: String, choiceId: String?) = attempt {
        val oldId = selectedId
        val choiceIndex =
                draft.nodes.first { it.id == nodeId }.choices.indexOfFirst { it.id == choiceId }
        capture()
        val actualId = if (nodeId == oldId) selectedId else nodeId
        editorState.select(actualId)
        selectedOutput = null
        rebuild(false)
        val actualChoice =
                if (choiceId == null) null else draft.nodes.first { it.id == actualId }.choices[choiceIndex].id
        canvas?.beginInline(actualId, actualChoice) { value, group ->
            attempt {
                val node = draft.nodes.first { it.id == selectedId }.deepCopy()
                val index = node.choices.indexOfFirst { it.id == actualChoice }
                if (actualChoice == null) {
                    node.text = value
                    editedNode?.text = value
                } else {
                    require(index >= 0) { "The selected answer no longer exists" }
                    node.choices[index].text = value
                    editedNode?.choices?.getOrNull(index)?.text = value
                }
                editorState.publish(
                    EditStoryDialogue.replaceNode(draft, selectedId, node),
                    selectedId,
                    editGroup = group
                )
                properties = draft.deepCopy()
                val fields = textFields(nodeForm)
                val field =
                        if (actualChoice == null) fields.firstOrNull { it.name == "story-field:Text" }
                        else fields.filter { it.name == "story-field:Answer text" }.getOrNull(index)
                synchronizingText = true
                try {
                    field?.text = value
                } finally {
                    synchronizingText = false
                }
                canvas?.refreshText(node)
            }
        }
    }

    /** Кнопки и Continue используют одну команду; первый ответ наследует переход выбранной карточки. */
    private fun addAnswer(nodeId: String = selectedId) {
        val previousId = selectedId
        capture()
        val actualId = if (nodeId == previousId) selectedId else nodeId
        val node = draft.nodes.first { it.id == actualId }
        var number = 1
        while (node.choices.any { it.id == "answer$number" }) number++
        val answerId = "answer$number"
        editorState.publish(EditStoryDialogue.addAnswer(draft, actualId, answerId), actualId)
        selectedOutput = null
        rebuild(false)
        centerSelected()
        openInline(actualId, answerId)
    }
}
