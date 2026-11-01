package com.unciv.ui.mapeditor

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.map.TileMap
import com.unciv.models.ruleset.Ruleset
import com.unciv.pure.application.story.EditStoryDialogue
import com.unciv.pure.domain.story.StoryDialogue
import com.unciv.pure.domain.story.StoryDialogueNode
import com.unciv.ui.utils.AutoScrollPane
import com.unciv.ui.utils.BaseScreen
import com.unciv.ui.utils.ZoomableScrollPane
import com.unciv.ui.utils.extensions.onClick
import com.unciv.ui.utils.extensions.toLabel
import com.unciv.ui.utils.extensions.toTextButton

/** Полноэкранный редактор владеет копией разговора до явного применения в черновик Story. */
class StoryDialogueGraphScreen(
    source: StoryDialogue,
    private val map: TileMap,
    private val ruleset: Ruleset,
    private val applyDialogue: (StoryDialogue) -> Unit
) : BaseScreen() {
    private var draft = source.deepCopy()
    private var selectedId = source.startNodeId
    private var editedNode: StoryDialogueNode? = null
    private var aiChoice = ""
    private var nodeForm: StoryEditorForm? = null
    private var metadataForm: StoryEditorForm? = null
    private val error = "".toLabel(Color.SALMON).apply { wrap = true; name = "story-graph-error" }
    private val root = Table().apply { setFillParent(true) }
    private val sidebar = Table().apply { top(); defaults().pad(4f).growX() }
    private val graph = object : ZoomableScrollPane(minZoom = 0.35f, maxZoom = 1.6f) {
        fun refreshContentSize() {
            sizeChanged()
            invalidate()
            validate()
        }
    }
    // Table размещает контейнер, а размер масштабируемой панели задаётся в её локальных координатах.
    private val graphViewport = object : Group() {
        override fun sizeChanged() {
            super.sizeChanged()
            graph.setSize(width / graph.scaleX, height / graph.scaleY)
            graph.refreshContentSize()
        }
    }.apply { addActor(graph) }
    private val sidebarScroll = AutoScrollPane(sidebar).apply {
        setScrollingDisabled(true, false)
        setFadeScrollBars(false)
    }
    private var sidebarCell: com.badlogic.gdx.scenes.scene2d.ui.Cell<AutoScrollPane>? = null
    private var closed = false

    init {
        stage.addActor(root)
        graph.setOverscroll(false, false)
        val toolbar = Table().apply { defaults().pad(4f).minHeight(42f) }
        fun button(text: String, action: () -> Unit) {
            toolbar.add(text.toTextButton().onClick { attempt(action) })
        }
        button("Apply dialogue") {
            capture()
            applyDialogue(draft.deepCopy())
            close()
        }
        button("Cancel") { close() }
        button("Add node") {
            capture()
            var number = 1
            while (draft.nodes.any { it.id == "node$number" }) number++
            val node = StoryDialogueNode(id = "node$number", text = "New dialogue line")
            draft = EditStoryDialogue.addNode(draft, node)
            selectedId = node.id
            rebuild(true)
        }
        button("Set as start") {
            capture()
            draft = EditStoryDialogue.setStart(draft, selectedId)
            rebuild(false)
        }
        button("Delete node") {
            capture()
            draft = EditStoryDialogue.removeNode(draft, selectedId)
            selectedId = draft.startNodeId
            rebuild(true)
        }
        val toolbarScroll = AutoScrollPane(toolbar).apply { setScrollingDisabled(false, true) }
        root.add(toolbarScroll).colspan(2).growX().height(58f).row()
        val navigation = Table().apply { defaults().pad(3f) }
        navigation.add("−".toTextButton().onClick { graph.zoomOut() })
        navigation.add("+".toTextButton().onClick { graph.zoomIn() })
        navigation.add("Center selected node".toTextButton().onClick { centerSelected() })
        navigation.add("Drag to pan • Select a node to edit".toLabel().apply { setFontScale(0.75f) })
        root.add(AutoScrollPane(navigation).apply { setScrollingDisabled(false, true) })
            .colspan(2).growX().height(48f).row()
        root.add(graphViewport).grow()
        sidebarCell = root.add(sidebarScroll).width(sidebarWidth()).growY()
        root.row()
        root.add(error).colspan(2).growX().minHeight(36f).pad(6f).row()
        // Escape/Android Back имеют ту же семантику, что Cancel; карта не изменяется.
        stage.addListener(object : InputListener() {
            override fun keyDown(event: InputEvent?, keycode: Int): Boolean {
                if (keycode != Input.Keys.ESCAPE && keycode != Input.Keys.BACK) return false
                close()
                return true
            }
        })
        rebuild(true)
    }

    /** Фиксированная ширина сохраняет доступность уже созданных полей при resize без потери ввода. */
    private fun sidebarWidth() = 300f

    private fun attempt(action: () -> Unit) {
        if (closed) return
        try {
            action()
            if (!closed) error.setText("")
        } catch (invalid: IllegalArgumentException) {
            error.setText(invalid.message.orEmpty())
        }
    }

    /** Поля меняют локальную реплику; команда публикует её в локальный граф только после проверки. */
    private fun capture() {
        metadataForm?.capture()
        nodeForm?.capture()
        val node = editedNode ?: return
        draft = EditStoryDialogue.replaceNode(draft, selectedId, node, aiChoice.ifBlank { null })
        selectedId = node.id
    }

    private fun mutateNode(action: () -> Unit) = attempt {
        metadataForm?.capture()
        nodeForm?.capture()
        action()
        val node = requireNotNull(editedNode)
        draft = EditStoryDialogue.replaceNode(draft, selectedId, node, aiChoice.ifBlank { null })
        selectedId = node.id
        rebuild(false)
    }

    private fun rebuild(center: Boolean) {
        val previousX = graph.scrollX
        val previousY = graph.scrollY
        val node = draft.nodes.firstOrNull { it.id == selectedId } ?: draft.nodes.firstOrNull()
        selectedId = node?.id.orEmpty()
        editedNode = node?.deepCopy()
        aiChoice = draft.aiChoices[selectedId].orEmpty()
        graph.actor = StoryDialogueGraphCanvas(draft, selectedId) { target ->
            attempt {
                val previousId = selectedId
                capture()
                // При клике на переименовываемую карточку сохраняем её новый ID.
                if (target != previousId) selectedId = target
                rebuild(false)
            }
        }
        sidebar.clear()
        val fieldWidth = sidebarWidth() - 32f
        metadataForm = StoryEditorForm(fieldWidth).also { form ->
            form.text("Dialogue ID", draft.id) { draft.id = it.trim() }
            form.flag("Pause world", draft.pauseWorld) { draft.pauseWorld = it }
            form.position("Camera focus (x,y; optional)", draft.focus) { draft.focus = it }
            sidebar.add(form).growX().row()
        }
        editedNode?.let { editable ->
            nodeForm = StoryDialogueEditor.createNode(
                draft, editable, map, ruleset, fieldWidth, aiChoice,
                { aiChoice = it }, ::mutateNode
            )
            sidebar.add(nodeForm).growX().row()
            sidebar.add("Update node".toTextButton().onClick {
                attempt { capture(); rebuild(false) }
            }).minHeight(44f).row()
        }
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

    /** Resize сохраняет незавершённый ввод: меняются размеры контейнеров, а не экземпляр черновика. */
    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        sidebarCell?.width(sidebarWidth())
        root.invalidateHierarchy()
        root.validate()
    }
}
